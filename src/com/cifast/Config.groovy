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
        this.baseBranch          = sanitizeBranch(params.get('baseBranch', 'main') as String)
        this.credentialsId       = params.get('credentialsId', null)
        this.region              = sanitize(params.get('region', 'us-east-2') as String, /^[a-z0-9\-]+$/, 'region')
        this.model               = sanitize(params.get('model', 'us.anthropic.claude-sonnet-4-6-v1') as String, /^[a-zA-Z0-9._:\-]+$/, 'model')
        this.confidenceThreshold = Math.max(0.0, Math.min(1.0, params.get('confidenceThreshold', 0.7) as double))
        this.testGlobs           = sanitizeGlobs(params.get('testGlobs', ['**/src/test/**/*.java']) as List<String>)
        this.maxDiffLines        = Math.max(1, params.get('maxDiffLines', 3000) as int)
        this.dryRun              = params.get('dryRun', false) as boolean
    }

    private static String sanitizeBranch(String branch) {
        sanitize(branch, /^[a-zA-Z0-9\/_.\-]+$/, 'baseBranch')
        if (branch.contains('..')) {
            throw new IllegalArgumentException("Invalid baseBranch: path traversal not allowed")
        }
        return branch
    }

    private static String sanitize(String value, String pattern, String name) {
        if (!(value ==~ pattern)) {
            throw new IllegalArgumentException("Invalid ${name}: contains disallowed characters")
        }
        return value
    }

    private static List<String> sanitizeGlobs(List<String> globs) {
        globs.each { glob ->
            if (!(glob ==~ /^[a-zA-Z0-9\/*._\-\[\]{}?]+$/)) {
                throw new IllegalArgumentException("Invalid test glob pattern: ${glob}")
            }
        }
        return globs
    }
}
