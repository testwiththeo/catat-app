package com.catat.app.domain.model

sealed class ExportFormat {
    object Jira : ExportFormat()
    object GitHub : ExportFormat()
    object Linear : ExportFormat()
    object Markdown : ExportFormat()

    fun name(): String = when (this) {
        is Jira -> "jira"
        is GitHub -> "github"
        is Linear -> "linear"
        is Markdown -> "markdown"
    }

    companion object {
        fun fromString(value: String?): ExportFormat = when (value) {
            "jira" -> Jira
            "github" -> GitHub
            "linear" -> Linear
            "markdown" -> Markdown
            else -> Jira
        }
    }
}
