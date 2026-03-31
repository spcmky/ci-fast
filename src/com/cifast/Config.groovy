package com.cifast

class Config implements Serializable {
    String baseBranch
    String credentialsId
    String region
    String model
    double confidenceThreshold
    List<String> testGlobs
    int maxDiffLines
    boolean dryRun

    Config(Map params) {
        this.baseBranch          = params.get('baseBranch', 'main')
        this.credentialsId       = params.get('credentialsId', null)
        this.region              = params.get('region', 'us-east-2')
        this.model               = params.get('model', 'us.anthropic.claude-sonnet-4-6-v1')
        this.confidenceThreshold = params.get('confidenceThreshold', 0.7) as double
        this.testGlobs           = params.get('testGlobs', ['**/src/test/**/*.java'])
        this.maxDiffLines        = params.get('maxDiffLines', 3000) as int
        this.dryRun              = params.get('dryRun', false) as boolean
    }
}
