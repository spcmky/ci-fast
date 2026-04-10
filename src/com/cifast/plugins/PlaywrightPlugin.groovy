package com.cifast.plugins

import com.cifast.LanguagePlugin

class PlaywrightPlugin extends LanguagePlugin {

    String getId() { 'playwright' }

    List<String> getTestGlobs() {
        ['**/*.spec.ts', '**/*.spec.js', '**/e2e/**/*.ts', '**/e2e/**/*.js',
         '**/tests/**/*.spec.ts', '**/tests/**/*.spec.js']
    }

    List<String> getDiffExclusions() {
        ['node_modules/**', 'test-results/**', 'playwright-report/**', '.auth/**']
    }

    String formatTestList(List<String> tests) {
        tests.join(' ')
    }

    String getPromptHints() {
        'Playwright E2E test project: these are end-to-end browser tests, not unit tests. Changes to page object models or test fixtures (playwright.config.ts, global-setup.ts) affect all tests. Changes to API routes or UI components may affect any E2E test that exercises that flow. Be conservative — E2E tests often have implicit cross-feature dependencies.'
    }
}
