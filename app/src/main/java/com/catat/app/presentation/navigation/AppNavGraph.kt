package com.catat.app.presentation.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.catat.app.presentation.screen.annotation.AnnotationScreen
import com.catat.app.presentation.screen.capture.CaptureScreen
import com.catat.app.presentation.screen.export.ExportScreen
import com.catat.app.presentation.screen.history.HistoryScreen
import com.catat.app.presentation.screen.onboarding.OnboardingScreen
import com.catat.app.presentation.screen.reportdetail.ReportDetailScreen
import com.catat.app.presentation.screen.settings.SettingsScreen
import com.catat.app.presentation.screen.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object History : Screen("history")
    object Capture : Screen("capture?path={path}&reportId={reportId}") {
        fun createRoute(path: String?, reportId: Long?) = buildString {
            append("capture")
            val params = buildList {
                if (!path.isNullOrBlank()) add("path=${Uri.encode(path)}")
                if (reportId != null) add("reportId=$reportId")
            }
            if (params.isNotEmpty()) {
                append("?")
                append(params.joinToString("&"))
            }
        }
    }
    object Annotation : Screen("annotation/{screenshotPath}?reportId={reportId}") {
        fun createRoute(path: String, reportId: Long?) =
            "annotation/${Uri.encode(path)}" + (reportId?.let { "?reportId=$it" } ?: "")
    }
    object ReportDetail : Screen("report/{reportId}") {
        fun createRoute(id: Long) = "report/$id"
    }
    object Export : Screen("export/{reportId}") {
        fun createRoute(id: Long) = "export/$id"
    }
    object Settings : Screen("settings")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    onRequestCapture: () -> Unit = {},
    onRequestOverlayPermission: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) }
    ) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onOnboardingRequired = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.History.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNewCapture = onRequestCapture,
                onOpenReport = { id ->
                    navController.navigate(Screen.ReportDetail.createRoute(id))
                },
                onSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.Capture.route,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("reportId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStack ->
            val path = backStack.arguments?.getString("path")?.let(Uri::decode)
            val reportId = backStack.arguments?.getLong("reportId")?.takeIf { it > 0L }
            CaptureScreen(
                capturePath = path,
                reportId = reportId,
                onRetake = onRequestCapture,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onUseCapture = { screenshotPath, id ->
                    navController.navigate(Screen.Annotation.createRoute(screenshotPath, id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Annotation.route,
            arguments = listOf(
                navArgument("screenshotPath") { type = NavType.StringType },
                navArgument("reportId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStack ->
            val path = Uri.decode(backStack.arguments?.getString("screenshotPath") ?: "")
            val reportId = backStack.arguments?.getLong("reportId")?.takeIf { it > 0L }
            AnnotationScreen(
                screenshotPath = path,
                reportId = reportId,
                onDone = { id ->
                    navController.navigate(Screen.ReportDetail.createRoute(id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ReportDetail.route,
            arguments = listOf(navArgument("reportId") { type = NavType.LongType })
        ) { backStack ->
            val id = backStack.arguments?.getLong("reportId") ?: return@composable
            ReportDetailScreen(
                reportId = id,
                onExport = { exportReportId ->
                    navController.navigate(Screen.Export.createRoute(exportReportId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Export.route,
            arguments = listOf(navArgument("reportId") { type = NavType.LongType })
        ) { backStack ->
            val id = backStack.arguments?.getLong("reportId") ?: return@composable
            ExportScreen(
                reportId = id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
