package com.cifast.plugins

import com.cifast.LanguagePlugin

class GoPlugin extends LanguagePlugin {

    String getId() { 'go' }

    List<String> getTestGlobs() {
        ['**/*_test.go']
    }

    List<String> getDiffExclusions() {
        ['vendor/**']
    }

    String formatTestList(List<String> tests) {
        // Go tests run by package — extract unique package directories
        def packages = tests.collect { path ->
            def dir = path.contains('/') ? path.substring(0, path.lastIndexOf('/')) : '.'
            "./${dir}/..."
        }.unique()
        packages.join(' ')
    }

    String getPromptHints() {
        '''\
Naming conventions:
- Test files live alongside source: foo.go → foo_test.go (same package directory)
- Test functions: TestFoo(t *testing.T), benchmarks: BenchmarkFoo(b *testing.B)
- _test.go files may use package_test (black-box) or package (white-box) — both are valid

Implicit dependencies:
- internal/ packages are only importable by their parent tree — changes affect tests in that subtree
- testdata/ directories contain test fixtures loaded at runtime — changes affect tests in the same package
- Shared test helpers (testutil/, testhelper packages) → select tests in all packages that import them
- Interface changes in one package affect all packages that implement or consume that interface

Framework gotchas:
- TestMain(m *testing.M) in a package controls setup/teardown for ALL tests in that package
- Build tags (//go:build integration) can hide tests — tag-gated tests won't appear in normal runs
- go test runs per-package — a change to pkg/a only needs tests in packages that directly or transitively import pkg/a
- Table-driven tests are common — a change to the test table data structure affects all subtests'''
    }
}
