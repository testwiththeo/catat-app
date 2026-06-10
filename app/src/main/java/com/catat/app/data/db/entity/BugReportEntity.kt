package com.catat.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bug_reports",
    indices = [
        Index(value = ["status"]),
        Index(value = ["created_at"])
    ]
)
data class BugReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String = "",

    @ColumnInfo(name = "steps_to_reproduce")
    val stepsToReproduce: String = "",

    @ColumnInfo(name = "actual_result")
    val actualResult: String = "",

    @ColumnInfo(name = "expected_result")
    val expectedResult: String = "",

    @ColumnInfo(name = "device_info")
    val deviceInfo: String = "{}",           // JSON

    @ColumnInfo(name = "app_info")
    val appInfo: String = "{}",              // JSON

    val screenshots: String = "[]",          // JSON array of file paths

    @ColumnInfo(name = "annotated_screenshot_path")
    val annotatedScreenshotPath: String? = null,

    val status: String = "DRAFT",

    @ColumnInfo(name = "export_format")
    val exportFormat: String? = null,

    val tags: String = "[]",                 // JSON array

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)
