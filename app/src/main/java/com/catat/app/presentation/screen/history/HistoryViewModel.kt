package com.catat.app.presentation.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.ReportStatus
import com.catat.app.domain.usecase.GetReportsUseCase
import com.catat.app.domain.usecase.SaveReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getReportsUseCase: GetReportsUseCase,
    private val saveReportUseCase: SaveReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(
        HistoryUiState.Content()
    )
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val reports: Flow<PagingData<BugReport>> = _uiState
        .filterIsInstance<HistoryUiState.Content>()
        .map { it.searchQuery.trim() to it.filter }
        .distinctUntilChanged()
        .flatMapLatest { (query, filter) ->
            getReportsUseCase.execute(filter.status, query)
        }
        .cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) = updateContent {
        it.copy(searchQuery = query)
    }

    fun selectFilter(filter: HistoryFilter) = updateContent {
        it.copy(filter = filter)
    }

    fun archiveReport(report: BugReport) {
        viewModelScope.launch {
            saveReportUseCase.update(
                report.copy(
                    status = ReportStatus.ARCHIVED,
                    updatedAt = System.currentTimeMillis()
                )
            )
            updateContent { it.copy(pendingUndoReport = report) }
        }
    }

    fun undoArchive(report: BugReport) {
        viewModelScope.launch {
            saveReportUseCase.update(report.copy(updatedAt = System.currentTimeMillis()))
            updateContent { it.copy(pendingUndoReport = null) }
        }
    }

    fun clearPendingUndo() = updateContent {
        it.copy(pendingUndoReport = null)
    }

    private fun updateContent(transform: (HistoryUiState.Content) -> HistoryUiState.Content) {
        _uiState.update { current ->
            if (current is HistoryUiState.Content) transform(current) else current
        }
    }
}

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Content(
        val searchQuery: String = "",
        val filter: HistoryFilter = HistoryFilter.All,
        val pendingUndoReport: BugReport? = null
    ) : HistoryUiState
}

enum class HistoryFilter(val label: String, val status: ReportStatus?) {
    All("All", null),
    Draft("Draft", ReportStatus.DRAFT),
    Exported("Exported", ReportStatus.EXPORTED)
}
