package com.catat.app.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import com.catat.app.data.storage.FileStorage
import com.catat.app.domain.repository.CaptureRepository
import com.catat.app.domain.repository.CaptureTimedOutException
import com.catat.app.domain.repository.SecureContentBlockedException
import com.catat.app.domain.repository.isSuccessfulCaptureResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber

@Singleton
class CaptureRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaProjectionManager: MediaProjectionManager,
    private val windowManager: WindowManager,
    private val fileStorage: FileStorage
) : CaptureRepository {

    private val captureMutex = Mutex()
    private var lastCapturePath: String? = null
    private var activeProjection: MediaProjection? = null
    private var activeVirtualDisplay: VirtualDisplay? = null

    override suspend fun requestCapture(
        resultCode: Int,
        mediaProjectionData: Intent
    ): Result<String> = captureMutex.withLock {
        runCatching {
            if (!resultCode.isSuccessfulCaptureResult()) {
                throw SecurityException("Screen capture consent was not granted")
            }

            val path = captureOnce(resultCode, mediaProjectionData)
            lastCapturePath = path
            path
        }.onFailure { error ->
            Timber.w(error, "Screenshot capture failed")
        }
    }

    override suspend fun getLastCapturePath(): String? = lastCapturePath

    override fun releaseProjection() {
        activeVirtualDisplay?.release()
        activeVirtualDisplay = null
        activeProjection?.stop()
        activeProjection = null
    }

    private suspend fun captureOnce(resultCode: Int, mediaProjectionData: Intent): String {
        val projection = mediaProjectionManager.getMediaProjection(resultCode, mediaProjectionData)
            ?: throw SecurityException("MediaProjection could not be created")
        activeProjection = projection

        val captureSize = resolveCaptureSize()
        val imageReader = ImageReader.newInstance(
            captureSize.width,
            captureSize.height,
            PixelFormat.RGBA_8888,
            MAX_IMAGES
        )
        val handlerThread = HandlerThread("CatatScreenCapture").apply { start() }
        val handler = Handler(handlerThread.looper)
        val projectionCallback = object : MediaProjection.Callback() {
            override fun onStop() {
                activeVirtualDisplay?.release()
                activeVirtualDisplay = null
                activeProjection = null
            }
        }

        projection.registerCallback(projectionCallback, handler)

        return try {
            activeVirtualDisplay = projection.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                captureSize.width,
                captureSize.height,
                captureSize.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                handler
            )

            val bitmap = awaitBitmap(imageReader, captureSize, handler)
            try {
                if (bitmap.isProbablySecureContentBlocked()) {
                    throw SecureContentBlockedException()
                }

                withContext(Dispatchers.IO) {
                    fileStorage.saveBitmap(bitmap, SCREENSHOT_DIR, SCREENSHOT_PREFIX).also {
                        fileStorage.enforceMaxFiles(SCREENSHOT_DIR, MAX_SCREENSHOTS)
                    }
                }
            } finally {
                bitmap.recycle()
            }
        } catch (error: TimeoutCancellationException) {
            throw CaptureTimedOutException()
        } finally {
            imageReader.setOnImageAvailableListener(null, null)
            imageReader.close()
            activeVirtualDisplay?.release()
            activeVirtualDisplay = null
            projection.unregisterCallback(projectionCallback)
            projection.stop()
            activeProjection = null
            handlerThread.quitSafely()
        }
    }

    private suspend fun awaitBitmap(
        imageReader: ImageReader,
        captureSize: CaptureSize,
        handler: Handler
    ): Bitmap = withTimeout(CAPTURE_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            val resumed = AtomicBoolean(false)

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val bitmap = try {
                    image.toBitmap(captureSize.width, captureSize.height)
                } catch (error: Throwable) {
                    image.close()
                    if (resumed.compareAndSet(false, true)) {
                        continuation.resumeWithException(error)
                    }
                    return@setOnImageAvailableListener
                }

                image.close()
                if (resumed.compareAndSet(false, true)) {
                    reader.setOnImageAvailableListener(null, null)
                    continuation.resume(bitmap)
                } else {
                    bitmap.recycle()
                }
            }, handler)

            continuation.invokeOnCancellation {
                imageReader.setOnImageAvailableListener(null, null)
            }
        }
    }

    private fun Image.toBitmap(width: Int, height: Int): Bitmap {
        val plane = planes.firstOrNull() ?: error("Captured image did not contain pixel data")
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val paddedWidth = width + rowPadding / pixelStride

        val paddedBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        paddedBitmap.copyPixelsFromBuffer(buffer)

        if (paddedWidth == width) {
            return paddedBitmap
        }

        return Bitmap.createBitmap(paddedBitmap, 0, 0, width, height).also {
            paddedBitmap.recycle()
        }
    }

    private fun resolveCaptureSize(): CaptureSize {
        val densityDpi = context.resources.displayMetrics.densityDpi
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            CaptureSize(
                width = bounds.width().coerceAtLeast(MIN_CAPTURE_DIMENSION),
                height = bounds.height().coerceAtLeast(MIN_CAPTURE_DIMENSION),
                densityDpi = densityDpi
            )
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            CaptureSize(
                width = metrics.widthPixels.coerceAtLeast(MIN_CAPTURE_DIMENSION),
                height = metrics.heightPixels.coerceAtLeast(MIN_CAPTURE_DIMENSION),
                densityDpi = metrics.densityDpi
            )
        }
    }

    private fun Bitmap.isProbablySecureContentBlocked(): Boolean {
        val xStep = (width / SECURE_SAMPLE_COLUMNS).coerceAtLeast(1)
        val yStep = (height / SECURE_SAMPLE_ROWS).coerceAtLeast(1)
        var darkPixels = 0
        var sampledPixels = 0

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val pixel = getPixel(x, y)
                val alpha = pixel ushr 24
                val red = pixel shr 16 and 0xFF
                val green = pixel shr 8 and 0xFF
                val blue = pixel and 0xFF
                if (alpha == 0 || (red <= DARK_THRESHOLD && green <= DARK_THRESHOLD && blue <= DARK_THRESHOLD)) {
                    darkPixels++
                }
                sampledPixels++
                x += xStep
            }
            y += yStep
        }

        return sampledPixels > 0 &&
            darkPixels.toFloat() / sampledPixels.toFloat() >= SECURE_BLOCKED_DARK_RATIO
    }

    private data class CaptureSize(
        val width: Int,
        val height: Int,
        val densityDpi: Int
    )

    private companion object {
        private const val VIRTUAL_DISPLAY_NAME = "CatatScreenCapture"
        private const val SCREENSHOT_DIR = "screenshots"
        private const val SCREENSHOT_PREFIX = "screenshot"
        private const val MAX_IMAGES = 2
        private const val MAX_SCREENSHOTS = 100
        private const val CAPTURE_TIMEOUT_MS = 5_000L
        private const val MIN_CAPTURE_DIMENSION = 1
        private const val SECURE_SAMPLE_COLUMNS = 32
        private const val SECURE_SAMPLE_ROWS = 32
        private const val DARK_THRESHOLD = 6
        private const val SECURE_BLOCKED_DARK_RATIO = 0.995f
    }
}
