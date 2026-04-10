package com.cifast

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurper

class ResponseParser implements Serializable {

    @NonCPS
    static TestSelection parse(String apiResponse, List<String> allTests) {
        def json = new JsonSlurper().parseText(apiResponse)

        // Bedrock Messages API response: content[].text
        def text = ''
        if (json.content) {
            text = json.content.find { it.type == 'text' }?.text ?: ''
        }

        // Try parsing the entire text as JSON first (expected case)
        try {
            def directParse = new JsonSlurper().parseText(text.trim())
            if (directParse.selected_tests != null) {
                return buildSelection(directParse, allTests)
            }
        } catch (Exception ignored) {}

        // Fallback: extract JSON by finding outermost braces (handles markdown wrapping)
        def start = text.indexOf('{')
        def end = text.lastIndexOf('}')
        if (start >= 0 && end > start) {
            try {
                def extracted = new JsonSlurper().parseText(text.substring(start, end + 1))
                if (extracted.selected_tests != null) {
                    return buildSelection(extracted, allTests)
                }
            } catch (Exception ignored) {}
        }

        return TestSelection.runAllFallback("Could not parse response JSON")
    }

    @NonCPS
    private static TestSelection buildSelection(Map result, List<String> allTests) {
        def selected = (result.selected_tests ?: []) as List<String>
        def confidence = (result.confidence ?: 0.0) as double
        def reasoning = (result.reasoning ?: '') as String

        // Validate and resolve: map selected names to actual test paths
        def validated = []
        selected.each { candidate ->
            def match = allTests.find { actual -> actual == candidate || actual.endsWith("/${candidate}") }
            if (match) validated.add(match)
        }

        // If all selected tests were hallucinated, fall back to running all
        if (validated.isEmpty() && !selected.isEmpty()) {
            return TestSelection.runAllFallback("All selected tests were hallucinated")
        }

        // Penalize confidence if some hallucinated test names were returned
        if (validated.size() < selected.size()) {
            confidence = Math.min(confidence, 0.5)
        }

        def skipped = allTests.findAll { !validated.contains(it) }

        return new TestSelection(
            runAll: false,
            selectedTests: validated,
            skippedTests: skipped,
            confidence: confidence,
            reasoning: reasoning,
            cached: false
        )
    }
}
