package com.catat.app.presentation.screen.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catat.app.data.storage.FileStorage
import com.catat.app.domain.export.ExportEngine
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.ExportFormat
import com.catat.app.domain.model.ReportStatus
import com.catat.app.domain.repository.ReportRepository
import com.catat.app.domain.usecase.ExportReportUseCase
import com.catat.app.domain.usecase.ExportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val exportReportUseCase: ExportReportUseCase,
    private val exportEngine: ExportEngine,
    private val fileStorage: FileStorage,
    private val clipboardManager: ClipboardManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Loading)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun load(reportId: Long) {
        viewModelScope.launch {
            _uiState.value = ExportUiState.Loading
            val report = reportRepository.getReportById(reportId)
            if (report == null) {
                _uiState.value = ExportUiState.NotFound
                return@launch
            }
            val format = report.exportFormat ?: ExportFormat.Jira
            _uiState.value = ExportUiState.Content(
                report = report,
                selectedFormat = format,
                preview = exportEngine.render(report, format),
                screenshotPath = report.annotatedScreenshotPath ?: report.screenshotPaths.firstOrNull()
            )
        }
    }

    fun selectFormat(format: ExportFormat) = updateContent { state ->
        state.copy(
            selectedFormat = format,
            preview = exportEngine.render(state.report, format),
            message = null
        )
    }

    fun copyToClipboard() {
        val state = _uiState.value as? ExportUiState.Content ?: return
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Catat bug report", state.preview))
        markExported("Copied to clipboard")
    }

    fun createShareIntent(): Intent? {
        val state = _uiState.value as? ExportUiState.Content ?: return null
        markExported("Ready to share")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, state.preview)
            putExtra(Intent.EXTRA_SUBJECT, state.report.title.ifBlank { "Bug report" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val screenshotUri = state.screenshotPath?.let { path ->
            runCatching { fileStorage.getFileUri(path) }.getOrNull()
        }
        if (screenshotUri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, screenshotUri)
            intent.clipData = ClipData.newUri(null, "screenshot", screenshotUri)
        }
        return intent
    }

    fun saveAsFile() {
        val state = _uiState.value as? ExportUiState.Content ?: return
        if (state.isWorking) return

        viewModelScope.launch {
            _uiState.value = state.copy(isWorking = true, message = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    fileStorage.saveText(
                        content = state.preview,
                        subDir = EXPORT_DIR,
                        prefix = "bug_report_${state.report.id}",
                        extension = exportEngine.fileExtension(state.selectedFormat)
                    )
                }
            }.onSuccess { path ->
                markExported("Saved to ${path.substringAfterLast('/')}")
                updateContent { it.copy(isWorking = false, savedFilePath = path) }
            }.onFailure { error ->
                _uiState.value = state.copy(
                    isWorking = false,
                    message = error.message ?: "Unable to save export file"
                )
            }
        }
    }

    private fun markExported(message: String) {
        val state = _uiState.value as? ExportUiState.Content ?: return
        viewModelScope.launch {
            when (val result = exportReportUseCase.execute(state.report, state.selectedFormat)) {
                is ExportResult.Success -> {
                    updateContent {
                        it.copy(
                            report = it.report.copy(
                                status = ReportStatus.EXPORTED,
                                exportFormat = it.selectedFormat,
                                updatedAt = System.currentTimeMillis()
                            ),
                            preview = result.content,
                            message = message,
                            screenshotPath = result.screenshotPath
                        )
                    }
                }
                is ExportResult.Failure -> {
                    updateContent {
                        it.copy(message = result.cause.message ?: "Unable to mark report exported")
                    }
                }
            }
        }
    }

    private fun updateContent(transform: (ExportUiState.Content) -> ExportUiState.Content) {
        _uiState.update { current ->
            if (current is ExportUiState.Content) transform(current) else current
        }
    }

    private companion object {
        private const val EXPORT_DIR = "exports"
    }
}

sealed interface ExportUiState {
    data object Loading : ExportUiState
    data object NotFound : ExportUiState
    data class Content(
        val report: BugReport,
        val selectedFormat: ExportFormat,
        val preview: String,
        val screenshotPath: String?,
        val isWorking: Boolean = false,
        val message: String? = null,
        val savedFilePath: String? = null
    ) : ExportUiState
}
