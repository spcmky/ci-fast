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
        '''\
Naming conventions:
- Test files: test_foo.py or foo_test.py
- Test functions: test_something() — class-based: class TestSomething with test_ methods
- Fixtures defined in conftest.py files at each directory level

Implicit dependencies:
- conftest.py at any directory level → select ALL tests in that directory and all subdirectories (fixtures are inherited)
- pytest plugins in conftest.py (pytest_configure, pytest_collection_modifyitems) → affects all tests
- Fixture files or factory modules (factories.py, fixtures/) → select tests that use those fixtures
- __init__.py in test packages → can affect test collection and imports

Framework gotchas:
- conftest.py fixtures cascade: a root conftest.py change affects the entire test suite, but a subdirectory conftest.py only affects tests below it
- Parametrized tests (@pytest.mark.parametrize) referencing external data files → changes to those data files affect the test
- Monkeypatching and mock fixtures defined in conftest.py — not visible from the test file alone
- Django projects: changes to models.py, settings.py, or migrations/ often require running all tests due to DB schema dependencies'''
    }
}
