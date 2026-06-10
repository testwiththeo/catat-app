package com.catat.app.presentation.screen.annotation

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.catat.app.data.storage.FileStorage
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.paging.PagingData
import com.catat.app.domain.model.ReportStatus

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AnnotationEngineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val engine = AnnotationEngine()

    @Test
    fun `stack blur changes high contrast pixels`() {
        val bitmap = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFFFFFFFF.toInt())
        bitmap.setPixel(2, 2, 0xFF000000.toInt())

        val blurred = engine.stackBlur(bitmap, radius = 1)

        assertNotEquals(0xFF000000.toInt(), blurred.getPixel(2, 2))
        assertNotEquals(0xFFFFFFFF.toInt(), blurred.getPixel(2, 1))
    }

    @Test
    fun `viewmodel supports undo and redo stacks`() {
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val viewModel = AnnotationViewModel(
            fileStorage = FakeFileStorage(bitmap),
            reportRepository = FakeReportRepository(),
            annotationEngine = engine
        )
        viewModel.seedEditingState(bitmap)

        viewModel.startAction(Offset(1f, 1f))
        viewModel.updateAction(Offset(10f, 10f))
        viewModel.finishAction()

        val afterDraw = viewModel.uiState.value as AnnotationUiState.Editing
        assertEquals(1, afterDraw.actions.size)

        viewModel.undo()
        val afterUndo = viewModel.uiState.value as AnnotationUiState.Editing
        assertTrue(afterUndo.actions.isEmpty())
        assertEquals(1, afterUndo.redoActions.size)

        viewModel.redo()
        val afterRedo = viewModel.uiState.value as AnnotationUiState.Editing
        assertEquals(1, afterRedo.actions.size)
        assertTrue(afterRedo.redoActions.isEmpty())
    }

    @Suppress("UNCHECKED_CAST")
    private fun AnnotationViewModel.seedEditingState(bitmap: Bitmap) {
        val field = AnnotationViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val state = field.get(this) as MutableStateFlow<AnnotationUiState>
        state.value = AnnotationUiState.Editing(
            baseBitmap = bitmap,
            screenshotPath = "source.png"
        )
    }
}

private class FakeFileStorage(private val bitmap: Bitmap) : FileStorage {
    override fun saveBitmap(bitmap: Bitmap, subDir: String, prefix: String): String = "$subDir/$prefix.png"
    override fun saveText(content: String, subDir: String, prefix: String, extension: String): String =
        "$subDir/$prefix.$extension"
    override fun loadBitmap(path: String): Bitmap = bitmap
    override fun delete(path: String): Boolean = true
    override fun listFiles(subDir: String): List<String> = emptyList()
    override fun enforceMaxFiles(subDir: String, max: Int) = Unit
    override fun getFileUri(path: String): Uri = Uri.parse("content://catat/$path")
}

private class FakeReportRepository : ReportRepository {
    override fun getReportsPaged(): Flow<PagingData<BugReport>> = flowOf(PagingData.empty())
    override fun getReportsPaged(status: ReportStatus?, searchQuery: String): Flow<PagingData<BugReport>> =
        flowOf(PagingData.empty())
    override fun getReportsByStatus(status: ReportStatus): Flow<List<BugReport>> = flowOf(emptyList())
    override suspend fun getAllReports(): List<BugReport> = emptyList()
    override suspend fun getReportById(id: Long): BugReport? = null
    override suspend fun saveReport(report: BugReport): Long = 1L
    override suspend fun updateReport(report: BugReport) = Unit
    override suspend fun deleteReport(id: Long) = Unit
    override suspend fun clearAllReports() = Unit
    override fun searchReports(query: String): Flow<List<BugReport>> = flowOf(emptyList())
    override fun getReportCount(): Flow<Int> = flowOf(0)
}
