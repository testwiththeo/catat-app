package com.catat.app.domain.usecase

import com.catat.app.domain.model.BugReport
import com.catat.app.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchReportsUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    fun execute(query: String): Flow<List<BugReport>> {
        val ftsQuery = FtsQueryBuilder.build(query)
        return reportRepository.searchReports(ftsQuery)
    }
}
