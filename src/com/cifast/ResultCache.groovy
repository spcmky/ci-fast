package com.cifast

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class ResultCache implements Serializable {
    def steps
    static final String CACHE_DIR = '.ci-fast-cache'
    static final String CACHE_VERSION = '2'
    static final long TTL_MS = 24 * 60 * 60 * 1000 // 24 hours

    ResultCache(def steps) {
        this.steps = steps
    }

    @NonCPS
    static String cacheKey(String sha, String baseBranch) {
        def safeSha = sha.replaceAll(/[^a-fA-F0-9]/, '')
        def safeBranch = baseBranch.replaceAll(/[^a-zA-Z0-9_.\-]/, '_')
        return "v${CACHE_VERSION}-${safeSha}-${safeBranch}"
    }

    TestSelection get(String sha, String baseBranch) {
        def key = cacheKey(sha, baseBranch)
        def path = "${CACHE_DIR}/${key}.json"
        def exists = steps.sh(script: "test -f ${path} && echo yes || echo no", returnStdout: true).trim()
        if (exists == 'yes') {
            def raw = steps.readFile(file: path)
            def map = new JsonSlurper().parseText(raw)
            // Check TTL
            if (map.timestamp && (System.currentTimeMillis() - (map.timestamp as long)) > TTL_MS) {
                steps.sh(script: "rm -f ${path}", returnStatus: true)
                return null
            }
            return new TestSelection(
                runAll: map.runAll,
                selectedTests: map.selectedTests,
                skippedTests: map.skippedTests,
                confidence: map.confidence as double,
                reasoning: map.reasoning,
                cached: true
            )
        }
        return null
    }

    void put(String sha, String baseBranch, TestSelection sel) {
        def key = cacheKey(sha, baseBranch)
        steps.sh(script: "mkdir -p ${CACHE_DIR}")
        def json = JsonOutput.prettyPrint(JsonOutput.toJson([
            runAll: sel.runAll,
            selectedTests: sel.selectedTests,
            skippedTests: sel.skippedTests,
            confidence: sel.confidence,
            reasoning: sel.reasoning,
            timestamp: System.currentTimeMillis()
        ]))
        steps.writeFile(file: "${CACHE_DIR}/${key}.json", text: json)
    }

    void cleanup() {
        steps.sh(script: "find ${CACHE_DIR} -name '*.json' -mtime +1 -delete 2>/dev/null || true", returnStatus: true)
    }
}
