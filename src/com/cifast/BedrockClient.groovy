package com.cifast

import groovy.json.JsonOutput

class BedrockClient implements Serializable {
    def steps
    Config config

    BedrockClient(def steps, Config config) {
        this.steps = steps
        this.config = config
    }

    TestSelection analyzeAndSelect(String diff, List<String> allTests) {
        def systemPrompt = steps.libraryResource('com/cifast/prompts/system.txt')
        def userTemplate = steps.libraryResource('com/cifast/prompts/user.txt')

        def testListing = allTests.collect { "- ${it}" }.join('\n')
        def userPrompt = userTemplate
            .replace('{{DIFF}}', diff)
            .replace('{{TEST_FILES}}', testListing)

        def requestBody = JsonOutput.toJson([
            anthropic_version: 'bedrock-2023-05-31',
            max_tokens: 4096,
            system: systemPrompt,
            messages: [
                [role: 'user', content: userPrompt]
            ]
        ])

        // Write request to temp file to avoid shell escaping issues with large diffs
        def requestFile = ".ci-fast-request-${System.currentTimeMillis()}.json"
        def responseFile = ".ci-fast-response-${System.currentTimeMillis()}.json"

        steps.writeFile(file: requestFile, text: requestBody)

        try {
            def awsCmd = buildAwsCommand(requestFile, responseFile)

            if (config.credentialsId) {
                // Uses jenkins-aws-credentials plugin binding
                steps.withCredentials([
                    [
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: config.credentialsId,
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    ]
                ]) {
                    steps.sh(script: awsCmd)
                }
            } else {
                // Use instance profile / environment credentials
                steps.sh(script: awsCmd)
            }

            def responseContent = steps.readFile(file: responseFile)
            return ResponseParser.parse(responseContent, allTests)
        } finally {
            steps.sh(script: "rm -f ${requestFile} ${responseFile}", returnStatus: true)
        }
    }

    private String buildAwsCommand(String requestFile, String responseFile) {
        return """aws bedrock-runtime invoke-model \\
            --region ${config.region} \\
            --model-id ${config.model} \\
            --content-type application/json \\
            --accept application/json \\
            --body fileb://${requestFile} \\
            ${responseFile}"""
    }
}
