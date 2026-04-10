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
        'Go project: changes to interfaces may affect all implementors. Changes to go.mod/go.sum should trigger all tests. Test files live in the same package directory as source files.'
    }
}
