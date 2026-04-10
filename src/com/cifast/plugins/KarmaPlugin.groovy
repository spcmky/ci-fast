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
        'Angular/Karma project: test files typically mirror source files (e.g., user.component.ts -> user.component.spec.ts). Changes to shared modules, services, or angular.json should trigger all tests. Changes to test.ts or karma.conf.js affect the test runner itself.'
    }
}
