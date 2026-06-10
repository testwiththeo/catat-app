package com.catat.app.domain.usecase

import android.content.Intent
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.ReportStatus
import com.catat.app.domain.repository.CaptureRepository
import com.catat.app.domain.repository.DeviceInfoRepository
import com.catat.app.domain.repository.ReportRepository
import javax.inject.Inject

class CaptureScreenshotUseCase @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val reportRepository: ReportRepository
) {

    /**
     * Orchestrates a full capture:
     * 1. Acquires screenshot via MediaProjection
     * 2. Collects device + app context
     * 3. Saves a DRAFT BugReport to the database
     * Returns Result<Pair<reportId, screenshotPath>> on success.
     */
    suspend fun execute(
        resultCode: Int,
        mediaProjectionData: Intent,
        foregroundPackageName: String
    ): Result<Pair<Long, String>> {
        val captureResult = captureRepository.requestCapture(resultCode, mediaProjectionData)
        if (captureResult.isFailure) {
            return Result.failure(captureResult.exceptionOrNull() ?: Exception("Capture failed"))
        }
        val screenshotPath = captureResult.getOrThrow()

        val deviceInfo = deviceInfoRepository.collectDeviceInfo()
        val appInfo = deviceInfoRepository.collectAppInfo(foregroundPackageName)

        val report = BugReport(
            screenshotPaths = listOf(screenshotPath),
            status = ReportStatus.DRAFT,
            deviceInfo = deviceInfo,
            appInfo = appInfo
        )
        val reportId = reportRepository.saveReport(report)
        return Result.success(reportId to screenshotPath)
    }
}
