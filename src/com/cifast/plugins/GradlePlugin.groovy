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
        '''\
Naming conventions:
- Test files mirror source: src/main/java/com/example/Foo.java → src/test/java/com/example/FooTest.java
- Kotlin tests may use Spec suffix (e.g., FooSpec.kt)
- Multi-module layout: subprojects each have their own src/test/ directory

Implicit dependencies:
- buildSrc/ changes affect the entire build — treat as run-all trigger
- Custom Gradle plugins or convention plugins in buildSrc/ → all tests
- Shared test fixtures (testFixtures source set) → select tests in any module that depends on them
- src/main/resources/ config files → select integration tests that load those configs

Framework gotchas:
- Gradle composite builds: changes to included builds may affect consumer tests
- Test task configuration in build.gradle (includes, excludes, JVM args) affects test execution itself
- Spring Boot with Gradle: changes to bootJar/bootRun config can affect integration test behavior
- settings.gradle(.kts) module includes determine which subprojects exist — changes affect test discovery'''
    }
}
