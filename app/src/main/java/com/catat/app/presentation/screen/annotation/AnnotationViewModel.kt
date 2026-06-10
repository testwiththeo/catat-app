package com.catat.app.presentation.screen.annotation

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catat.app.data.storage.FileStorage
import com.catat.app.domain.model.AnnotationAction
import com.catat.app.domain.model.AnnotationTool
import com.catat.app.domain.model.BlurIntensity
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.ReportStatus
import com.catat.app.domain.repository.ReportRepository
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
class AnnotationViewModel @Inject constructor(
    private val fileStorage: FileStorage,
    private val reportRepository: ReportRepository,
    private val annotationEngine: AnnotationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnnotationUiState>(AnnotationUiState.Loading)
    val uiState: StateFlow<AnnotationUiState> = _uiState.asStateFlow()

    fun load(screenshotPath: String, reportId: Long?) {
        val current = _uiState.value
        if (current is AnnotationUiState.Editing && current.screenshotPath == screenshotPath) return

        viewModelScope.launch {
            _uiState.value = AnnotationUiState.Loading
            val bitmap = withContext(Dispatchers.IO) {
                fileStorage.loadBitmap(screenshotPath)
            }
            _uiState.value = if (bitmap == null) {
                AnnotationUiState.Error("Unable to load screenshot")
            } else {
                AnnotationUiState.Editing(
                    baseBitmap = bitmap,
                    screenshotPath = screenshotPath,
                    reportId = reportId
                )
            }
        }
    }

    fun selectTool(tool: AnnotationTool) = updateEditing {
        it.copy(selectedTool = tool, inFlightAction = null)
    }

    fun selectColor(color: Color) = updateEditing { it.copy(selectedColor = color) }

    fun setStrokeWidth(width: Float) = updateEditing {
        it.copy(strokeWidth = width.coerceIn(MIN_STROKE_WIDTH, MAX_STROKE_WIDTH))
    }

    fun setBlurIntensity(intensity: BlurIntensity) = updateEditing {
        it.copy(blurIntensity = intensity)
    }

    fun undo() = updateEditing { state ->
        if (state.actions.isEmpty()) state else state.copy(
            actions = state.actions.dropLast(1),
            redoActions = (state.redoActions + state.actions.last()).takeLast(MAX_ACTIONS),
            inFlightAction = null
        )
    }

    fun redo() = updateEditing { state ->
        if (state.redoActions.isEmpty()) state else {
            val action = state.redoActions.last()
            state.copy(
                actions = (state.actions + action).takeLast(MAX_ACTIONS),
                redoActions = state.redoActions.dropLast(1),
                inFlightAction = null
            )
        }
    }

    fun startAction(point: Offset) = updateEditing { state ->
        if (state.selectedTool == AnnotationTool.Text) {
            state.copy(pendingTextPosition = point)
        } else {
            state.copy(
                inFlightAction = AnnotationAction(
                    tool = state.selectedTool,
                    path = listOf(point),
                    color = state.selectedColor,
                    strokeWidth = state.strokeWidth,
                    blurIntensity = state.blurIntensity
                )
            )
        }
    }

    fun updateAction(point: Offset) = updateEditing { state ->
        val action = state.inFlightAction ?: return@updateEditing state
        val path = when (action.tool) {
            AnnotationTool.Pen -> action.path + point
            else -> listOf(action.path.first(), point)
        }
        state.copy(inFlightAction = action.copy(path = path))
    }

    fun finishAction() = updateEditing { state ->
        val action = state.inFlightAction ?: return@updateEditing state
        if (action.path.size < 2) {
            state.copy(inFlightAction = null)
        } else {
            state.copy(
                actions = (state.actions + action).takeLast(MAX_ACTIONS),
                redoActions = emptyList(),
                inFlightAction = null
            )
        }
    }

    fun addText(text: String) = updateEditing { state ->
        val position = state.pendingTextPosition ?: return@updateEditing state
        if (text.isBlank()) {
            state.copy(pendingTextPosition = null)
        } else {
            state.copy(
                actions = (state.actions + AnnotationAction(
                    tool = AnnotationTool.Text,
                    path = listOf(position),
                    color = state.selectedColor,
                    strokeWidth = state.strokeWidth,
                    text = text.trim()
                )).takeLast(MAX_ACTIONS),
                redoActions = emptyList(),
                pendingTextPosition = null
            )
        }
    }

    fun dismissTextInput() = updateEditing {
        it.copy(pendingTextPosition = null)
    }

    fun saveAnnotatedBitmap() {
        val state = _uiState.value as? AnnotationUiState.Editing ?: return
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            runCatching {
                val rendered = withContext(Dispatchers.Default) {
                    annotationEngine.render(state.baseBitmap, state.actions)
                }
                val annotatedPath = try {
                    withContext(Dispatchers.IO) {
                        fileStorage.saveBitmap(rendered, SCREENSHOT_DIR, ANNOTATED_PREFIX)
                    }
                } finally {
                    rendered.recycle()
                }
                saveReportWithAnnotation(state, annotatedPath)
            }.onSuccess { reportId ->
                _uiState.value = AnnotationUiState.Saved(reportId)
            }.onFailure { error ->
                _uiState.value = state.copy(
                    isSaving = false,
                    errorMessage = error.message ?: "Unable to save annotation"
                )
            }
        }
    }

    private suspend fun saveReportWithAnnotation(
        state: AnnotationUiState.Editing,
        annotatedPath: String
    ): Long {
        val existing = state.reportId?.let { reportRepository.getReportById(it) }
        return if (existing != null) {
            val screenshots = if (existing.screenshotPaths.isEmpty()) {
                listOf(state.screenshotPath)
            } else {
                existing.screenshotPaths
            }
            reportRepository.updateReport(
                existing.copy(
                    screenshotPaths = screenshots,
                    annotatedScreenshotPath = annotatedPath,
                    updatedAt = System.currentTimeMillis()
                )
            )
            existing.id
        } else {
            reportRepository.saveReport(
                BugReport(
                    screenshotPaths = listOf(state.screenshotPath),
                    annotatedScreenshotPath = annotatedPath,
                    status = ReportStatus.DRAFT
                )
            )
        }
    }

    private fun updateEditing(transform: (AnnotationUiState.Editing) -> AnnotationUiState.Editing) {
        _uiState.update { current ->
            if (current is AnnotationUiState.Editing) transform(current) else current
        }
    }

    private companion object {
        private const val MAX_ACTIONS = 50
        private const val MIN_STROKE_WIDTH = 2f
        private const val MAX_STROKE_WIDTH = 24f
        private const val SCREENSHOT_DIR = "screenshots"
        private const val ANNOTATED_PREFIX = "annotated"
    }
}

sealed interface AnnotationUiState {
    data object Loading : AnnotationUiState
    data class Error(val message: String) : AnnotationUiState
    data class Saved(val reportId: Long) : AnnotationUiState
    data class Editing(
        val baseBitmap: Bitmap,
        val screenshotPath: String,
        val reportId: Long? = null,
        val actions: List<AnnotationAction> = emptyList(),
        val redoActions: List<AnnotationAction> = emptyList(),
        val inFlightAction: AnnotationAction? = null,
        val pendingTextPosition: Offset? = null,
        val selectedTool: AnnotationTool = AnnotationTool.Arrow,
        val selectedColor: Color = Color.Red,
        val strokeWidth: Float = 5f,
        val blurIntensity: BlurIntensity = BlurIntensity.Medium,
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    ) : AnnotationUiState
}
