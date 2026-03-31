package com.cifast

class DiffAnalyzer implements Serializable {
    def steps

    DiffAnalyzer(def steps) {
        this.steps = steps
    }

    String capture(String baseBranch, int maxLines) {
        // Fetch base branch for shallow clones
        steps.sh(script: "git fetch origin ${baseBranch} --depth=1 2>/dev/null || true", returnStatus: true)

        def mergeBase = steps.sh(
            script: "git merge-base origin/${baseBranch} HEAD 2>/dev/null || git rev-parse origin/${baseBranch}",
            returnStdout: true
        ).trim()

        def diff = steps.sh(
            script: "git diff ${mergeBase}...HEAD -- . ':!*.lock' ':!package-lock.json' ':!*.min.js' ':!*.min.css' ':!*.map'",
            returnStdout: true
        ).trim()

        if (!diff) {
            return ''
        }

        // Truncate to stay within token limits
        def lines = diff.readLines()
        if (lines.size() > maxLines) {
            def truncated = lines.take(maxLines).join('\n')
            return truncated + "\n\n[TRUNCATED: ${lines.size() - maxLines} lines omitted]"
        }
        return diff
    }
}
