package com.catat.app.domain.model

data class AppPreferences(
    val defaultExportFormat: ExportFormat = ExportFormat.Jira,
    val screenshotQuality: Int = 90,
    val floatingButtonPosition: ButtonPosition = ButtonPosition.RIGHT,
    val attachDeviceInfo: Boolean = true,
    val attachAppInfo: Boolean = true,
    val attachNetworkInfo: Boolean = true,
    val attachBatteryInfo: Boolean = true,
    val attachMemoryInfo: Boolean = true,
    val voiceLanguage: String = "en-US",
    val onboardingCompleted: Boolean = false
)

enum class ButtonPosition { LEFT, RIGHT }
