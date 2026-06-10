package com.catat.app.domain.model

data class BugReport(
    val id: Long = 0,
    val title: String = "",
    val stepsToReproduce: String = "",
    val actualResult: String = "",
    val expectedResult: String = "",
    val deviceInfo: DeviceInfo? = null,
    val appInfo: AppInfo? = null,
    val screenshotPaths: List<String> = emptyList(),
    val annotatedScreenshotPath: String? = null,
    val status: ReportStatus = ReportStatus.DRAFT,
    val exportFormat: ExportFormat? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
