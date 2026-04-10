package com.cifast.plugins

import com.cifast.LanguagePlugin

class JestPlugin extends LanguagePlugin {

    String getId() { 'jest' }

    List<String> getTestGlobs() {
        ['**/*.test.ts', '**/*.test.tsx', '**/*.test.js', '**/*.test.jsx',
         '**/*.spec.ts', '**/*.spec.tsx', '**/*.spec.js', '**/*.spec.jsx']
    }

    List<String> getDiffExclusions() {
        ['node_modules/**', 'dist/**', 'build/**', '.next/**']
    }

    String formatTestList(List<String> tests) {
        tests.join(' ')
    }

    String getPromptHints() {
        'TypeScript/JavaScript project using Jest: changes to shared modules in src/utils or src/lib likely affect many tests. Changes to package.json dependencies should trigger all tests.'
    }
}
