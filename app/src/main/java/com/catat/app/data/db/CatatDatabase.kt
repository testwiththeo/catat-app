package com.catat.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.catat.app.data.db.dao.BugReportDao
import com.catat.app.data.db.dao.TemplateDao
import com.catat.app.data.db.entity.BugReportEntity
import com.catat.app.data.db.entity.BugReportFtsEntity
import com.catat.app.data.db.entity.TemplateEntity

@Database(
    entities = [BugReportEntity::class, TemplateEntity::class, BugReportFtsEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CatatDatabase : RoomDatabase() {

    abstract fun bugReportDao(): BugReportDao
    abstract fun templateDao(): TemplateDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE bug_reports ADD COLUMN annotated_screenshot_path TEXT"
                )
            }
        }
    }
}
