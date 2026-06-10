package com.catat.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.sqlite.db.SimpleSQLiteQuery
import com.catat.app.data.db.dao.BugReportDao
import com.catat.app.data.repository.BugReportMapper.toDomain
import com.catat.app.data.repository.BugReportMapper.toEntity
import com.catat.app.data.storage.FileStorage
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.ReportStatus
import com.catat.app.domain.repository.ReportRepository
import com.catat.app.domain.usecase.FtsQueryBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val dao: BugReportDao,
    private val fileStorage: FileStorage
) : ReportRepository {

    companion object {
        private const val PAGE_SIZE = 20
    }

    override fun getReportsPaged(): Flow<PagingData<BugReport>> {
        return getReportsPaged(status = null, searchQuery = "")
    }

    override fun getReportsPaged(
        status: ReportStatus?,
        searchQuery: String
    ): Flow<PagingData<BugReport>> {
        val ftsQuery = FtsQueryBuilder.build(searchQuery)
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)
        ) {
            when {
                searchQuery.isNotBlank() && ftsQuery != "*" -> {
                    dao.searchPaged(buildPagedSearchQuery(status, ftsQuery))
                }
                status != null -> dao.getPagedByStatus(status.name)
                else -> dao.getAllPaged()
            }
        }.flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    override fun getReportsByStatus(status: ReportStatus): Flow<List<BugReport>> {
        return dao.getByStatus(status.name).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllReports(): List<BugReport> {
        return dao.getAllOnce().map { it.toDomain() }
    }

    override suspend fun getReportById(id: Long): BugReport? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun saveReport(report: BugReport): Long {
        return dao.insert(report.toEntity())
    }

    override suspend fun updateReport(report: BugReport) {
        dao.update(report.toEntity())
    }

    override suspend fun deleteReport(id: Long) {
        val entity = dao.getById(id) ?: return
        dao.deleteById(id)
        cleanupFiles(entity)
    }

    override suspend fun clearAllReports() {
        val reports = dao.getAllOnce()
        dao.deleteAll()
        reports.forEach(::cleanupFiles)
    }

    override fun searchReports(query: String): Flow<List<BugReport>> {
        val sqlQuery = SimpleSQLiteQuery(
            """
            SELECT * FROM bug_reports
            WHERE id IN (
                SELECT rowid FROM bug_reports_fts
                WHERE bug_reports_fts MATCH ?
            )
            ORDER BY created_at DESC
            """.trimIndent(),
            arrayOf(query)
        )
        return dao.search(sqlQuery).map { list -> list.map { it.toDomain() } }
    }

    private fun buildPagedSearchQuery(status: ReportStatus?, ftsQuery: String): SimpleSQLiteQuery {
        val where = buildString {
            append("status != ?")
            if (status != null) {
                append(" AND status = ?")
            }
            append(
                """
                 AND id IN (
                    SELECT rowid FROM bug_reports_fts
                    WHERE bug_reports_fts MATCH ?
                )
                """.trimIndent()
            )
        }
        val args = mutableListOf<Any>("ARCHIVED")
        if (status != null) {
            args += status.name
        }
        args += ftsQuery

        return SimpleSQLiteQuery(
            """
            SELECT * FROM bug_reports
            WHERE $where
            ORDER BY created_at DESC
            """.trimIndent(),
            args.toTypedArray()
        )
    }

    override fun getReportCount(): Flow<Int> {
        return dao.getCount()
    }

    private fun cleanupFiles(entity: com.catat.app.data.db.entity.BugReportEntity) {
        try {
            val screenshotListType = object : TypeToken<List<String>>() {}.type
            val screenshots: List<String> = Gson().fromJson(entity.screenshots, screenshotListType)
                ?: emptyList()
            (screenshots + listOfNotNull(entity.annotatedScreenshotPath)).distinct().forEach { path ->
                fileStorage.delete(path)
            }
        } catch (e: Exception) {
            Timber.w(e, "Error cleaning up screenshot files for report ${entity.id}")
        }
    }
}
