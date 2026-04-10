package com.cifast

class TestSelection implements Serializable {
    boolean runAll
    List<String> selectedTests
    List<String> skippedTests
    double confidence
    String reasoning
    boolean cached
    LanguagePlugin plugin

    /**
     * Format test list using the assigned plugin.
     */
    String formatTestList() {
        if (!plugin) {
            throw new IllegalStateException("No plugin set on TestSelection")
        }
        return plugin.formatTestList(selectedTests)
    }

    /**
     * Format test list using a specific plugin.
     */
    String formatForPlugin(LanguagePlugin p) {
        return p.formatTestList(selectedTests)
    }

    /**
     * @deprecated Use formatTestList() or formatForPlugin() instead
     */
    @Deprecated
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
     * @deprecated Use formatTestList() or formatForPlugin() instead
     */
    @Deprecated
    String gradleTestList() {
        selectedTests.collect { path ->
            def fqcn = path.replaceAll(/\.(java|groovy|kt)$/, '')
                           .replaceAll('/', '.')
                           .replaceFirst(/^.*?src\.test\.(java|groovy|kotlin)\./, '')
            "--tests \"${fqcn}\""
        }.join(' ')
    }

    /**
     * @deprecated Use formatTestList() or formatForPlugin() instead
     */
    @Deprecated
    String jestTestList() {
        selectedTests.join(' ')
    }

    /**
     * @deprecated Use formatTestList() or formatForPlugin() instead
     */
    @Deprecated
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
