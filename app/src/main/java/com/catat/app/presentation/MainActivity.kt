package com.catat.app.presentation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.catat.app.R
import com.catat.app.presentation.navigation.AppNavGraph
import com.catat.app.presentation.navigation.Screen
import com.catat.app.presentation.theme.CatatTheme
import com.catat.app.service.CaptureOverlayService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var mediaProjectionManager: MediaProjectionManager

    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private var navController: NavHostController? = null
    private var pendingRoute: String? = null
    private var initialIntentHandled = false
    private var notificationPermissionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        registerLaunchers()

        maybeRequestNotificationPermission()

        setContent {
            CatatTheme {
                val controller = rememberNavController()

                LaunchedEffect(controller) {
                    navController = controller
                    if (!initialIntentHandled) {
                        initialIntentHandled = true
                        handleIntent(intent)
                    }
                    pendingRoute?.let { route ->
                        pendingRoute = null
                        controller.navigate(route) { launchSingleTop = true }
                    }
                }

                AppNavGraph(
                    navController = controller,
                    onRequestCapture = ::launchScreenCaptureConsent,
                    onRequestOverlayPermission = ::openOverlayPermissionSettings
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tryStartCaptureOverlayService()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun registerLaunchers() {
        screenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                ContextCompat.startForegroundService(
                    this,
                    CaptureOverlayService.captureGrantedIntent(this, result.resultCode, data)
                )
                navigateToCapturePreview(path = null, reportId = null)
            } else {
                ContextCompat.startForegroundService(
                    this,
                    CaptureOverlayService.captureCancelledIntent(this)
                )
                Toast.makeText(this, R.string.capture_cancelled, Toast.LENGTH_SHORT).show()
            }
        }

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    R.string.notification_permission_required,
                    Toast.LENGTH_LONG
                ).show()
            }
            tryStartCaptureOverlayService()
        }
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            CaptureOverlayService.ACTION_REQUEST_CAPTURE -> launchScreenCaptureConsent()
            CaptureOverlayService.ACTION_SHOW_CAPTURE -> {
                navigateToCapturePreview(
                    path = intent.getStringExtra(CaptureOverlayService.EXTRA_SCREENSHOT_PATH),
                    reportId = intent.getLongExtra(CaptureOverlayService.EXTRA_REPORT_ID, NO_REPORT_ID)
                        .takeIf { it != NO_REPORT_ID }
                )
            }
        }
    }

    private fun launchScreenCaptureConsent() {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }

    private fun navigateToCapturePreview(path: String?, reportId: Long?) {
        val route = Screen.Capture.createRoute(path, reportId)
        val controller = navController
        if (controller == null) {
            pendingRoute = route
        } else {
            controller.navigate(route) { launchSingleTop = true }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationPermissionRequested) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        notificationPermissionRequested = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun tryStartCaptureOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }
        ContextCompat.startForegroundService(this, CaptureOverlayService.startIntent(this))
    }

    private fun openOverlayPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            tryStartCaptureOverlayService()
            return
        }

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private companion object {
        private const val NO_REPORT_ID = Long.MIN_VALUE
    }
}
