package com.catat.app.presentation.screen.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catat.app.domain.repository.CaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val captureRepository: CaptureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    init {
        loadLastCapture()
    }

    fun acceptCapturePath(path: String?) {
        if (path.isNullOrBlank()) return
        _uiState.update {
            it.copy(
                screenshotPath = path,
                isLoading = false,
                isWaitingForCapture = false
            )
        }
    }

    fun markRetakeRequested() {
        _uiState.update {
            it.copy(isWaitingForCapture = true, isLoading = false)
        }
    }

    fun loadLastCapture() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val path = captureRepository.getLastCapturePath()
            _uiState.update {
                it.copy(
                    screenshotPath = path,
                    isLoading = false,
                    isWaitingForCapture = path == null
                )
            }
        }
    }
}

data class CaptureUiState(
    val screenshotPath: String? = null,
    val isLoading: Boolean = false,
    val isWaitingForCapture: Boolean = false
)
