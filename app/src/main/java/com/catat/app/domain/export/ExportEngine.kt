package com.catat.app.domain.export

import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.DeviceInfo
import com.catat.app.domain.model.ExportFormat
import javax.inject.Inject

class ExportEngine @Inject constructor() {

    fun render(report: BugReport, format: ExportFormat): String {
        return when (format) {
            ExportFormat.Jira -> JiraTemplate.render(report)
            ExportFormat.GitHub -> GitHubTemplate.render(report)
            ExportFormat.Linear -> LinearTemplate.render(report)
            ExportFormat.Markdown -> MarkdownTemplate.render(report)
        }
    }

    fun fileExtension(format: ExportFormat): String = when (format) {
        ExportFormat.Jira -> "txt"
        ExportFormat.GitHub,
        ExportFormat.Linear,
        ExportFormat.Markdown -> "md"
    }
}

interface ReportExportTemplate {
    fun render(report: BugReport): String
}

object JiraTemplate : ReportExportTemplate {
    override fun render(report: BugReport): String = buildString {
        appendLine("h2. ${report.title.ifBlank { "Untitled bug" }.escapeJira()}")
        appendLine()
        appendLine("*Steps to Reproduce*")
        appendLine(report.stepsToReproduce.escapeJira().ifBlank { "-" })
        appendLine()
        appendLine("*Actual Result*")
        appendLine(report.actualResult.escapeJira().ifBlank { "-" })
        appendLine()
        appendLine("*Expected Result*")
        appendLine(report.expectedResult.escapeJira().ifBlank { "-" })
        appendLine()
        appendLine("*Device Context*")
        appendLine("{noformat}")
        appendLine(report.deviceInfo.toExportText())
        appendLine("{noformat}")
        report.bestScreenshotName()?.let {
            appendLine()
            appendLine("!$it!")
        }
    }
}

object GitHubTemplate : ReportExportTemplate {
    override fun render(report: BugReport): String = buildMarkdown(report, heading = "#")
}

object LinearTemplate : ReportExportTemplate {
    override fun render(report: BugReport): String = buildMarkdown(report, heading = "##")
}

object MarkdownTemplate : ReportExportTemplate {
    override fun render(report: BugReport): String = buildMarkdown(report, heading = "#")
}

private fun buildMarkdown(report: BugReport, heading: String): String = buildString {
    appendLine("$heading ${report.title.ifBlank { "Untitled bug" }.escapeMarkdownHeading()}")
    appendLine()
    appendLine("## Steps to Reproduce")
    appendLine(report.stepsToReproduce.ifBlank { "-" })
    appendLine()
    appendLine("## Actual Result")
    appendLine(report.actualResult.ifBlank { "-" })
    appendLine()
    appendLine("## Expected Result")
    appendLine(report.expectedResult.ifBlank { "-" })
    appendLine()
    appendLine("## Device Context")
    appendLine("```")
    appendLine(report.deviceInfo.toExportText().replace("```", "'''"))
    appendLine("```")
    report.bestScreenshotName()?.let {
        appendLine()
        appendLine("## Screenshot")
        appendLine("![$it]($it)")
    }
}

private fun BugReport.bestScreenshotName(): String? {
    val path = annotatedScreenshotPath ?: screenshotPaths.firstOrNull()
    return path?.substringAfterLast('/')
}

private fun DeviceInfo?.toExportText(): String {
    if (this == null) return "Not captured"
    return listOf(
        "Model: $deviceModel",
        "Manufacturer: $manufacturer",
        "OS: $osVersion",
        "SDK: $sdkLevel",
        "Build: $buildNumber",
        "Network: $networkType",
        "Battery: $batteryLevel%",
        "RAM available: $ramAvailableMb MB",
        "Storage free: $storageFreeMb MB",
        "Resolution: $screenResolution",
        "Density: $screenDensity dpi"
    ).joinToString("\n")
}

private fun String.escapeJira(): String {
    return buildString {
        this@escapeJira.forEach { char ->
            when (char) {
                '*', '_', '[' -> {
                    append('\\')
                    append(char)
                }
                else -> append(char)
            }
        }
    }
}

private fun String.escapeMarkdownHeading(): String {
    return replace("#", "\\#").replace("\n", " ")
}
