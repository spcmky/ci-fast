package com.cifast.plugins

import com.cifast.LanguagePlugin

class MavenPlugin extends LanguagePlugin {

    String getId() { 'maven' }

    List<String> getTestGlobs() {
        ['**/src/test/**/*.java', '**/src/test/**/*.groovy', '**/src/test/**/*.kt']
    }

    List<String> getDiffExclusions() {
        ['pom.xml.versionsBackup', '*.iml']
    }

    String formatTestList(List<String> tests) {
        tests.collect { path ->
            path.replaceAll(/\.java$/, '')
                .replaceAll(/\.groovy$/, '')
                .replaceAll(/\.kt$/, '')
                .replaceAll('/', '.')
                .replaceFirst(/^.*?src\.test\.(java|groovy|kotlin)\./, '')
        }.join(',')
    }

    String getPromptHints() {
        'Java/Maven project: changes to pom.xml dependency versions or plugin configs should trigger all tests. Changes to src/main/resources config files may affect integration tests.'
    }
}
