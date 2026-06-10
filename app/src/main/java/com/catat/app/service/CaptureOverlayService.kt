package com.catat.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.catat.app.R
import com.catat.app.data.datastore.AppPreferencesDataStore
import com.catat.app.domain.model.ButtonPosition
import com.catat.app.domain.repository.CaptureTimedOutException
import com.catat.app.domain.repository.SecureContentBlockedException
import com.catat.app.domain.usecase.CaptureScreenshotUseCase
import com.catat.app.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@AndroidEntryPoint
class CaptureOverlayService : Service() {

    @Inject lateinit var windowManager: WindowManager
    @Inject lateinit var captureScreenshotUseCase: CaptureScreenshotUseCase
    @Inject lateinit var preferencesDataStore: AppPreferencesDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val overlayPrefs by lazy {
        getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
    }

    private var overlayView: ImageButton? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var quickActionsView: View? = null
    private var consentPending = false
    private var isCapturing = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startOverlayForeground()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> {
                if (canDrawOverlays()) {
                    showFloatingButton()
                } else {
                    Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
                    stopSelf(startId)
                }
            }
            ACTION_CAPTURE_GRANTED -> {
                val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, RESULT_CODE_MISSING)
                    ?: RESULT_CODE_MISSING
                val data = intent?.mediaProjectionData()
                if (resultCode != RESULT_CODE_MISSING && data != null) {
                    captureWithConsent(resultCode, data)
                } else {
                    if (canDrawOverlays()) showFloatingButton() else stopSelf(startId)
                    Toast.makeText(this, R.string.capture_cancelled, Toast.LENGTH_SHORT).show()
                }
            }
            ACTION_CAPTURE_CANCELLED -> {
                consentPending = false
                isCapturing = false
                if (canDrawOverlays()) showFloatingButton() else stopSelf(startId)
                Toast.makeText(this, R.string.capture_cancelled, Toast.LENGTH_SHORT).show()
            }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        clampFloatingButtonToScreen()
    }

    override fun onDestroy() {
        removeQuickActions()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        serviceScope.cancel()
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingButton() {
        if (!canDrawOverlays()) return
        if (overlayView != null) {
            overlayView?.visibility = View.VISIBLE
            return
        }

        val buttonSize = dp(56)
        val params = createFloatingButtonParams(buttonSize)
        val button = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_camera)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(21, 101, 192))
            }
            elevation = dp(8).toFloat()
            contentDescription = getString(R.string.capture_screenshot)
        }

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        val gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    button.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    requestScreenCaptureConsent()
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    button.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    toggleQuickActions()
                }
            }
        )

        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragging = false

        button.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (!dragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                        dragging = true
                        removeQuickActions()
                    }
                    if (dragging) {
                        params.x = startX + dx
                        params.y = startY + dy
                        clampParams(params)
                        windowManager.updateViewLayout(button, params)
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        persistOverlayPosition(params.x, params.y)
                    }
                    dragging = false
                }
            }
            true
        }

        overlayView = button
        overlayLayoutParams = params
        windowManager.addView(button, params)
    }

    private fun requestScreenCaptureConsent() {
        if (consentPending || isCapturing) return
        consentPending = true
        overlayView?.visibility = View.GONE
        removeQuickActions()

        val intent = Intent(this, MainActivity::class.java)
            .setAction(ACTION_REQUEST_CAPTURE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        runCatching { startActivity(intent) }
            .onFailure { error ->
                Timber.w(error, "Unable to launch screen capture consent")
                consentPending = false
                overlayView?.visibility = View.VISIBLE
                Toast.makeText(this, R.string.capture_cancelled, Toast.LENGTH_SHORT).show()
            }
    }

    private fun captureWithConsent(resultCode: Int, data: Intent) {
        if (isCapturing) return
        consentPending = false
        isCapturing = true
        overlayView?.visibility = View.GONE
        removeQuickActions()

        serviceScope.launch {
            startMediaProjectionForeground()
            delay(OVERLAY_HIDE_DELAY_MS)

            val result = captureScreenshotUseCase.execute(
                resultCode = resultCode,
                mediaProjectionData = data,
                foregroundPackageName = packageName
            )

            result
                .onSuccess { (reportId, screenshotPath) ->
                    Toast.makeText(
                        this@CaptureOverlayService,
                        R.string.capture_saved,
                        Toast.LENGTH_SHORT
                    ).show()
                    openCapturePreview(reportId, screenshotPath)
                }
                .onFailure { error ->
                    val messageRes = when (error) {
                        is SecureContentBlockedException -> R.string.screen_capture_blocked
                        is CaptureTimedOutException -> R.string.capture_timed_out
                        is SecurityException -> R.string.screen_capture_blocked
                        else -> R.string.capture_failed
                    }
                    Timber.w(error, "Capture from overlay failed")
                    Toast.makeText(this@CaptureOverlayService, messageRes, Toast.LENGTH_LONG).show()
                }

            isCapturing = false
            if (canDrawOverlays()) {
                startOverlayForeground()
                showFloatingButton()
            } else {
                stopSelf()
            }
        }
    }

    private fun openCapturePreview(reportId: Long, screenshotPath: String) {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(ACTION_SHOW_CAPTURE)
            .putExtra(EXTRA_REPORT_ID, reportId)
            .putExtra(EXTRA_SCREENSHOT_PATH, screenshotPath)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    private fun toggleQuickActions() {
        if (quickActionsView == null) {
            showQuickActions()
        } else {
            removeQuickActions()
        }
    }

    private fun showQuickActions() {
        if (!canDrawOverlays() || overlayView == null || overlayLayoutParams == null) return

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.WHITE)
            }
            elevation = dp(8).toFloat()
        }

        panel.addView(
            Button(this).apply {
                text = getString(R.string.quick_action_capture)
                setOnClickListener {
                    removeQuickActions()
                    requestScreenCaptureConsent()
                }
            }
        )
        panel.addView(
            Button(this).apply {
                text = getString(R.string.quick_action_hide)
                setOnClickListener {
                    removeQuickActions()
                    stopSelf()
                }
            }
        )

        val params = createQuickActionsParams()
        quickActionsView = panel
        windowManager.addView(panel, params)
    }

    private fun removeQuickActions() {
        quickActionsView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        quickActionsView = null
    }

    private fun createFloatingButtonParams(buttonSize: Int): WindowManager.LayoutParams {
        val screen = screenSize()
        val savedX = overlayPrefs.getInt(PREF_X, Int.MIN_VALUE)
        val savedY = overlayPrefs.getInt(PREF_Y, Int.MIN_VALUE)
        val preferredSide = runCatching {
            runBlocking { preferencesDataStore.preferences.first().floatingButtonPosition }
        }.getOrDefault(ButtonPosition.RIGHT)
        val defaultX = when (preferredSide) {
            ButtonPosition.LEFT -> dp(16)
            ButtonPosition.RIGHT -> (screen.x - buttonSize - dp(16)).coerceAtLeast(0)
        }
        val defaultY = (screen.y / 2 - buttonSize / 2).coerceAtLeast(0)

        return WindowManager.LayoutParams(
            buttonSize,
            buttonSize,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (savedX == Int.MIN_VALUE) defaultX else savedX
            y = if (savedY == Int.MIN_VALUE) defaultY else savedY
            clampParams(this)
        }
    }

    private fun createQuickActionsParams(): WindowManager.LayoutParams {
        val buttonParams = overlayLayoutParams ?: error("Floating button is not attached")
        val buttonSize = buttonParams.width
        val screen = screenSize()
        val panelWidth = dp(196)
        val rightSideX = buttonParams.x + buttonSize + dp(8)
        val leftSideX = buttonParams.x - panelWidth - dp(8)

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (rightSideX + panelWidth <= screen.x) rightSideX else leftSideX.coerceAtLeast(0)
            y = buttonParams.y.coerceIn(0, screen.y - dp(56))
        }
    }

    private fun clampFloatingButtonToScreen() {
        val view = overlayView ?: return
        val params = overlayLayoutParams ?: return
        clampParams(params)
        persistOverlayPosition(params.x, params.y)
        windowManager.updateViewLayout(view, params)
        removeQuickActions()
    }

    private fun clampParams(params: WindowManager.LayoutParams) {
        val screen = screenSize()
        params.x = params.x.coerceIn(0, (screen.x - params.width).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (screen.y - params.height).coerceAtLeast(0))
    }

    private fun persistOverlayPosition(x: Int, y: Int) {
        overlayPrefs.edit()
            .putInt(PREF_X, x)
            .putInt(PREF_Y, y)
            .apply()
    }

    private fun screenSize(): Point {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            val point = Point()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(point)
            point
        }
    }

    private fun canDrawOverlays(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun overlayWindowType(): Int {
        return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    }

    private fun startOverlayForeground() {
        val notification = buildNotification(
            title = getString(R.string.capture_notification_title),
            text = getString(R.string.capture_notification_text)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startMediaProjectionForeground() {
        val notification = buildNotification(
            title = getString(R.string.capture_notification_active_title),
            text = getString(R.string.capture_notification_active_text)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val captureIntent = PendingIntent.getActivity(
            this,
            REQUEST_CAPTURE_CODE,
            Intent(this, MainActivity::class.java).setAction(ACTION_REQUEST_CAPTURE),
            pendingIntentFlags()
        )
        val stopIntent = PendingIntent.getService(
            this,
            REQUEST_STOP_CODE,
            Intent(this, CaptureOverlayService::class.java).setAction(ACTION_STOP),
            pendingIntentFlags()
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(captureIntent)
            .addAction(0, getString(R.string.quick_action_capture), captureIntent)
            .addAction(0, getString(R.string.quick_action_hide), stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.capture_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.capture_notification_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun Intent.mediaProjectionData(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(EXTRA_MEDIA_PROJECTION_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(EXTRA_MEDIA_PROJECTION_DATA)
        }
    }

    companion object {
        const val ACTION_START = "com.catat.app.action.START_CAPTURE_OVERLAY"
        const val ACTION_STOP = "com.catat.app.action.STOP_CAPTURE_OVERLAY"
        const val ACTION_REQUEST_CAPTURE = "com.catat.app.action.REQUEST_CAPTURE"
        const val ACTION_CAPTURE_GRANTED = "com.catat.app.action.CAPTURE_GRANTED"
        const val ACTION_CAPTURE_CANCELLED = "com.catat.app.action.CAPTURE_CANCELLED"
        const val ACTION_SHOW_CAPTURE = "com.catat.app.action.SHOW_CAPTURE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_MEDIA_PROJECTION_DATA = "media_projection_data"
        const val EXTRA_REPORT_ID = "report_id"
        const val EXTRA_SCREENSHOT_PATH = "screenshot_path"

        private const val NOTIFICATION_ID = 2407
        private const val NOTIFICATION_CHANNEL_ID = "capture_overlay"
        private const val REQUEST_CAPTURE_CODE = 4100
        private const val REQUEST_STOP_CODE = 4101
        private const val RESULT_CODE_MISSING = Int.MIN_VALUE
        private const val OVERLAY_PREFS = "capture_overlay"
        private const val PREF_X = "overlay_x"
        private const val PREF_Y = "overlay_y"
        private const val OVERLAY_HIDE_DELAY_MS = 300L

        fun startIntent(context: Context): Intent =
            Intent(context, CaptureOverlayService::class.java).setAction(ACTION_START)

        fun captureGrantedIntent(context: Context, resultCode: Int, data: Intent): Intent =
            Intent(context, CaptureOverlayService::class.java)
                .setAction(ACTION_CAPTURE_GRANTED)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_MEDIA_PROJECTION_DATA, data)

        fun captureCancelledIntent(context: Context): Intent =
            Intent(context, CaptureOverlayService::class.java).setAction(ACTION_CAPTURE_CANCELLED)
    }
}
