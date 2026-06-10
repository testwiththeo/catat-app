package com.catat.app.domain.repository

import androidx.paging.PagingData
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.ReportStatus
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun getReportsPaged(): Flow<PagingData<BugReport>>
    fun getReportsPaged(status: ReportStatus?, searchQuery: String): Flow<PagingData<BugReport>>
    fun getReportsByStatus(status: ReportStatus): Flow<List<BugReport>>
    suspend fun getAllReports(): List<BugReport>
    suspend fun getReportById(id: Long): BugReport?
    suspend fun saveReport(report: BugReport): Long
    suspend fun updateReport(report: BugReport)
    suspend fun deleteReport(id: Long)
    suspend fun clearAllReports()
    fun searchReports(query: String): Flow<List<BugReport>>
    fun getReportCount(): Flow<Int>
}
