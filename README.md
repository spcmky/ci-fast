# ci-fast

A Jenkins Shared Library that uses Claude (via AWS Bedrock) to intelligently select which tests to run based on code changes. Reduces CI feedback time by only running tests impacted by your diff.

## How It Works

```
Jenkinsfile calls cifast()
        │
        ▼
┌─────────────────┐
│  Check Cache     │──hit──▶ Return cached TestSelection
│  (SHA + branch)  │
└────────┬────────┘
         │ miss
         ▼
┌─────────────────┐     ┌──────────────────┐
│  DiffAnalyzer    │     │  TestDiscovery    │
│  git diff base.. │     │  find test files  │
│  HEAD, truncate  │     │  by glob patterns │
└────────┬────────┘     └────────┬─────────┘
         │                       │
         └───────────┬───────────┘
                     ▼
          ┌─────────────────────┐
          │  BedrockClient       │
          │  Send diff + test    │
          │  list to Claude via  │
          │  aws bedrock-runtime │
          └──────────┬──────────┘
                     ▼
          ┌─────────────────────┐
          │  ResponseParser      │
          │  Parse JSON response │
          │  Validate test names │
          │  Strip hallucinations│
          └──────────┬──────────┘
                     ▼
          ┌─────────────────────┐
          │  Confidence Check    │
          │  Below threshold?    │
          │  → runAll = true     │
          └──────────┬──────────┘
                     ▼
            Return TestSelection
            (with build-tool formatters)
```

### Step by step

1. **Cache check** — Looks for a cached result keyed by `commitSHA + baseBranch`. If found, returns immediately without calling Bedrock.

2. **Diff capture** (`DiffAnalyzer`) — Runs `git diff <merge-base>...HEAD` excluding lock files, minified assets, and source maps. Truncates to `maxDiffLines` (default 3000) to stay within token limits.

3. **Test discovery** (`TestDiscovery`) — Finds all test files matching your glob patterns (e.g. `**/src/test/**/*.java`). This becomes the "menu" Claude selects from.

4. **Bedrock invocation** (`BedrockClient`) — Writes the request payload to a temp file (avoids shell escaping issues with large diffs), then calls `aws bedrock-runtime invoke-model`. Uses IAM instance profile or Jenkins AWS credentials for auth.

5. **Response parsing** (`ResponseParser`) — Extracts Claude's JSON response containing `selected_tests`, `confidence`, and `reasoning`. Validates every test name against the actual test file list — hallucinated names are stripped and confidence is penalized to 0.5.

6. **Confidence threshold** — If Claude's confidence is below the threshold (default 0.7), the library falls back to running all tests. This is the safety net.

7. **Cache write** — Stores the result for future runs of the same commit.

### Fail-open design

Every failure mode results in running all tests, never skipping them:

| Failure | Behavior |
|---------|----------|
| Bedrock API unreachable | `runAll = true` |
| Response not parseable | `runAll = true` |
| Hallucinated test names | Strip invalid names, penalize confidence |
| Confidence below threshold | `runAll = true` |
| No test files found | `runAll = true` |
| git diff fails | `runAll = true` |

## Quick Start

### 1. Add the shared library

Manage Jenkins → System → Global Pipeline Libraries:
- Name: `ci-fast`
- Default version: `main`
- Source: your Git repo URL

### 2. Store AWS credentials

Either:
- Use an IAM instance profile on your Jenkins agents (no config needed)
- Add AWS credentials in Jenkins: Manage Jenkins → Credentials → Add `AWS Credentials` type

### 3. Use in a Jenkinsfile

```groovy
@Library('ci-fast') _

pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                script {
                    def sel = cifast(
                        baseBranch: 'main',
                        testGlobs: ['**/src/test/**/*.java'],
                    )
                    if (sel.runAll) {
                        sh 'mvn test'
                    } else {
                        sh "mvn test -Dtest=${sel.mavenTestList()}"
                    }
                }
            }
        }
    }
}
```

### Build tool examples

**Gradle:**
```groovy
def sel = cifast(testGlobs: ['**/src/test/**/*.java', '**/src/test/**/*.kt'])
sh sel.runAll ? './gradlew test' : "./gradlew test ${sel.gradleTestList()}"
```

