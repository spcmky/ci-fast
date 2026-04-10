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
        'Ruby project using RSpec: changes to spec/spec_helper.rb or spec/rails_helper.rb affect all tests. Changes to shared examples or shared contexts affect all specs that include them. Changes to Gemfile should trigger all tests. Factory changes (spec/factories/) may affect any test using that factory.'
    }
}
