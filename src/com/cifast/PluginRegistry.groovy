package com.cifast

import com.cifast.plugins.*

class PluginRegistry implements Serializable {

    private static final Map<String, LanguagePlugin> BUILTIN = [
        maven     : new MavenPlugin(),
        gradle    : new GradlePlugin(),
        junit5    : new JUnit5Plugin(),
        jest      : new JestPlugin(),
        pytest    : new PytestPlugin(),
        go        : new GoPlugin(),
        rspec     : new RSpecPlugin(),
        karma     : new KarmaPlugin(),
        playwright: new PlaywrightPlugin()
    ]

    static LanguagePlugin resolve(String id) {
        def plugin = BUILTIN.get(id)
        if (!plugin) {
            throw new IllegalArgumentException("Unknown plugin: ${id}. Available: ${BUILTIN.keySet().join(', ')}")
        }
        return plugin
    }

    static LanguagePlugin detect(def steps) {
        if (steps.fileExists('pom.xml')) return BUILTIN.maven
        if (steps.fileExists('build.gradle') || steps.fileExists('build.gradle.kts')) return BUILTIN.gradle
        if (steps.fileExists('package.json')) return BUILTIN.jest
        if (steps.fileExists('go.mod')) return BUILTIN.go
        if (steps.fileExists('pytest.ini') || steps.fileExists('setup.py') || steps.fileExists('pyproject.toml')) return BUILTIN.pytest
        if (steps.fileExists('Gemfile')) return BUILTIN.rspec
        if (steps.fileExists('karma.conf.js') || steps.fileExists('karma.conf.ts')) return BUILTIN.karma
        if (steps.fileExists('playwright.config.ts') || steps.fileExists('playwright.config.js')) return BUILTIN.playwright
        return null
    }

    static List<String> available() {
        return BUILTIN.keySet().toList()
    }
}
