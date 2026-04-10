package com.cifast.plugins

import com.cifast.LanguagePlugin

class GradlePlugin extends LanguagePlugin {

    String getId() { 'gradle' }

    List<String> getTestGlobs() {
        ['**/src/test/**/*.java', '**/src/test/**/*.groovy', '**/src/test/**/*.kt']
    }

    List<String> getDiffExclusions() {
        ['gradle/wrapper/**', '.gradle/**']
    }

    String formatTestList(List<String> tests) {
        tests.collect { path ->
            def fqcn = path.replaceAll(/\.(java|groovy|kt)$/, '')
                           .replaceAll('/', '.')
                           .replaceFirst(/^.*?src\.test\.(java|groovy|kotlin)\./, '')
            "--tests \"${fqcn}\""
        }.join(' ')
    }

    String getPromptHints() {
        'Java/Gradle project: changes to build.gradle, build.gradle.kts, or settings.gradle should trigger all tests. Multi-module projects may have tests in subproject directories.'
    }
}
