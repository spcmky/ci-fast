package com.cifast

import spock.lang.Specification

class TestSelectionTest extends Specification {

    def "mavenTestList converts paths to FQCNs"() {
        given:
        def sel = new TestSelection(
            selectedTests: [
                'src/test/java/com/example/UserServiceTest.java',
                'src/test/java/com/example/OrderServiceTest.java'
            ]
        )

        when:
        def result = sel.mavenTestList()

        then:
        result == 'com.example.UserServiceTest,com.example.OrderServiceTest'
    }

    def "gradleTestList includes --tests prefix for each entry"() {
        given:
        def sel = new TestSelection(
            selectedTests: [
                'src/test/java/com/example/UserServiceTest.java',
                'src/test/java/com/example/OrderServiceTest.java'
            ]
        )

        when:
        def result = sel.gradleTestList()

        then:
        result == '--tests "com.example.UserServiceTest" --tests "com.example.OrderServiceTest"'
    }

    def "jestTestList joins paths with spaces"() {
        given:
        def sel = new TestSelection(
            selectedTests: ['src/tests/UserService.test.ts', 'src/tests/Order.test.ts']
        )

        when:
        def result = sel.jestTestList()

        then:
        result == 'src/tests/UserService.test.ts src/tests/Order.test.ts'
    }

    def "pytestTestList joins paths with spaces"() {
        given:
        def sel = new TestSelection(
            selectedTests: ['tests/test_user.py', 'tests/test_order.py']
        )

        when:
        def result = sel.pytestTestList()

        then:
        result == 'tests/test_user.py tests/test_order.py'
    }

    def "runAllFallback returns correct defaults"() {
        when:
        def sel = TestSelection.runAllFallback("test reason")

        then:
        sel.runAll
        sel.selectedTests.isEmpty()
        sel.skippedTests.isEmpty()
        sel.confidence == 0.0
        sel.reasoning == 'Fallback: test reason'
        !sel.cached
    }

    def "mavenTestList handles Kotlin files"() {
        given:
        def sel = new TestSelection(
            selectedTests: ['src/test/kotlin/com/example/UserServiceTest.kt']
        )

        when:
        def result = sel.mavenTestList()

        then:
        result == 'com.example.UserServiceTest'
    }
}
