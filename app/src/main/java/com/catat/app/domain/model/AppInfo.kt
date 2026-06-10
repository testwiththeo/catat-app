package com.catat.app.domain.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isForeground: Boolean,
    val isSystemApp: Boolean
)
