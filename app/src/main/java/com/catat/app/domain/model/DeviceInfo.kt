package com.catat.app.domain.model

data class DeviceInfo(
    val deviceModel: String,
    val manufacturer: String,
    val osVersion: String,
    val sdkLevel: Int,
    val buildNumber: String,
    val networkType: String,   // "WiFi" | "Mobile" | "Ethernet" | "None"
    val batteryLevel: Int,     // 0–100
    val ramAvailableMb: Long,
    val storageFreeMb: Long,
    val screenResolution: String,
    val screenDensity: Int
)
