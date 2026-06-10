package com.catat.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = BugReportEntity::class)
@Entity(tableName = "bug_reports_fts")
data class BugReportFtsEntity(
    val title: String,

    @ColumnInfo(name = "steps_to_reproduce")
    val stepsToReproduce: String,

    @ColumnInfo(name = "actual_result")
    val actualResult: String,

    @ColumnInfo(name = "expected_result")
    val expectedResult: String
)
