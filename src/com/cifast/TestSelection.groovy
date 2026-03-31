package com.cifast

class TestSelection implements Serializable {
    boolean runAll
    List<String> selectedTests
    List<String> skippedTests
    double confidence
    String reasoning
    boolean cached

    /**
     * Maven: -Dtest=com.foo.TestA,com.foo.TestB
     */
    String mavenTestList() {
        selectedTests.collect { path ->
            path.replaceAll(/\.java$/, '')
                .replaceAll(/\.groovy$/, '')
                .replaceAll(/\.kt$/, '')
                .replaceAll('/', '.')
                .replaceFirst(/^.*?src\.test\.(java|groovy|kotlin)\./, '')
        }.join(',')
    }

    /**
     * Gradle: --tests "com.foo.TestA" --tests "com.foo.TestB"
     */
    String gradleTestList() {
        selectedTests.collect { path ->
            def fqcn = path.replaceAll(/\.(java|groovy|kt)$/, '')
                           .replaceAll('/', '.')
                           .replaceFirst(/^.*?src\.test\.(java|groovy|kotlin)\./, '')
            "\"${fqcn}\""
        }.join(' --tests ')
    }

    /**
     * Jest: space-separated relative paths
     */
    String jestTestList() {
        selectedTests.join(' ')
    }

    /**
     * Pytest: space-separated relative paths
     */
    String pytestTestList() {
        selectedTests.join(' ')
    }

    static TestSelection runAllFallback(String reason) {
        return new TestSelection(
            runAll: true,
            selectedTests: [],
            skippedTests: [],
            confidence: 0.0,
            reasoning: "Fallback: ${reason}",
            cached: false
        )
    }
}
