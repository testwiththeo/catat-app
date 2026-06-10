package com.catat.app.domain.usecase

import androidx.paging.PagingData
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.ReportStatus
import com.catat.app.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SaveReportUseCaseTest {

    @Test
    fun `save delegates to repository and returns id`() = runTest {
        val repository = InMemoryReportRepository()
        val useCase = SaveReportUseCase(repository)

        val id = useCase.save(BugReport(title = "Crash"))

        assertEquals(1L, id)
        assertEquals("Crash", repository.savedReports.first().title)
    }

    @Test
    fun `update delegates to repository`() = runTest {
        val repository = InMemoryReportRepository()
        val useCase = SaveReportUseCase(repository)

        useCase.update(BugReport(id = 4L, title = "Updated"))

        assertNotNull(repository.updatedReport)
        assertEquals("Updated", repository.updatedReport?.title)
    }
}

private class InMemoryReportRepository : ReportRepository {
    val savedReports = mutableListOf<BugReport>()
    var updatedReport: BugReport? = null

    override fun getReportsPaged(): Flow<PagingData<BugReport>> = flowOf(PagingData.empty())
    override fun getReportsPaged(status: ReportStatus?, searchQuery: String): Flow<PagingData<BugReport>> =
        flowOf(PagingData.empty())
    override fun getReportsByStatus(status: ReportStatus): Flow<List<BugReport>> = flowOf(emptyList())
    override suspend fun getAllReports(): List<BugReport> = savedReports
    override suspend fun getReportById(id: Long): BugReport? = savedReports.firstOrNull { it.id == id }
    override suspend fun saveReport(report: BugReport): Long {
        savedReports += report.copy(id = savedReports.size + 1L)
        return savedReports.last().id
    }
    override suspend fun updateReport(report: BugReport) {
        updatedReport = report
    }
    override suspend fun deleteReport(id: Long) = Unit
    override suspend fun clearAllReports() = savedReports.clear()
    override fun searchReports(query: String): Flow<List<BugReport>> = flowOf(emptyList())
    override fun getReportCount(): Flow<Int> = flowOf(savedReports.size)
}
