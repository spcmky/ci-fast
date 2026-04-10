package com.cifast

import spock.lang.Specification
import groovy.json.JsonOutput

class ResponseParserTest extends Specification {

    def allTests = [
        'src/test/java/com/example/UserServiceTest.java',
        'src/test/java/com/example/OrderServiceTest.java',
        'src/test/java/com/example/PaymentServiceTest.java'
    ]

    private String bedrockResponse(String text) {
        return JsonOutput.toJson([
            content: [[type: 'text', text: text]]
        ])
    }

    def "parses valid JSON response"() {
        given:
        def response = bedrockResponse(JsonOutput.toJson([
            selected_tests: ['src/test/java/com/example/UserServiceTest.java'],
            confidence: 0.9,
            reasoning: 'Changed UserService'
        ]))

        when:
        def result = ResponseParser.parse(response, allTests)

        then:
        !result.runAll
        result.selectedTests.size() == 1
        result.confidence == 0.9
        result.skippedTests.size() == 2
    }

    def "handles JSON wrapped in markdown"() {
        given:
        def json = JsonOutput.toJson([
            selected_tests: ['src/test/java/com/example/UserServiceTest.java'],
            confidence: 0.85,
            reasoning: 'test'
        ])
        def response = bedrockResponse("Here is my analysis:\n```json\n${json}\n```")

        when:
        def result = ResponseParser.parse(response, allTests)

        then:
        !result.runAll
        result.selectedTests.size() == 1
    }

    def "handles braces in reasoning field"() {
        given:
        def response = bedrockResponse(JsonOutput.toJson([
            selected_tests: ['src/test/java/com/example/UserServiceTest.java'],
            confidence: 0.85,
            reasoning: 'Changed {Config} class and Map<String, Object> usage'
        ]))

        when:
        def result = ResponseParser.parse(response, allTests)

        then:
        !result.runAll
        result.selectedTests.size() == 1
    }

    def "strips hallucinated test names and penalizes confidence"() {
        given:
        def response = bedrockResponse(JsonOutput.toJson([
            selected_tests: [
                'src/test/java/com/example/UserServiceTest.java',
                'src/test/java/com/example/FakeTest.java'
            ],
            confidence: 0.9,
            reasoning: 'test'
        ]))

        when:
        def result = ResponseParser.parse(response, allTests)

        then:
        result.selectedTests.size() == 1
        result.selectedTests[0] == 'src/test/java/com/example/UserServiceTest.java'
        result.confidence == 0.5
    }

    def "falls back to runAll when all tests hallucinated"() {
        given:
        def response = bedrockResponse(JsonOutput.toJson([
            selected_tests: ['NonexistentTest.java', 'AnotherFake.java'],
            confidence: 0.9,
            reasoning: 'test'
        ]))

        when:
        def result = ResponseParser.parse(response, allTests)

        then:
        result.runAll
        result.reasoning.contains('hallucinated')
    }

    def "falls back to runAll on unparseable response"() {
        given:
        def response = bedrockResponse("I cannot process this request properly.")

        when:
        def result = ResponseParser.parse(response, allTests)

        then:
        result.runAll
    }

    def "matches test by short name with path boundary"() {
        given:
        def response = bedrockResponse(JsonOutput.toJson([
            selected_tests: ['UserServiceTest.java'],
            confidence: 0.9,
            reasoning: 'test'
        ]))

        when:
        def result = ResponseParser.parse(response, allTests)

        then:
        result.selectedTests.size() == 1
        result.selectedTests[0] == 'src/test/java/com/example/UserServiceTest.java'
    }

    def "short name does not match ambiguous suffix"() {
        given:
        def tests = [
            'src/test/java/com/example/UserServiceTest.java',
            'src/test/java/com/example/SuperUserServiceTest.java'
        ]
        def response = bedrockResponse(JsonOutput.toJson([
            selected_tests: ['UserServiceTest.java'],
            confidence: 0.9,
            reasoning: 'test'
        ]))

        when:
        def result = ResponseParser.parse(response, tests)

        then:
        result.selectedTests.size() == 1
        result.selectedTests[0] == 'src/test/java/com/example/UserServiceTest.java'
        result.skippedTests.contains('src/test/java/com/example/SuperUserServiceTest.java')
    }
}
