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
        '''\
Naming conventions:
- Test classes: FooTest.java, FooTests.java, or FooSpec.java
- Nested tests use @Nested inner classes — changes to the outer class affect all nested tests
- Parameterized tests via @MethodSource reference static factory methods — changes to those methods affect the test

Implicit dependencies:
- Custom @ExtendWith extensions (e.g., MockitoExtension, SpringExtension, custom lifecycle callbacks) → select all tests annotated with that extension
- @RegisterExtension fields in base test classes → select all subclasses
- @TempDir, @Timeout, or custom composed annotations defined in test utilities → select tests using them

Framework gotchas:
- @TestInstance(PER_CLASS) tests share state across methods — more sensitive to setup changes
- @DynamicTest factories generate tests at runtime — changes to the factory method affect all generated cases
- Conditional execution annotations (@EnabledIf, @DisabledOnOs) may hide tests on certain platforms
- TestReporter and TestInfo injections mean test infrastructure changes can ripple to any test using them'''
    }
}
