package com.catat.app.data.repository

import com.catat.app.data.db.entity.BugReportEntity
import com.catat.app.domain.model.AppInfo
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.DeviceInfo
import com.catat.app.domain.model.ExportFormat
import com.catat.app.domain.model.ReportStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object BugReportMapper {

    private val gson = Gson()

    fun BugReport.toEntity(): BugReportEntity = BugReportEntity(
        id = id,
        title = title,
        stepsToReproduce = stepsToReproduce,
        actualResult = actualResult,
        expectedResult = expectedResult,
        deviceInfo = deviceInfo?.let { gson.toJson(it) } ?: "{}",
        appInfo = appInfo?.let { gson.toJson(it) } ?: "{}",
        screenshots = gson.toJson(screenshotPaths),
        annotatedScreenshotPath = annotatedScreenshotPath,
        status = status.name,
        exportFormat = exportFormat?.name(),
        tags = gson.toJson(tags),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun BugReportEntity.toDomain(): BugReport {
        val screenshotListType = object : TypeToken<List<String>>() {}.type
        val tagsListType = object : TypeToken<List<String>>() {}.type
        return BugReport(
            id = id,
            title = title,
            stepsToReproduce = stepsToReproduce,
            actualResult = actualResult,
            expectedResult = expectedResult,
            deviceInfo = try { gson.fromJson(deviceInfo, DeviceInfo::class.java) } catch (e: Exception) { null },
            appInfo = try { gson.fromJson(appInfo, AppInfo::class.java) } catch (e: Exception) { null },
            screenshotPaths = try { gson.fromJson(screenshots, screenshotListType) } catch (e: Exception) { emptyList() },
            annotatedScreenshotPath = annotatedScreenshotPath,
            status = try { ReportStatus.valueOf(status) } catch (e: Exception) { ReportStatus.DRAFT },
            exportFormat = exportFormat?.let { ExportFormat.fromString(it) },
            tags = try { gson.fromJson(tags, tagsListType) } catch (e: Exception) { emptyList() },
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
