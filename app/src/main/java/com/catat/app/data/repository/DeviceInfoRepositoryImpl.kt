package com.catat.app.data.repository

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import com.catat.app.domain.model.AppInfo
import com.catat.app.domain.model.DeviceInfo
import com.catat.app.domain.repository.DeviceInfoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceInfoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityManager: ConnectivityManager
) : DeviceInfoRepository {

    override fun collectDeviceInfo(): DeviceInfo {
        val networkType = getNetworkType()
        val batteryLevel = getBatteryLevel()
        val ramAvailableMb = getAvailableRam()
        val storageFreeMb = getFreeStorage()
        val (resolution, density) = getScreenInfo()

        return DeviceInfo(
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            osVersion = Build.VERSION.RELEASE,
            sdkLevel = Build.VERSION.SDK_INT,
            buildNumber = Build.DISPLAY,
            networkType = networkType,
            batteryLevel = batteryLevel,
            ramAvailableMb = ramAvailableMb,
            storageFreeMb = storageFreeMb,
            screenResolution = resolution,
            screenDensity = density
        )
    }

    override fun collectAppInfo(packageName: String): AppInfo {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            val appName = pm.getApplicationLabel(info.applicationInfo).toString()
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            val isSystemApp = (info.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            AppInfo(
                packageName = packageName,
                appName = appName,
                versionName = info.versionName ?: "",
                versionCode = versionCode,
                isForeground = false, // determined contextually, not from PackageManager
                isSystemApp = isSystemApp
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.w(e, "Package not found: $packageName")
            AppInfo(
                packageName = packageName,
                appName = packageName,
                versionName = "",
                versionCode = 0,
                isForeground = false,
                isSystemApp = false
            )
        }
    }

    private fun getNetworkType(): String {
        val network = connectivityManager.activeNetwork ?: return "None"
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return "None"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "None"
        }
    }

    private fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
    }

    private fun getAvailableRam(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }

    private fun getFreeStorage(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBytes / (1024 * 1024)
        } catch (e: Exception) {
            Timber.w(e, "Failed to get free storage")
            0L
        }
    }

    private fun getScreenInfo(): Pair<String, Int> {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = wm.currentWindowMetrics.bounds
                val density = context.resources.displayMetrics.densityDpi
                "${bounds.width()}x${bounds.height()}" to density
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getMetrics(metrics)
                "${metrics.widthPixels}x${metrics.heightPixels}" to metrics.densityDpi
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get screen info")
            "unknown" to 0
        }
    }
}
