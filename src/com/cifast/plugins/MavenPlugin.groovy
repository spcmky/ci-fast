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
        '''\
Naming conventions:
- Test files mirror source: src/main/java/com/example/Foo.java → src/test/java/com/example/FooTest.java
- Integration tests often use suffix IT (e.g., FooIT.java) and live alongside unit tests

Implicit dependencies:
- src/main/resources/ config files (application.properties, *.xml configs) → select integration tests that load Spring/config contexts
- src/test/resources/ test fixtures → select tests in the same package that rely on those fixtures
- Abstract test base classes or test utility classes (TestHelper, TestFixtures) → select all tests that extend/use them

Framework gotchas:
- Spring @ContextConfiguration or @SpringBootTest tests share cached contexts — a change to any @Configuration class can affect all context-loading tests
- Surefire/Failsafe plugin config in pom.xml controls which tests run and how — changes to <includes>/<excludes> patterns affect test discovery itself
- Multi-module Maven: a change in a parent module's src/main may affect tests in child modules that depend on it'''
    }
}
