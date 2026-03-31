package com.cifast

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class ResultCache implements Serializable {
    def steps
    static final String CACHE_DIR = '.ci-fast-cache'

    ResultCache(def steps) {
        this.steps = steps
    }

    TestSelection get(String sha, String baseBranch) {
        def key = "${sha}-${baseBranch.replaceAll('/', '_')}"
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
        def key = "${sha}-${baseBranch.replaceAll('/', '_')}"
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
