package com.catat.app.presentation.screen.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import com.catat.app.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catat.app.data.datastore.AppPreferencesDataStore
import com.catat.app.data.storage.FileStorage
import com.catat.app.domain.model.AppPreferences
import com.catat.app.domain.model.ButtonPosition
import com.catat.app.domain.model.ExportFormat
import com.catat.app.domain.model.Template
import com.catat.app.domain.repository.ReportRepository
import com.catat.app.domain.repository.TemplateRepository
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val templateRepository: TemplateRepository,
    private val reportRepository: ReportRepository,
    private val fileStorage: FileStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesDataStore.preferences,
                templateRepository.getTemplatesByCategory(STEPS_TEMPLATE_CATEGORY)
            ) { preferences, templates ->
                SettingsUiState.Content(
                    preferences = preferences,
                    templates = templates,
                    appVersion = BuildConfig.VERSION_NAME
                )
            }.collect { next ->
                val current = _uiState.value as? SettingsUiState.Content
                _uiState.value = next.copy(
                    isWorking = current?.isWorking ?: false,
                    message = current?.message,
                    backupPath = current?.backupPath
                )
            }
        }
    }

    fun updateDefaultExportFormat(format: ExportFormat) = launchPreferenceUpdate {
        preferencesDataStore.updateDefaultExportFormat(format)
    }

    fun updateScreenshotQuality(quality: ScreenshotQuality) = launchPreferenceUpdate {
        preferencesDataStore.updateScreenshotQuality(quality.value)
    }

    fun updateFloatingButtonPosition(position: ButtonPosition) = launchPreferenceUpdate {
        preferencesDataStore.updateFloatingButtonPosition(position)
    }

    fun updateAttachDeviceInfo(enabled: Boolean) = launchPreferenceUpdate {
        preferencesDataStore.updateAttachDeviceInfo(enabled)
    }

    fun updateAttachAppInfo(enabled: Boolean) = launchPreferenceUpdate {
        preferencesDataStore.updateAttachAppInfo(enabled)
    }

    fun updateAttachNetworkInfo(enabled: Boolean) = launchPreferenceUpdate {
        preferencesDataStore.updateAttachNetworkInfo(enabled)
    }

    fun updateAttachBatteryInfo(enabled: Boolean) = launchPreferenceUpdate {
        preferencesDataStore.updateAttachBatteryInfo(enabled)
    }

    fun updateAttachMemoryInfo(enabled: Boolean) = launchPreferenceUpdate {
        preferencesDataStore.updateAttachMemoryInfo(enabled)
    }

    fun updateVoiceLanguage(language: String) = launchPreferenceUpdate {
        preferencesDataStore.updateVoiceLanguage(language)
    }

    fun saveTemplate(templateId: Long?, name: String, content: String) {
        if (name.isBlank() || content.isBlank()) {
            setMessage("Template name and content are required")
            return
        }
        viewModelScope.launch {
            runCatching {
                templateRepository.saveTemplate(
                    Template(
                        id = templateId ?: 0L,
                        name = name.trim(),
                        content = content.trim(),
                        category = STEPS_TEMPLATE_CATEGORY,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }.onSuccess {
                setMessage("Template saved")
            }.onFailure {
                setMessage(it.message ?: "Unable to save template")
            }
        }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch {
            runCatching {
                templateRepository.deleteTemplate(template.id)
            }.onSuccess {
                setMessage("Template deleted")
            }.onFailure {
                setMessage(it.message ?: "Unable to delete template")
            }
        }
    }

    fun clearAllReports() {
        viewModelScope.launch {
            setWorking(true)
            runCatching {
                withContext(Dispatchers.IO) { reportRepository.clearAllReports() }
            }.onSuccess {
                setWorking(false, "All reports cleared")
            }.onFailure {
                setWorking(false, it.message ?: "Unable to clear reports")
            }
        }
    }

    fun exportAllData() {
        viewModelScope.launch {
            setWorking(true)
            runCatching {
                withContext(Dispatchers.IO) { createBackupZip() }
            }.onSuccess { path ->
                updateContent {
                    it.copy(
                        isWorking = false,
                        message = "Backup saved to ${path.substringAfterLast('/')}",
                        backupPath = path
                    )
                }
            }.onFailure { error ->
                setWorking(false, error.message ?: "Unable to export data")
            }
        }
    }

    fun createBackupShareIntent(): Intent? {
        val state = _uiState.value as? SettingsUiState.Content ?: return null
        val path = state.backupPath ?: return null
        val uri = runCatching { fileStorage.getFileUri(path) }.getOrNull() ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, "catat_backup", uri)
        }
    }

    private suspend fun createBackupZip(): String {
        val reports = reportRepository.getAllReports()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val exportDir = File(context.filesDir, EXPORT_DIR).also { it.mkdirs() }
        val backup = File(exportDir, "catat_backup_$timestamp.zip")
        val gson = GsonBuilder().setPrettyPrinting().create()

        ZipOutputStream(FileOutputStream(backup)).use { zip ->
            zip.putNextEntry(ZipEntry("reports.json"))
            zip.write(gson.toJson(reports).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            reports.forEach { report ->
                val paths = (report.screenshotPaths + listOfNotNull(report.annotatedScreenshotPath)).distinct()
                paths.forEach { path ->
                    val file = File(path)
                    if (file.exists() && file.isFile) {
                        zip.putNextEntry(ZipEntry("screenshots/report_${report.id}/${file.name}"))
                        file.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
        return backup.absolutePath
    }

    private fun launchPreferenceUpdate(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { setMessage(it.message ?: "Unable to save setting") }
        }
    }

    private fun setWorking(isWorking: Boolean, message: String? = null) {
        updateContent { it.copy(isWorking = isWorking, message = message) }
    }

    private fun setMessage(message: String?) {
        updateContent { it.copy(message = message) }
    }

    private fun updateContent(transform: (SettingsUiState.Content) -> SettingsUiState.Content) {
        _uiState.update { current ->
            if (current is SettingsUiState.Content) transform(current) else current
        }
    }

    private companion object {
        private const val STEPS_TEMPLATE_CATEGORY = "steps"
        private const val EXPORT_DIR = "exports"
    }
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Content(
        val preferences: AppPreferences,
        val templates: List<Template>,
        val appVersion: String,
        val isWorking: Boolean = false,
        val message: String? = null,
        val backupPath: String? = null
    ) : SettingsUiState
}

enum class ScreenshotQuality(val label: String, val value: Int) {
    High("High", 100),
    Medium("Medium", 80),
    Low("Low", 60);

    companion object {
        fun fromValue(value: Int): ScreenshotQuality = entries.minBy {
            kotlin.math.abs(it.value - value)
        }
    }
}
