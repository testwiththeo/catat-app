package com.catat.app.domain.repository

import android.app.Activity
import android.content.Intent

interface CaptureRepository {
    suspend fun requestCapture(resultCode: Int, mediaProjectionData: Intent): Result<String>
    suspend fun getLastCapturePath(): String?
    fun releaseProjection()
}

class SecureContentBlockedException(
    message: String = "The current screen blocks screenshot capture"
) : IllegalStateException(message)

class CaptureTimedOutException(
    message: String = "Timed out waiting for a screenshot frame"
) : IllegalStateException(message)

fun Int.isSuccessfulCaptureResult(): Boolean = this == Activity.RESULT_OK