**Jest:**
```groovy
def sel = cifast(testGlobs: ['**/*.test.ts', '**/*.spec.ts'])
sh sel.runAll ? 'npx jest' : "npx jest ${sel.jestTestList()}"
```

**Pytest:**
```groovy
def sel = cifast(testGlobs: ['**/test_*.py', '**/*_test.py'])
sh sel.runAll ? 'pytest' : "pytest ${sel.pytestTestList()}"
```

### Dry run

See what would be selected without actually filtering:

```groovy
cifastDryRun(baseBranch: 'main', testGlobs: ['**/src/test/**/*.java'])
```

## Configuration

| Parameter | Default | Description |
|-----------|---------|-------------|
| `baseBranch` | `main` | Branch to diff against |
| `credentialsId` | `null` | Jenkins AWS credentials ID. `null` = instance profile |
| `region` | `us-east-2` | AWS region for Bedrock |
| `model` | `us.anthropic.claude-sonnet-4-6-v1` | Bedrock model ID |
| `confidenceThreshold` | `0.7` | Below this → run all tests |
| `testGlobs` | `['**/src/test/**/*.java']` | File patterns to discover tests |
| `maxDiffLines` | `3000` | Truncate diff after this many lines |
| `dryRun` | `false` | Report selection without filtering |
| `smallModel` | `us.anthropic.claude-haiku-4-5-v1` | Cheaper model for small diffs |
| `smallDiffThreshold` | `200` | Max diff lines to use small model |
| `smallTestThreshold` | `30` | Max test count to use small model |
| `promptRules` | `''` | Inline rules appended to Claude's system prompt |

## TestSelection object

The `cifast()` call returns a `TestSelection` with:

| Property/Method | Type | Description |
|----------------|------|-------------|
| `runAll` | boolean | `true` if all tests should run (fallback or low confidence) |
| `selectedTests` | List | Test file paths selected by Claude |
| `skippedTests` | List | Test file paths that can be skipped |
| `confidence` | double | 0.0-1.0 confidence score |
| `reasoning` | String | Claude's explanation of the selection |
| `cached` | boolean | Whether the result came from cache |
| `mavenTestList()` | String | Comma-separated FQCNs for `-Dtest=` |
| `gradleTestList()` | String | `--tests` compatible filter |
| `jestTestList()` | String | Space-separated relative paths |
| `pytestTestList()` | String | Space-separated relative paths |

## Customizing Test Selection Rules

ci-fast supports two ways to give Claude project-specific guidance beyond the built-in plugin hints:

### `.ci-fast-rules` file

Create a `.ci-fast-rules` file in your repo root. It's read automatically on every run and appended to the system prompt. This is the recommended approach — it lives in version control, is PR-reviewable, and applies to every build.

```groovy
// Jenkinsfile — no extra config needed, .ci-fast-rules is read automatically
def sel = cifast(plugin: 'maven')
```

### `promptRules` parameter

Pass inline rules via the `cifast()` call for Jenkinsfile-level overrides. Useful for branch-specific or stage-specific rules.

```groovy
def sel = cifast(
    plugin: 'jest',
    promptRules: 'Changes to src/api/ must always include tests/integration/api.test.ts'
)
```

Both can be used together — plugin hints, `.ci-fast-rules`, and `promptRules` are all combined in that order.

### Writing effective `.ci-fast-rules`

Rules should tell Claude things it **cannot infer from the diff alone**: project-specific conventions, implicit coupling, architectural boundaries. Avoid repeating what the plugin already covers.

**Good rules** — project-specific knowledge:
```
Any change to src/db/migrations/ must run ALL tests (schema changes affect everything).
Changes to src/shared/auth/ must include tests for: user-service, admin-service, api-gateway.
The payments module (src/payments/) has a hard dependency on src/billing/ — always select tests for both.
Files in src/generated/ are auto-generated — ignore them when selecting tests.
```

**Bad rules** — too generic or duplicating plugin hints:
```
Changes to package.json should run all tests.          # Plugin already covers this
Test files end in .test.ts.                             # Plugin already knows this
Be careful when selecting tests.                        # Too vague, not actionable
Only run tests that are affected by the changes.        # This is literally the core task
```

### Recommended examples by project type

