package com.catat.app.domain.usecase

import com.catat.app.domain.export.ExportEngine
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.ExportFormat
import com.catat.app.domain.model.ReportStatus
import com.catat.app.domain.repository.ReportRepository
import javax.inject.Inject

sealed class ExportResult {
    data class Success(val content: String, val screenshotPath: String?) : ExportResult()
    data class Failure(val cause: Throwable) : ExportResult()
}

class ExportReportUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
    private val exportEngine: ExportEngine
) {
    suspend fun execute(report: BugReport, format: ExportFormat): ExportResult {
        return try {
            val content = exportEngine.render(report, format)
            val updatedReport = report.copy(
                status = ReportStatus.EXPORTED,
                exportFormat = format,
                updatedAt = System.currentTimeMillis()
            )
            reportRepository.updateReport(updatedReport)
            ExportResult.Success(
                content = content,
                screenshotPath = report.annotatedScreenshotPath ?: report.screenshotPaths.firstOrNull()
            )
        } catch (e: Exception) {
            ExportResult.Failure(e)
        }
    }
}
