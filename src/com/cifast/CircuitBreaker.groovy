package com.cifast

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class CircuitBreaker implements Serializable {
    def steps
    static final String STATE_FILE = '.ci-fast-circuit-breaker.json'
    static final int MAX_FAILURES = 5
    static final long WINDOW_MS = 10 * 60 * 1000 // 10 minutes

    CircuitBreaker(def steps) {
        this.steps = steps
    }

    boolean isOpen() {
        def exists = steps.sh(script: "test -f ${STATE_FILE} && echo yes || echo no", returnStdout: true).trim()
        if (exists != 'yes') return false

        def raw = steps.readFile(file: STATE_FILE)
        def state = parseState(raw)
        if (!state) return false

        def now = System.currentTimeMillis()
        def recentFailures = filterRecent(state.failures, now, WINDOW_MS)
        return recentFailures.size() >= MAX_FAILURES
    }

    void recordFailure() {
        def state = [failures: []]
        def exists = steps.sh(script: "test -f ${STATE_FILE} && echo yes || echo no", returnStdout: true).trim()
        if (exists == 'yes') {
            def raw = steps.readFile(file: STATE_FILE)
            state = parseState(raw) ?: state
        }

        def now = System.currentTimeMillis()
        state.failures = filterRecent(state.failures, now, WINDOW_MS)
        state.failures.add(now)

        def json = JsonOutput.prettyPrint(JsonOutput.toJson(state))
        steps.writeFile(file: STATE_FILE, text: json)
    }

    @NonCPS
    static Map parseState(String raw) {
        try {
            return new JsonSlurper().parseText(raw) as Map
        } catch (Exception ignored) {
            return null
        }
    }

    @NonCPS
    static List filterRecent(List failures, long now, long windowMs) {
        return (failures ?: []).findAll { (it as long) > (now - windowMs) }
    }
}
