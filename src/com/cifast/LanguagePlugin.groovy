package com.cifast

abstract class LanguagePlugin implements Serializable {
    /** Unique identifier (e.g., "maven", "gradle", "jest") */
    abstract String getId()

    /** Test discovery glob patterns */
    abstract List<String> getTestGlobs()

    /** Format selected test paths for build tool CLI execution */
    abstract String formatTestList(List<String> tests)

    /** Additional files/patterns to exclude from git diff analysis */
    List<String> getDiffExclusions() { return [] }

    /** Language-specific hints appended to the Claude system prompt */
    String getPromptHints() { return '' }
}
