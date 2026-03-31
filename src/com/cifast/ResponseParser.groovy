package com.cifast

import groovy.json.JsonSlurper

class ResponseParser implements Serializable {

    static TestSelection parse(String apiResponse, List<String> allTests) {
        def json = new JsonSlurper().parseText(apiResponse)

        // Bedrock Messages API response: content[].text
        def text = ''
        if (json.content) {
            text = json.content.find { it.type == 'text' }?.text ?: ''
        }

        // Extract JSON object from response text
        def jsonMatch = (text =~ /(?s)\{[^{}]*"selected_tests"[^{}]*\}/)
        if (!jsonMatch.find()) {
            // Try parsing the entire text as JSON
            try {
                def directParse = new JsonSlurper().parseText(text.trim())
                if (directParse.selected_tests != null) {
                    return buildSelection(directParse, allTests)
                }
            } catch (Exception ignored) {}
            return TestSelection.runAllFallback("Could not parse response JSON")
        }

        def result
        try {
            result = new JsonSlurper().parseText(jsonMatch[0])
        } catch (Exception e) {
            return TestSelection.runAllFallback("JSON parse error: ${e.message}")
        }

        return buildSelection(result, allTests)
    }

    private static TestSelection buildSelection(Map result, List<String> allTests) {
        def selected = (result.selected_tests ?: []) as List<String>
        def confidence = (result.confidence ?: 0.0) as double
        def reasoning = (result.reasoning ?: '') as String

        // Validate: every selected test must exist in allTests
        def validated = selected.findAll { candidate ->
            allTests.any { actual -> actual == candidate || actual.endsWith(candidate) }
        }

        // Penalize confidence if hallucinated test names were returned
        if (validated.size() < selected.size()) {
            def hallucinated = selected.size() - validated.size()
            confidence = Math.min(confidence, 0.5)
        }

        def skipped = allTests.findAll { actual ->
            !validated.any { sel -> actual == sel || actual.endsWith(sel) }
        }

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
