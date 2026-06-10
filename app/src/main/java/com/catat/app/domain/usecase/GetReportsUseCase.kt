package com.catat.app.domain.usecase

import androidx.paging.PagingData
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.ReportStatus
import com.catat.app.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReportsUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    fun execute(): Flow<PagingData<BugReport>> = reportRepository.getReportsPaged()

    fun execute(status: ReportStatus?, searchQuery: String): Flow<PagingData<BugReport>> =
        reportRepository.getReportsPaged(status, searchQuery)
}
