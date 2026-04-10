package com.cifast

import com.cifast.plugins.*
import spock.lang.Specification

class PluginTest extends Specification {

    def "PluginRegistry resolves all built-in plugins"() {
        expect:
        PluginRegistry.resolve(id).getId() == id

        where:
        id << ['maven', 'gradle', 'junit5', 'jest', 'pytest', 'go', 'rspec', 'karma', 'playwright']
    }

    def "PluginRegistry throws on unknown plugin"() {
        when:
        PluginRegistry.resolve('unknown')

        then:
        thrown(IllegalArgumentException)
    }

    def "PluginRegistry lists available plugins"() {
        expect:
        PluginRegistry.available().containsAll(['maven', 'gradle', 'junit5', 'jest', 'pytest', 'go', 'rspec', 'karma', 'playwright'])
    }

    def "MavenPlugin formats test list as FQCNs"() {
        given:
        def plugin = new MavenPlugin()
        def tests = [
            'src/test/java/com/example/UserServiceTest.java',
            'src/test/java/com/example/OrderServiceTest.java'
        ]

        expect:
        plugin.formatTestList(tests) == 'com.example.UserServiceTest,com.example.OrderServiceTest'
    }

    def "GradlePlugin formats test list with --tests flags"() {
        given:
        def plugin = new GradlePlugin()
        def tests = [
            'src/test/java/com/example/UserServiceTest.java',
            'src/test/java/com/example/OrderServiceTest.java'
        ]

        expect:
        plugin.formatTestList(tests) == '--tests "com.example.UserServiceTest" --tests "com.example.OrderServiceTest"'
    }

    def "JestPlugin formats test list as space-separated paths"() {
        given:
        def plugin = new JestPlugin()
        def tests = ['src/components/Button.test.tsx', 'src/utils/format.test.ts']

        expect:
        plugin.formatTestList(tests) == 'src/components/Button.test.tsx src/utils/format.test.ts'
    }

    def "PytestPlugin formats test list as space-separated paths"() {
        given:
        def plugin = new PytestPlugin()
        def tests = ['tests/test_user.py', 'tests/test_order.py']

        expect:
        plugin.formatTestList(tests) == 'tests/test_user.py tests/test_order.py'
    }

    def "GoPlugin formats test list as package paths"() {
        given:
        def plugin = new GoPlugin()
        def tests = [
            'internal/auth/auth_test.go',
            'internal/auth/token_test.go',
            'cmd/server/main_test.go'
        ]

        expect:
        plugin.formatTestList(tests) == './internal/auth/... ./cmd/server/...'
    }

    def "all plugins provide structured prompt hints"() {
        expect:
        def hints = plugin.getPromptHints()
        hints != null
        hints.contains('Naming conventions:')
        hints.contains('Implicit dependencies:')
        hints.contains('Framework gotchas:')

        where:
        plugin << [new MavenPlugin(), new GradlePlugin(), new JUnit5Plugin(), new JestPlugin(), new PytestPlugin(), new GoPlugin(), new RSpecPlugin(), new KarmaPlugin(), new PlaywrightPlugin()]
    }

    def "all plugins provide test globs"() {
        expect:
        !plugin.getTestGlobs().isEmpty()

        where:
        plugin << [new MavenPlugin(), new GradlePlugin(), new JUnit5Plugin(), new JestPlugin(), new PytestPlugin(), new GoPlugin(), new RSpecPlugin(), new KarmaPlugin(), new PlaywrightPlugin()]
    }

    def "JUnit5Plugin formats test list with --include-classname flags"() {
        given:
        def plugin = new JUnit5Plugin()
        def tests = [
            'src/test/java/com/example/UserServiceTest.java',
            'src/test/java/com/example/OrderServiceTest.java'
        ]

        expect:
        plugin.formatTestList(tests) == '--include-classname "^com.example.UserServiceTest$" --include-classname "^com.example.OrderServiceTest$"'
    }

    def "RSpecPlugin formats test list as space-separated paths"() {
        given:
        def plugin = new RSpecPlugin()
        def tests = ['spec/models/user_spec.rb', 'spec/services/auth_spec.rb']

        expect:
        plugin.formatTestList(tests) == 'spec/models/user_spec.rb spec/services/auth_spec.rb'
    }

    def "KarmaPlugin formats test list as comma-separated paths"() {
        given:
        def plugin = new KarmaPlugin()
        def tests = ['src/app/user/user.component.spec.ts', 'src/app/auth/auth.service.spec.ts']

        expect:
        plugin.formatTestList(tests) == 'src/app/user/user.component.spec.ts,src/app/auth/auth.service.spec.ts'
    }

    def "PlaywrightPlugin formats test list as space-separated paths"() {
        given:
        def plugin = new PlaywrightPlugin()
        def tests = ['tests/login.spec.ts', 'tests/checkout.spec.ts']

        expect:
        plugin.formatTestList(tests) == 'tests/login.spec.ts tests/checkout.spec.ts'
    }

    def "TestSelection.formatTestList() delegates to plugin"() {
        given:
        def sel = new TestSelection(
            selectedTests: ['src/test/java/com/example/UserServiceTest.java'],
            plugin: new MavenPlugin()
        )

        expect:
        sel.formatTestList() == 'com.example.UserServiceTest'
    }

    def "TestSelection.formatTestList() throws without plugin"() {
        given:
        def sel = new TestSelection(selectedTests: ['test.java'])

        when:
        sel.formatTestList()

        then:
        thrown(IllegalStateException)
    }

    def "TestSelection.formatForPlugin() uses provided plugin"() {
        given:
        def sel = new TestSelection(
            selectedTests: ['src/test/java/com/example/UserServiceTest.java'],
            plugin: new MavenPlugin()
        )

        expect:
        sel.formatForPlugin(new GradlePlugin()) == '--tests "com.example.UserServiceTest"'
    }
}
