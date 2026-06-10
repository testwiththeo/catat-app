package com.catat.app.domain.usecase

/**
 * Builds a safe SQLite FTS4 MATCH expression from raw user input.
 *
 * Rules:
 * - Blank/empty input → "*" (match all)
 * - SQL injection characters stripped: ' " ; --
 * - Each token of length >= 2 gets a '*' suffix for prefix matching
 * - Single-character tokens ignored (noise)
 */
object FtsQueryBuilder {

    private val INJECTION_PATTERN = Regex("""['";\-]{2,}|[';"]""")
    private val WHITESPACE_REGEX = Regex("\\s+")

    fun build(userInput: String): String {
        if (userInput.isBlank()) return "*"

        val sanitised = userInput
            .replace("--", "")
            .replace("'", "")
            .replace("\"", "")
            .replace(";", "")
            .trim()

        if (sanitised.isBlank()) return "*"

        val tokens = sanitised
            .split(WHITESPACE_REGEX)
            .filter { it.length >= 2 }
            .map { "$it*" }

        return if (tokens.isEmpty()) "*" else tokens.joinToString(" ")
    }
}
