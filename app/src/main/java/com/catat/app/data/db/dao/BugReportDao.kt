package com.catat.app.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.catat.app.data.db.entity.BugReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BugReportDao {

    @Query("SELECT * FROM bug_reports WHERE status != 'ARCHIVED' ORDER BY created_at DESC")
    fun getAllPaged(): PagingSource<Int, BugReportEntity>

    @Query("SELECT * FROM bug_reports WHERE status = :status ORDER BY created_at DESC")
    fun getPagedByStatus(status: String): PagingSource<Int, BugReportEntity>

    @Query("SELECT * FROM bug_reports WHERE id = :id")
    suspend fun getById(id: Long): BugReportEntity?

    @Query("SELECT * FROM bug_reports ORDER BY created_at DESC")
    suspend fun getAllOnce(): List<BugReportEntity>

    @Query("SELECT * FROM bug_reports WHERE status = :status ORDER BY created_at DESC")
    fun getByStatus(status: String): Flow<List<BugReportEntity>>

    @Query("SELECT * FROM bug_reports WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<BugReportEntity>>

    @Query("SELECT * FROM bug_reports WHERE app_info LIKE '%' || :packageName || '%' ORDER BY created_at DESC")
    fun getByApp(packageName: String): Flow<List<BugReportEntity>>

    @RawQuery(observedEntities = [BugReportEntity::class])
    fun search(query: SupportSQLiteQuery): Flow<List<BugReportEntity>>

    @RawQuery(observedEntities = [BugReportEntity::class])
    fun searchPaged(query: SupportSQLiteQuery): PagingSource<Int, BugReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: BugReportEntity): Long

    @Update
    suspend fun update(report: BugReportEntity)

    @Delete
    suspend fun delete(report: BugReportEntity)

    @Query("DELETE FROM bug_reports WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bug_reports WHERE status = :status")
    suspend fun deleteByStatus(status: String)

    @Query("DELETE FROM bug_reports")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM bug_reports")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM bug_reports WHERE status = :status")
    fun getCountByStatus(status: String): Flow<Int>
}
