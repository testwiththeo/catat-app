package com.catat.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.catat.app.data.db.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Query("SELECT * FROM templates ORDER BY usage_count DESC")
    fun getAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE category = :category ORDER BY usage_count DESC")
    fun getByCategory(category: String): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: Long): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TemplateEntity): Long

    @Update
    suspend fun update(template: TemplateEntity)

    @Delete
    suspend fun delete(template: TemplateEntity)

    @Query("UPDATE templates SET usage_count = usage_count + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)
}
