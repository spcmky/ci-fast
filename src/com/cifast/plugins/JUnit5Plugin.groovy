package com.cifast.plugins

import com.cifast.LanguagePlugin

class JUnit5Plugin extends LanguagePlugin {

    String getId() { 'junit5' }

    List<String> getTestGlobs() {
        ['**/src/test/**/*.java', '**/src/test/**/*.groovy', '**/src/test/**/*.kt']
    }

    List<String> getDiffExclusions() {
        ['pom.xml.versionsBackup', '*.iml']
    }

    String formatTestList(List<String> tests) {
        def fqcns = tests.collect { path ->
            path.replaceAll(/\.(java|groovy|kt)$/, '')
                .replaceAll('/', '.')
                .replaceFirst(/^.*?src\.test\.(java|groovy|kotlin)\./, '')
        }
        fqcns.collect { "--include-classname \"^${it}\$\"" }.join(' ')
    }

    String getPromptHints() {
        'Java project using JUnit 5: tests may use @Tag annotations for grouping. Changes to @ExtendWith custom extensions affect all tests using that extension. Changes to pom.xml or build.gradle dependencies should trigger all tests.'
    }
}
