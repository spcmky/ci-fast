import com.cifast.*

def call(Map params = [:]) {
    // Resolve plugin
    def plugin = resolvePlugin(params)

    // Default testGlobs from plugin if not explicitly set
    if (!params.containsKey('testGlobs')) {
        params.testGlobs = plugin.getTestGlobs()
    }

    def config = new Config(params)
    def metrics = new Metrics(this)

    // Clean up orphaned temp files from aborted runs
    sh(script: "find . -maxdepth 1 \\( -name '.ci-fast-request-*.json' -o -name '.ci-fast-response-*.json' \\) -mmin +30 -delete 2>/dev/null || true", returnStatus: true)

    // 1. Check cache
    def cache = new ResultCache(this)
    def commitSha = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
    if (!config.dryRun) {
        def cached = cache.get(commitSha, config.baseBranch)
        if (cached) {
            cached.plugin = plugin
            echo "[ci-fast] Cache hit for ${commitSha[0..7]}"
            metrics.emit([event: 'cache_hit', sha: commitSha[0..7], plugin: plugin.getId()])
            return cached
        }
    }

    // 2. Gather inputs
    def analyzer = new DiffAnalyzer(this)
    def diff = analyzer.capture(config.baseBranch, config.maxDiffLines, plugin.getDiffExclusions())
    if (analyzer.truncationRatio > 0.5) {
        echo "[ci-fast] Over 50% of diff truncated (${(int)(analyzer.truncationRatio * 100)}%), running all tests"
        metrics.emit([event: 'truncation_fallback', truncationRatio: analyzer.truncationRatio])
        return TestSelection.runAllFallback("Diff truncation exceeded 50%")
    }
    def allTests = new TestDiscovery(this).find(config.testGlobs)

    if (allTests.isEmpty()) {
        echo "[ci-fast] No test files found matching globs, running all tests"
        return TestSelection.runAllFallback("No test files discovered")
    }

    // 3. Check circuit breaker
    def breaker = new CircuitBreaker(this)
    if (breaker.isOpen()) {
        echo "[ci-fast] Circuit breaker open (too many recent failures). Running all tests."
        metrics.emit([event: 'circuit_breaker_open'])
        return TestSelection.runAllFallback("Circuit breaker open")
    }

    // 4. Auto-select model for small inputs
    if (analyzer.diffLineCount < config.smallDiffThreshold && allTests.size() < config.smallTestThreshold) {
        echo "[ci-fast] Small input detected, using ${config.smallModel}"
        config.model = config.smallModel
    }

    // 5. Call Claude via Bedrock
    def selection
    def startTime = System.currentTimeMillis()
    try {
        selection = new BedrockClient(this, config).analyzeAndSelect(diff, allTests, plugin.getPromptHints())
    } catch (Exception e) {
        echo "[ci-fast] Bedrock API error: ${e.message}. Falling back to all tests."
        breaker.recordFailure()
        metrics.emit([event: 'api_error', error: e.message])
        return TestSelection.runAllFallback("API error: ${e.message}")
    }
    def apiDurationMs = System.currentTimeMillis() - startTime
    selection.plugin = plugin

    // 6. Apply confidence threshold
    if (selection.confidence < config.confidenceThreshold) {
        echo "[ci-fast] Confidence ${selection.confidence} below threshold ${config.confidenceThreshold}. Running all tests."
        selection.runAll = true
    }

    // 7. Dry run reporting
    if (config.dryRun) {
        echo "[ci-fast] DRY RUN - would select ${selection.selectedTests.size()} of ${allTests.size()} tests"
        echo "[ci-fast] Reasoning: ${selection.reasoning}"
        selection.selectedTests.each { echo "[ci-fast]   + ${it}" }
        selection.runAll = true
        return selection
    }

    // 8. Cache and return
    cache.put(commitSha, config.baseBranch, selection)
    cache.cleanup()
    metrics.emit([
        event: 'selection',
        sha: commitSha[0..7],
        model: config.model,
        plugin: plugin.getId(),
        selected: selection.selectedTests.size(),
        total: allTests.size(),
        confidence: selection.confidence,
        runAll: selection.runAll,
        apiDurationMs: apiDurationMs,
        diffLines: analyzer.diffLineCount
    ])
    echo "[ci-fast] Selected ${selection.selectedTests.size()} of ${allTests.size()} tests (confidence: ${selection.confidence})"
    return selection
}

private LanguagePlugin resolvePlugin(Map params) {
    if (params.plugin instanceof LanguagePlugin) {
        return params.plugin
    }
    if (params.plugin instanceof String) {
        return PluginRegistry.resolve(params.plugin as String)
    }
    def detected = PluginRegistry.detect(this)
    if (detected) {
        echo "[ci-fast] Auto-detected plugin: ${detected.getId()}"
        return detected
    }
    return PluginRegistry.resolve('maven')
}
