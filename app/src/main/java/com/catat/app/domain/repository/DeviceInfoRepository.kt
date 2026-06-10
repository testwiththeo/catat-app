package com.catat.app.domain.repository

import com.catat.app.domain.model.AppInfo
import com.catat.app.domain.model.DeviceInfo

interface DeviceInfoRepository {
    fun collectDeviceInfo(): DeviceInfo
    fun collectAppInfo(packageName: String): AppInfo
}