**Java/Spring monolith:**
```
Changes to src/main/java/com/example/config/ → run ALL tests (Spring context config).
Changes to src/main/resources/db/migration/ → run ALL tests (Flyway migrations).
The OrderService depends on InventoryService and PaymentService — changes to either should include order tests.
src/main/java/com/example/common/ is imported by every module — treat changes as run-all.
```

**TypeScript/React monorepo:**
```
packages/design-system/ is consumed by all apps — changes run all tests.
Changes to packages/api-client/src/generated/ can be ignored (auto-generated from OpenAPI).
The checkout flow spans packages/cart, packages/payment, and packages/order — test all three together.
Any change to .env.example means environment variables changed — run integration tests.
```

**Python/Django:**
```
Changes to any models.py → run ALL tests (DB schema impact).
Changes to myapp/signals.py → run tests for all apps that register signal handlers.
The celery tasks in tasks/ depend on services/ — always test both together.
conftest.py in tests/integration/ sets up the test database — changes run all integration tests.
```

**Go microservice:**
```
Changes to internal/middleware/ affect all HTTP handlers — run all handler tests.
The proto/ directory contains generated code — ignore when selecting tests, but changes to *.proto files should run all tests.
cmd/worker/ and cmd/api/ share internal/domain/ — test both when domain changes.
```

**E2E/Playwright:**
```
Changes to any page object in tests/pages/ → run all specs that import that page object.
The global auth setup in tests/auth.setup.ts affects every authenticated test.
API changes in src/api/routes/ → run the E2E spec that covers that user flow.
Be extra conservative — missed E2E tests are expensive to debug.
```

## Limitations

### LLM-inherent

- **No static analysis** — Claude reasons about the diff textually, not through an AST or dependency graph. It can miss indirect dependencies that aren't obvious from the diff (e.g., a change to a database column default that affects a test via 3 layers of abstraction).
- **Token limits** — Large diffs are truncated at `maxDiffLines`. If the most impactful changes happen to be past the truncation point, they will be missed. The truncation notice lowers Claude's confidence, but it's not guaranteed to trigger a full run.
- **Hallucination** — Claude may return test names that don't exist. The parser catches and strips these, but it indicates the model is guessing rather than matching. Confidence is penalized but not zeroed.
- **Non-deterministic** — The same diff can produce slightly different selections across runs. The cache mitigates this for identical commits, but re-running after cache expiry may yield different results.
- **No learning** — Each invocation is stateless. Claude doesn't learn from past test failures. A test that has historically failed for changes like this one gets no priority boost.

### Architecture

- **No dependency graph** — The library doesn't parse `import` statements, `pom.xml` module dependencies, or build tool dependency trees. It relies entirely on Claude's reasoning about the diff and test file names/paths.
- **Flat test list** — Test discovery is glob-based. It doesn't understand test suites, parameterized tests, or test categories. If a test class contains 500 test methods and only 2 are relevant, the whole class runs.
- **Workspace-local cache** — The cache lives in the Jenkins workspace. Different agents building the same commit won't share cache hits. Workspace cleanup wipes the cache.
- **Single API call** — Very large projects with thousands of tests and a large diff may exceed Bedrock's input token limit. No chunking or multi-call strategy exists yet.
- **AWS-only** — Hardcoded to Bedrock. Can't use Anthropic API directly, Azure, or local models without modifying `BedrockClient`.

### Operational

- **Bedrock latency** — Each uncached call adds 3-15 seconds of wall time for the API round-trip. For small test suites where the full run is <30 seconds, this overhead may not be worth it.
- **Bedrock cost** — Approximately $0.003-0.01 per invocation with Sonnet (varies by diff/test list size). At scale (hundreds of builds/day), this adds up.
- **AWS CLI required** — Jenkins agents must have `aws` CLI installed and configured. The library shells out to it rather than using the Java SDK.
- **Jenkins plugin dependencies** — Requires the `Credentials Binding` and `AWS Credentials` plugins (if not using IAM instance profile).

## Future Improvements

### High impact, low effort

