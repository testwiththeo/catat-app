package com.catat.app.presentation.screen.reportdetail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catat.app.data.storage.FileStorage
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.DeviceInfo
import com.catat.app.domain.model.ReportStatus
import com.catat.app.domain.repository.DeviceInfoRepository
import com.catat.app.domain.repository.ReportRepository
import com.catat.app.domain.usecase.SaveReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reportRepository: ReportRepository,
    private val saveReportUseCase: SaveReportUseCase,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val fileStorage: FileStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportDetailUiState>(ReportDetailUiState.Loading)
    val uiState: StateFlow<ReportDetailUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    fun load(reportId: Long) {
        viewModelScope.launch {
            _uiState.value = ReportDetailUiState.Loading
            val report = reportRepository.getReportById(reportId)
            if (report == null) {
                _uiState.value = ReportDetailUiState.NotFound
                return@launch
            }

            val deviceInfo = report.deviceInfo ?: deviceInfoRepository.collectDeviceInfo()
            val screenshotPath = report.annotatedScreenshotPath ?: report.screenshotPaths.firstOrNull()
            val screenshotBitmap = screenshotPath?.let { path ->
                withContext(Dispatchers.IO) { fileStorage.loadBitmap(path) }
            }

            _uiState.value = ReportDetailUiState.Content(
                report = report.copy(deviceInfo = deviceInfo),
                title = report.title,
                stepsToReproduce = report.stepsToReproduce,
                actualResult = report.actualResult,
                expectedResult = report.expectedResult,
                deviceContext = deviceInfo.toEditableText(),
                screenshotPath = screenshotPath,
                screenshotBitmap = screenshotBitmap
            )
        }
    }

    fun updateTitle(value: String) = updateContent { it.copy(title = value) }
    fun updateSteps(value: String) = updateContent { it.copy(stepsToReproduce = value) }
    fun updateActual(value: String) = updateContent { it.copy(actualResult = value) }
    fun updateExpected(value: String) = updateContent { it.copy(expectedResult = value) }
    fun updateDeviceContext(value: String) = updateContent { it.copy(deviceContext = value) }

    fun saveDraft(navigateToExport: Boolean = false) {
        val state = _uiState.value as? ReportDetailUiState.Content ?: return
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, message = null)
            runCatching {
                val report = state.toBugReport()
                if (report.id > 0L) {
                    saveReportUseCase.update(report)
                    report.id
                } else {
                    saveReportUseCase.save(report)
                }
            }.onSuccess { id ->
                _uiState.update { current ->
                    if (current is ReportDetailUiState.Content) {
                        current.copy(
                            report = current.report.copy(id = id),
                            isSaving = false,
                            message = "Draft saved",
                            exportReportId = if (navigateToExport) id else null
                        )
                    } else {
                        current
                    }
                }
            }.onFailure { error ->
                _uiState.value = state.copy(
                    isSaving = false,
                    message = error.message ?: "Unable to save report"
                )
            }
        }
    }

    fun startVoiceInput() {
        val state = _uiState.value as? ReportDetailUiState.Content ?: return
        if (state.isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _uiState.value = state.copy(message = "Speech recognition is not available")
            return
        }

        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            speechRecognizer = it
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                updateContent { it.copy(isListening = true, message = "Listening") }
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                updateContent { it.copy(isListening = false) }
            }

            override fun onError(error: Int) {
                updateContent { it.copy(isListening = false, message = "Voice input stopped") }
            }

            override fun onResults(results: Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                val spokenText = matches.firstOrNull().orEmpty()
                updateContent { current ->
                    val separator = if (current.stepsToReproduce.isBlank()) "" else "\n"
                    current.copy(
                        stepsToReproduce = current.stepsToReproduce + separator + spokenText,
                        isListening = false,
                        message = if (spokenText.isBlank()) "No speech captured" else null
                    )
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Steps to reproduce")
        }
        recognizer.startListening(intent)
    }

    fun stopVoiceInput() {
        speechRecognizer?.stopListening()
        updateContent { it.copy(isListening = false) }
    }

    fun consumeExportNavigation() = updateContent {
        it.copy(exportReportId = null)
    }

    override fun onCleared() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onCleared()
    }

    private fun ReportDetailUiState.Content.toBugReport(): BugReport {
        val fallbackDeviceInfo = report.deviceInfo ?: deviceInfoRepository.collectDeviceInfo()
        return report.copy(
            title = title.trim(),
            stepsToReproduce = stepsToReproduce.trim(),
            actualResult = actualResult.trim(),
            expectedResult = expectedResult.trim(),
            deviceInfo = deviceContext.toDeviceInfo(fallbackDeviceInfo),
            screenshotPaths = report.screenshotPaths,
            annotatedScreenshotPath = report.annotatedScreenshotPath,
            status = ReportStatus.DRAFT,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun updateContent(transform: (ReportDetailUiState.Content) -> ReportDetailUiState.Content) {
        _uiState.update { current ->
            if (current is ReportDetailUiState.Content) transform(current) else current
        }
    }
}

sealed interface ReportDetailUiState {
    data object Loading : ReportDetailUiState
    data object NotFound : ReportDetailUiState
    data class Content(
        val report: BugReport,
        val title: String,
        val stepsToReproduce: String,
        val actualResult: String,
        val expectedResult: String,
        val deviceContext: String,
        val screenshotPath: String?,
        val screenshotBitmap: Bitmap?,
        val isSaving: Boolean = false,
        val isListening: Boolean = false,
        val message: String? = null,
        val exportReportId: Long? = null
    ) : ReportDetailUiState
}

private fun DeviceInfo.toEditableText(): String {
    return listOf(
        "Model: $deviceModel",
        "Manufacturer: $manufacturer",
        "OS: $osVersion",
        "SDK: $sdkLevel",
        "Build: $buildNumber",
        "Network: $networkType",
        "Battery: $batteryLevel",
        "RAM MB: $ramAvailableMb",
        "Storage MB: $storageFreeMb",
        "Resolution: $screenResolution",
        "Density: $screenDensity"
    ).joinToString("\n")
}

private fun String.toDeviceInfo(fallback: DeviceInfo): DeviceInfo {
    val values = lineSequence()
        .mapNotNull { line ->
            val index = line.indexOf(':')
            if (index <= 0) null else line.substring(0, index).trim().lowercase() to
                line.substring(index + 1).trim()
        }
        .toMap()

    return fallback.copy(
        deviceModel = values["model"].orFallback(fallback.deviceModel),
        manufacturer = values["manufacturer"].orFallback(fallback.manufacturer),
        osVersion = values["os"].orFallback(fallback.osVersion),
        sdkLevel = values["sdk"].toIntOrFallback(fallback.sdkLevel),
        buildNumber = values["build"].orFallback(fallback.buildNumber),
        networkType = values["network"].orFallback(fallback.networkType),
        batteryLevel = values["battery"].toIntOrFallback(fallback.batteryLevel).coerceIn(0, 100),
        ramAvailableMb = values["ram mb"].toLongOrFallback(fallback.ramAvailableMb),
        storageFreeMb = values["storage mb"].toLongOrFallback(fallback.storageFreeMb),
        screenResolution = values["resolution"].orFallback(fallback.screenResolution),
        screenDensity = values["density"].toIntOrFallback(fallback.screenDensity)
    )
}

private fun String?.orFallback(fallback: String): String =
    if (isNullOrBlank()) fallback else this

private fun String?.toIntOrFallback(fallback: Int): Int =
    this?.toIntOrNull() ?: fallback

private fun String?.toLongOrFallback(fallback: Long): Long =
    this?.toLongOrNull() ?: fallback
