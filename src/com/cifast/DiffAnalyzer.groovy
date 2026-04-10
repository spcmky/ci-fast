package com.cifast

import com.cloudbees.groovy.cps.NonCPS

class DiffAnalyzer implements Serializable {
    def steps
    double truncationRatio = 0.0
    int diffLineCount = 0

    DiffAnalyzer(def steps) {
        this.steps = steps
    }

    String capture(String baseBranch, int maxLines) {
        // Fetch base branch with sufficient depth for merge-base
        steps.sh(script: "git fetch origin ${baseBranch} --deepen=50 2>/dev/null || git fetch origin ${baseBranch} --depth=50 2>/dev/null || true", returnStatus: true)

        def mergeBase = steps.sh(
            script: "git merge-base origin/${baseBranch} HEAD 2>/dev/null || git rev-parse origin/${baseBranch} 2>/dev/null || echo ''",
            returnStdout: true
        ).trim()

        if (!mergeBase) {
            return ''
        }

        def diff = steps.sh(
            script: "git diff ${mergeBase}...HEAD -- . ':!package-lock.json' ':!yarn.lock' ':!Gemfile.lock' ':!composer.lock' ':!pnpm-lock.yaml' ':!*.min.js' ':!*.min.css' ':!*.map'",
            returnStdout: true
        ).trim()

        if (!diff) {
            return ''
        }

        this.diffLineCount = computeLineCount(diff)
        this.truncationRatio = this.diffLineCount > maxLines ? (this.diffLineCount - maxLines) / (double) this.diffLineCount : 0.0
        return truncate(diff, maxLines)
    }

    @NonCPS
    static int computeLineCount(String text) {
        return text.readLines().size()
    }

    @NonCPS
    static String truncate(String diff, int maxLines) {
        def lines = diff.readLines()
        if (lines.size() > maxLines) {
            def truncated = lines.take(maxLines).join('\n')
            return truncated + "\n\n[TRUNCATED: ${lines.size() - maxLines} lines omitted]"
        }
        return diff
    }
}
