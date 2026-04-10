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
        '''\
Naming conventions:
- Test files mirror source: src/components/Button.tsx → src/components/Button.test.tsx or __tests__/Button.test.tsx
- Some projects use .spec.ts/.spec.tsx instead of .test.ts/.test.tsx

Implicit dependencies:
- jest.config.js/ts, jest.setup.js/ts → affects all tests (test runner config, global mocks)
- __mocks__/ directory files → select tests that import the mocked module
- Test utilities or custom render wrappers (e.g., test-utils.tsx, renderWithProviders) → select all tests that import them
- Shared hooks, contexts, or providers (e.g., AuthContext, ThemeProvider) → select tests for components that consume them

Framework gotchas:
- Module aliases in tsconfig paths or jest moduleNameMapper can obscure which source file a test actually imports
- Snapshot files (.snap) are auto-generated — a source change may require snapshot updates in multiple test files
- CSS modules or styled-components: visual changes may not break tests unless snapshot testing is used
- Monorepo with workspaces: cross-package imports mean a change in packages/shared can affect tests in packages/app'''
    }
}
