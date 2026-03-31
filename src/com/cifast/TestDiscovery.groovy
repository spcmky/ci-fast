package com.cifast

class TestDiscovery implements Serializable {
    def steps

    TestDiscovery(def steps) {
        this.steps = steps
    }

    List<String> find(List<String> globs) {
        def allTests = []
        globs.each { glob ->
            def result = steps.sh(
                script: "find . -path '${glob}' -type f 2>/dev/null | sort",
                returnStdout: true
            ).trim()
            if (result) {
                allTests.addAll(result.readLines().collect { it.replaceFirst(/^\.\//, '') })
            }
        }
        return allTests.unique()
    }
}
