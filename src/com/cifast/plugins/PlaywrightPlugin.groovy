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
        '''\
Naming conventions:
- Test files: *.spec.ts or *.spec.js, typically in tests/ or e2e/ directories
- Page Object Models (POMs) usually in pages/ or page-objects/ directories

Implicit dependencies:
- playwright.config.ts/js → affects ALL tests (browser config, base URL, timeouts, projects)
- global-setup.ts/global-teardown.ts → affects all tests (auth state, DB seeding)
- Page Object Model files → select all tests that use that page object
- Test fixtures defined via test.extend() → select all tests that use the extended fixture
- .auth/ state files (stored auth) → affects tests that rely on authenticated sessions

Framework gotchas:
- E2E tests have implicit cross-feature dependencies — a change to a shared UI component (navbar, sidebar, modal) can break tests for unrelated features
- API route changes affect any E2E test that exercises a flow hitting that endpoint
- Test.describe blocks can have beforeAll/beforeEach hooks that set up shared state
- Be conservative: when in doubt about E2E scope, include the test — E2E flakiness from missed selection is costly to debug'''
    }
}