- **Changed-file heuristic pre-filter** — Before calling Claude, match changed file paths to test file paths by naming convention (e.g., `UserService.java` → `UserServiceTest.java`). Use Claude only for the ambiguous cases. Cuts API calls by 40-60% for well-structured projects.
- **Parallel test + full run validation** — Run selected tests immediately, then run the full suite in a background stage. Compare results to measure accuracy over time without slowing the feedback loop.
- **Configurable prompt overrides** — Let users supply a custom system prompt or append project-specific rules (e.g., "any change to `src/db/` must run all integration tests") without forking the library.
- **Metrics collection** — Track selection accuracy (selected tests vs actual failures), API latency, cache hit rate, and cost per build. Publish to CloudWatch or a dashboard.

### High impact, medium effort

- **Import/dependency graph analysis** — Parse `import` statements (Java), `require/import` (JS/TS), or `from ... import` (Python) to build a lightweight dependency graph. Use it as structured context alongside the diff, replacing pure LLM reasoning for direct dependencies.
- **Historical test failure correlation** — Store a mapping of `{changed_files → failed_tests}` from past builds. Feed the top-N historically correlated tests to Claude as "must include" hints. This is the core of what Launchable does.
- **Shared cache (S3/Redis)** — Move the cache from workspace-local to a shared store so all agents benefit from the same commit being analyzed once.
- **Multi-model strategy** — Use Haiku for small diffs (<500 lines, <50 tests) and Sonnet for larger ones. Cut costs by 80% for the simple cases.

### High impact, high effort

- **Feedback loop / fine-tuning** — When the selected tests pass but the full suite (run in validation) catches a failure, log the miss. Use accumulated miss data to improve the prompt or fine-tune a smaller model.
- **Chunked analysis for monorepos** — For projects with 1000+ test files, split the test list into groups by module/package, run parallel Claude calls per group, and merge results. Handles token limits and improves accuracy per-module.
- **Language-aware AST parsing** — Instead of sending raw diff text, send a structured summary: "method `calculate()` in `PricingEngine.java` changed signature from `(int)` to `(int, boolean)`". More token-efficient and less ambiguous for Claude.
- **Flaky test detection** — Track test pass/fail rates per test across builds. Exclude known-flaky tests from selection scoring (run them separately or quarantine them). Prevents flaky tests from inflating the "must run" list.

### Nice to have

- **Slack/Teams notification** — Post selection reasoning to a channel for visibility: "ci-fast skipped 180 of 200 tests for PR #42 (confidence: 0.92)".
- **Jenkins Blue Ocean integration** — Show selected vs skipped tests in the pipeline visualization.
- **Direct Anthropic API support** — Add a `provider: 'anthropic'` config option for teams not on AWS.
- **Gradle/Maven plugin wrappers** — Native build tool plugins that call `cifast` under the hood, so the Jenkinsfile just says `mvn ci-fast:test`.

## Project Structure

```
ci-fast/
├── vars/
│   ├── cifast.groovy              # Main pipeline step entry point
│   └── cifastDryRun.groovy        # Dry-run convenience step
├── src/com/cifast/
│   ├── BedrockClient.groovy       # AWS Bedrock invoke-model via CLI
│   ├── Config.groovy              # Configuration defaults and parsing
│   ├── DiffAnalyzer.groovy        # git diff capture and truncation
│   ├── ResponseParser.groovy      # Claude response → TestSelection
│   ├── ResultCache.groovy         # File-based SHA+branch cache
│   ├── TestDiscovery.groovy       # Glob-based test file discovery
│   └── TestSelection.groovy       # Result object with build-tool formatters
└── resources/com/cifast/prompts/
    ├── system.txt                 # System prompt for Claude
    └── user.txt                   # User prompt template (diff + test list)
```

## Running Tests

```bash
docker build -f Dockerfile.test -t ci-fast-test . && docker run --rm ci-fast-test
```

Tests use [Spock](https://spockframework.org/) and run inside a Gradle container — no local JDK or Jenkins runtime required. The `stubs/` directory provides a `@NonCPS` annotation stub so source compiles without Jenkins on the classpath.

Test files live in `test/com/cifast/` and cover input validation, response parsing, hallucination handling, and build-tool formatters.

## Prerequisites

- Jenkins 2.346+ with Pipeline plugin
- AWS CLI installed on Jenkins agents
- AWS Bedrock access with Claude model enabled in your region
- Jenkins plugins: Credentials Binding, AWS Credentials (if not using instance profile)
