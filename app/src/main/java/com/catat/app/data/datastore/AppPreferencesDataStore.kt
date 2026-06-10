package com.catat.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.catat.app.domain.model.AppPreferences
import com.catat.app.domain.model.ButtonPosition
import com.catat.app.domain.model.ExportFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "catat_preferences")

@Singleton
class AppPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object Keys {
        val DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")
        val SCREENSHOT_QUALITY    = intPreferencesKey("screenshot_quality")
        val FLOATING_BTN_POSITION = stringPreferencesKey("floating_button_position")
        val ATTACH_DEVICE_INFO    = booleanPreferencesKey("attach_device_info")
        val ATTACH_APP_INFO       = booleanPreferencesKey("attach_app_info")
        val ATTACH_NETWORK_INFO   = booleanPreferencesKey("attach_network_info")
        val ATTACH_BATTERY_INFO   = booleanPreferencesKey("attach_battery_info")
        val ATTACH_MEMORY_INFO    = booleanPreferencesKey("attach_memory_info")
        val VOICE_LANGUAGE        = stringPreferencesKey("voice_language")
        val ONBOARDING_COMPLETED  = booleanPreferencesKey("onboarding_completed")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            defaultExportFormat = ExportFormat.fromString(prefs[DEFAULT_EXPORT_FORMAT]),
            screenshotQuality   = prefs[SCREENSHOT_QUALITY] ?: 90,
            floatingButtonPosition = ButtonPosition.valueOf(
                prefs[FLOATING_BTN_POSITION] ?: ButtonPosition.RIGHT.name
            ),
            attachDeviceInfo  = prefs[ATTACH_DEVICE_INFO]  ?: true,
            attachAppInfo     = prefs[ATTACH_APP_INFO]     ?: true,
            attachNetworkInfo = prefs[ATTACH_NETWORK_INFO] ?: true,
            attachBatteryInfo = prefs[ATTACH_BATTERY_INFO] ?: true,
            attachMemoryInfo  = prefs[ATTACH_MEMORY_INFO]  ?: true,
            voiceLanguage     = prefs[VOICE_LANGUAGE]      ?: "en-US",
            onboardingCompleted = prefs[ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun updateDefaultExportFormat(format: ExportFormat) {
        context.dataStore.edit { it[DEFAULT_EXPORT_FORMAT] = format.name() }
    }

    suspend fun updateScreenshotQuality(quality: Int) {
        context.dataStore.edit { it[SCREENSHOT_QUALITY] = quality }
    }

    suspend fun updateFloatingButtonPosition(position: ButtonPosition) {
        context.dataStore.edit { it[FLOATING_BTN_POSITION] = position.name }
    }

    suspend fun updateAttachDeviceInfo(enabled: Boolean) {
        context.dataStore.edit { it[ATTACH_DEVICE_INFO] = enabled }
    }

    suspend fun updateAttachAppInfo(enabled: Boolean) {
        context.dataStore.edit { it[ATTACH_APP_INFO] = enabled }
    }

    suspend fun updateAttachNetworkInfo(enabled: Boolean) {
        context.dataStore.edit { it[ATTACH_NETWORK_INFO] = enabled }
    }

    suspend fun updateAttachBatteryInfo(enabled: Boolean) {
        context.dataStore.edit { it[ATTACH_BATTERY_INFO] = enabled }
    }

    suspend fun updateAttachMemoryInfo(enabled: Boolean) {
        context.dataStore.edit { it[ATTACH_MEMORY_INFO] = enabled }
    }

    suspend fun updateVoiceLanguage(language: String) {
        context.dataStore.edit { it[VOICE_LANGUAGE] = language }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }
}
