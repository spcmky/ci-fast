package com.cifast

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class ResultCache implements Serializable {
    def steps
    static final String CACHE_DIR = '.ci-fast-cache'

    ResultCache(def steps) {
        this.steps = steps
    }

    @NonCPS
    static String cacheKey(String sha, String baseBranch) {
        def safeSha = sha.replaceAll(/[^a-fA-F0-9]/, '')
        def safeBranch = baseBranch.replaceAll(/[^a-zA-Z0-9_.\-]/, '_')
        return "${safeSha}-${safeBranch}"
    }

    TestSelection get(String sha, String baseBranch) {
        def key = cacheKey(sha, baseBranch)
        def path = "${CACHE_DIR}/${key}.json"
        def exists = steps.sh(script: "test -f ${path} && echo yes || echo no", returnStdout: true).trim()
        if (exists == 'yes') {
            def raw = steps.readFile(file: path)
            def map = new JsonSlurper().parseText(raw)
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
            reasoning: sel.reasoning
        ]))
        steps.writeFile(file: "${CACHE_DIR}/${key}.json", text: json)
    }
}
