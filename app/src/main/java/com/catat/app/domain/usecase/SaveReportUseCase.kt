package com.catat.app.domain.usecase

import com.catat.app.domain.model.BugReport
import com.catat.app.domain.repository.ReportRepository
import javax.inject.Inject

class SaveReportUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend fun save(report: BugReport): Long = reportRepository.saveReport(report)
    suspend fun update(report: BugReport) = reportRepository.updateReport(report)
}
