import com.cifast.*

def call(Map params = [:]) {
    def config = new Config(params)

    // 1. Check cache
    def cache = new ResultCache(this)
    def commitSha = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
    if (!config.dryRun) {
        def cached = cache.get(commitSha, config.baseBranch)
        if (cached) {
            echo "[ci-fast] Cache hit for ${commitSha[0..7]}"
            return cached
        }
    }

    // 2. Gather inputs
    def diff = new DiffAnalyzer(this).capture(config.baseBranch, config.maxDiffLines)
    def allTests = new TestDiscovery(this).find(config.testGlobs)

    if (allTests.isEmpty()) {
        echo "[ci-fast] No test files found matching globs, running all tests"
        return TestSelection.runAllFallback("No test files discovered")
    }

    // 3. Call Claude via Bedrock
    def selection
    try {
        selection = new BedrockClient(this, config).analyzeAndSelect(diff, allTests)
    } catch (Exception e) {
        echo "[ci-fast] Bedrock API error: ${e.message}. Falling back to all tests."
        return TestSelection.runAllFallback("API error: ${e.message}")
    }

    // 4. Apply confidence threshold
    if (selection.confidence < config.confidenceThreshold) {
        echo "[ci-fast] Confidence ${selection.confidence} below threshold ${config.confidenceThreshold}. Running all tests."
        selection.runAll = true
    }

    // 5. Dry run reporting
    if (config.dryRun) {
        echo "[ci-fast] DRY RUN - would select ${selection.selectedTests.size()} of ${allTests.size()} tests"
        echo "[ci-fast] Reasoning: ${selection.reasoning}"
        selection.selectedTests.each { echo "[ci-fast]   + ${it}" }
        selection.runAll = true
        return selection
    }

    // 6. Cache and return
    cache.put(commitSha, config.baseBranch, selection)
    echo "[ci-fast] Selected ${selection.selectedTests.size()} of ${allTests.size()} tests (confidence: ${selection.confidence})"
    return selection
}
