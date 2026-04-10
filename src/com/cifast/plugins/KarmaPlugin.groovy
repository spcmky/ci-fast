package com.cifast.plugins

import com.cifast.LanguagePlugin

class KarmaPlugin extends LanguagePlugin {

    String getId() { 'karma' }

    List<String> getTestGlobs() {
        ['**/*.spec.ts', '**/*.spec.js', '**/*.test.ts', '**/*.test.js',
         '**/test/**/*.ts', '**/test/**/*.js']
    }

    List<String> getDiffExclusions() {
        ['node_modules/**', 'dist/**', '.angular/**', 'coverage/**']
    }

    String formatTestList(List<String> tests) {
        // Karma uses --grep for pattern matching against describe/it blocks
        // Fall back to file-based filtering via environment variable
        tests.join(',')
    }

    String getPromptHints() {
        '''\
Naming conventions:
- Test files mirror source: user.component.ts → user.component.spec.ts
- Services: auth.service.ts → auth.service.spec.ts
- Pipes, directives, guards follow the same pattern

Implicit dependencies:
- karma.conf.js/ts and test.ts → affects ALL tests (test runner bootstrap and configuration)
- angular.json test configuration → affects test discovery and execution
- Shared modules (SharedModule, CoreModule) → select tests for all components that import them
- Custom test utilities or mock services in testing/ directories → select tests that inject them

Framework gotchas:
- Angular TestBed configuration: components declare their own TestBed setup, but changes to a shared module affect all components that import it
- Dependency injection: changing a service's provider or interface affects all components/specs that inject it
- OnPush change detection: template-related test failures may not be obvious from source changes alone
- Lazy-loaded modules: changes to a lazy module only affect specs within that module's scope'''
    }
}
