package com.cifast.plugins

import com.cifast.LanguagePlugin

class PytestPlugin extends LanguagePlugin {

    String getId() { 'pytest' }

    List<String> getTestGlobs() {
        ['**/test_*.py', '**/*_test.py', '**/tests/**/*.py']
    }

    List<String> getDiffExclusions() {
        ['__pycache__/**', '*.pyc', '*.egg-info/**']
    }

    String formatTestList(List<String> tests) {
        tests.join(' ')
    }

    String getPromptHints() {
        'Python project using pytest: changes to conftest.py fixtures affect all tests in that directory and below. Changes to requirements.txt or pyproject.toml should trigger all tests.'
    }
}
