package com.cifast.plugins

import com.cifast.LanguagePlugin

class RSpecPlugin extends LanguagePlugin {

    String getId() { 'rspec' }

    List<String> getTestGlobs() {
        ['**/spec/**/*_spec.rb']
    }

    List<String> getDiffExclusions() {
        ['.bundle/**', 'vendor/bundle/**']
    }

    String formatTestList(List<String> tests) {
        tests.join(' ')
    }

    String getPromptHints() {
        '''\
Naming conventions:
- Test files mirror source: app/models/user.rb → spec/models/user_spec.rb
- Service objects: app/services/foo.rb → spec/services/foo_spec.rb
- Controllers, mailers, jobs follow the same pattern under spec/

Implicit dependencies:
- spec/spec_helper.rb and spec/rails_helper.rb → affects ALL specs (global config, shared setup)
- spec/support/ directory (custom matchers, shared contexts, request helpers) → select specs that include those modules
- spec/factories/ (FactoryBot) → select specs that build/create instances of the changed factory
- Shared examples (shared_examples_for, it_behaves_like) → select all specs that include them

Framework gotchas:
- Rails autoloading: changes to app/models/ or app/services/ may affect any spec that uses those classes, even without explicit require
- Database schema (db/schema.rb, migrations/) changes → typically requires running all specs due to test DB rebuild
- before(:suite) and before(:all) hooks in helpers run once and cache state — changes to them affect entire spec suites
- Concerns and modules mixed into multiple classes → select specs for all classes that include the changed concern'''
    }
}
