package com.catat.app.presentation.screen.reportdetail

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.catat.app.domain.model.DeviceInfo
import com.catat.app.presentation.theme.CatatColors
import com.catat.app.presentation.theme.CatatShapes
import com.catat.app.presentation.theme.CatatShadows
import com.catat.app.presentation.theme.CatatTextStyles
import com.catat.app.presentation.theme.cardColor
import com.catat.app.presentation.theme.iosShadow
import com.catat.app.presentation.theme.separatorColor
import com.catat.app.presentation.theme.secondaryBackground
import com.catat.app.presentation.theme.shimmer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun ReportDetailScreen(
    reportId: Long,
    onExport: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ReportDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startVoiceInput()
    }

    LaunchedEffect(reportId) {
        viewModel.load(reportId)
    }

    LaunchedEffect(uiState) {
        val exportId = (uiState as? ReportDetailUiState.Content)?.exportReportId
        if (exportId != null) {
            viewModel.consumeExportNavigation()
            onExport(exportId)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            ReportDetailUiState.Loading -> ReportDetailLoading()
            ReportDetailUiState.NotFound -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Report not found", style = MaterialTheme.typography.bodyLarge)
                }
            }
            is ReportDetailUiState.Content -> {
                ReportDetailContent(
                    state = state,
                    onBack = onBack,
                    onTitleChanged = viewModel::updateTitle,
                    onStepsChanged = viewModel::updateSteps,
                    onActualChanged = viewModel::updateActual,
                    onExpectedChanged = viewModel::updateExpected,
                    onDeviceContextChanged = viewModel::updateDeviceContext,
                    onVoiceInput = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            if (state.isListening) viewModel.stopVoiceInput() else viewModel.startVoiceInput()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onSaveDraft = { viewModel.saveDraft(navigateToExport = false) },
                    onExport = { viewModel.saveDraft(navigateToExport = true) }
                )
            }
        }
    }
}

@Composable
private fun ReportDetailContent(
    state: ReportDetailUiState.Content,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onStepsChanged: (String) -> Unit,
    onActualChanged: (String) -> Unit,
    onExpectedChanged: (String) -> Unit,
    onDeviceContextChanged: (String) -> Unit,
    onVoiceInput: () -> Unit,
    onSaveDraft: () -> Unit,
    onExport: () -> Unit
) {
    var showFullscreenScreenshot by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ReportDetailTopBar(
                onBack = onBack,
                onExport = onExport,
                exportEnabled = !state.isSaving
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScreenshotCard(
                    bitmap = state.screenshotBitmap,
                    onClick = { if (state.screenshotBitmap != null) showFullscreenScreenshot = true }
                )

                FormCard {
                    AppleTextField(
                        label = "Title",
                        value = state.title,
                        onValueChange = onTitleChanged,
                        singleLine = true,
                        minHeight = 40.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    AppleTextField(
                        label = "Steps to Reproduce",
                        value = state.stepsToReproduce,
                        onValueChange = onStepsChanged,
                        minHeight = 112.dp,
                        trailing = {
                            IconButton(onClick = onVoiceInput) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = if (state.isListening) "Stop voice input" else "Voice input",
                                    tint = if (state.isListening) CatatColors.Accent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppleTextField(
                            label = "Actual Result",
                            value = state.actualResult,
                            onValueChange = onActualChanged,
                            minHeight = 118.dp,
                            modifier = Modifier.weight(1f)
                        )
                        AppleTextField(
                            label = "Expected Result",
                            value = state.expectedResult,
                            onValueChange = onExpectedChanged,
                            minHeight = 118.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ContextSection(
                    deviceInfo = state.report.deviceInfo,
                    deviceContext = state.deviceContext,
                    onDeviceContextChanged = onDeviceContextChanged
                )

                AnimatedVisibility(visible = state.message != null) {
                    SavedIndicator(message = state.message.orEmpty())
                }

                Spacer(Modifier.height(84.dp))
            }
        }

        SaveDraftButton(
            isSaving = state.isSaving,
            onClick = onSaveDraft,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showFullscreenScreenshot && state.screenshotBitmap != null) {
        FullscreenScreenshotDialog(
            bitmap = state.screenshotBitmap,
            onDismiss = { showFullscreenScreenshot = false }
        )
    }
}

@Composable
private fun ReportDetailTopBar(
    onBack: () -> Unit,
    onExport: () -> Unit,
    exportEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = "Report Details",
            style = CatatTextStyles.Title3,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onExport, enabled = exportEnabled) {
            Text(
                text = "Export",
                color = if (exportEnabled) CatatColors.Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                style = CatatTextStyles.Body,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ScreenshotCard(bitmap: Bitmap?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 8.dp)
            .iosShadow(CatatShadows.shadowSm, cornerRadius = 12.dp)
            .clip(CatatShapes.md)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.cardColor(),
        shape = CatatShapes.md
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Screenshot thumbnail",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        } else {
            ScreenshotPlaceholder()
        }
    }
}

@Composable
private fun ScreenshotPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryBackground()),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            drawRoundRect(
                color = CatatColors.Accent.copy(alpha = 0.16f),
                topLeft = Offset(size.width * 0.22f, size.height * 0.12f),
                size = Size(size.width * 0.56f, size.height * 0.76f),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
            )
            drawLine(
                color = CatatColors.Accent,
                start = Offset(size.width * 0.36f, size.height * 0.62f),
                end = Offset(size.width * 0.66f, size.height * 0.38f),
                strokeWidth = 4.dp.toPx()
            )
        }
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .iosShadow(CatatShadows.shadowSm, cornerRadius = 12.dp)
            .clip(CatatShapes.md)
            .background(MaterialTheme.colorScheme.cardColor())
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun AppleTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minHeight: androidx.compose.ui.unit.Dp = 56.dp,
    trailing: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val focusLineFraction by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "fieldFocusLine"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = CatatTextStyles.Footnote,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(top = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
                singleLine = singleLine,
                textStyle = CatatTextStyles.Body.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(CatatColors.Accent)
            )
            trailing?.invoke()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.separatorColor())
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(focusLineFraction)
                    .height(1.5.dp)
                    .background(CatatColors.Accent)
            )
        }
    }
}

@Composable
private fun ContextSection(
    deviceInfo: DeviceInfo?,
    deviceContext: String,
    onDeviceContextChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(CatatShapes.md)
            .background(MaterialTheme.colorScheme.secondaryBackground())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "AUTO-ATTACHED CONTEXT",
                style = CatatTextStyles.Footnote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {}) {
                Text("Edit", color = CatatColors.Accent, style = CatatTextStyles.Footnote)
            }
        }
        ContextGrid(deviceInfo = deviceInfo)
        AppleTextField(
            label = "Editable Context",
            value = deviceContext,
            onValueChange = onDeviceContextChanged,
            minHeight = 120.dp
        )
    }
}

@Composable
private fun ContextGrid(deviceInfo: DeviceInfo?) {
    val rows = listOf(
        "Device" to (deviceInfo?.deviceModel ?: "Unknown"),
        "OS" to (deviceInfo?.osVersion ?: "Unknown"),
        "Network" to (deviceInfo?.networkType ?: "Unknown"),
        "Battery" to (deviceInfo?.batteryLevel?.let { "$it%" } ?: "Unknown"),
        "RAM" to (deviceInfo?.ramAvailableMb?.let { "$it MB" } ?: "Unknown"),
        "Storage" to (deviceInfo?.storageFreeMb?.let { "$it MB" } ?: "Unknown")
    ).chunked(2)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (label, value) ->
                    ContextCell(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ContextCell(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CatatShapes.sm)
            .background(MaterialTheme.colorScheme.cardColor())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label | $value",
            style = CatatTextStyles.Footnote,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SavedIndicator(message: String) {
    var visible by remember(message) { mutableStateOf(true) }
    val timestamp = remember(message) {
        SimpleDateFormat("h:mma", Locale.getDefault()).format(Date())
    }
    LaunchedEffect(message) {
        visible = true
        delay(3000)
        visible = false
    }
    AnimatedVisibility(visible = visible) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clip(CatatShapes.full)
                .background(MaterialTheme.colorScheme.secondaryBackground())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (message.contains("saved", ignoreCase = true)) {
                    "\u2713 Saved $timestamp"
                } else {
                    message
                },
                style = CatatTextStyles.Footnote,
                color = CatatColors.Success
            )
        }
    }
}

@Composable
private fun SaveDraftButton(
    isSaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(52.dp)
            .iosShadow(CatatShadows.shadowMd, cornerRadius = 999.dp)
            .clip(CatatShapes.full)
            .clickable(enabled = !isSaving, onClick = onClick),
        shape = CatatShapes.full,
        color = CatatColors.Accent,
        contentColor = Color.White,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Save as Draft",
                    style = CatatTextStyles.Body,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FullscreenScreenshotDialog(bitmap: Bitmap, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 6f)
        scale = nextScale
        offset += panChange
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Fullscreen screenshot",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(transformState)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ReportDetailLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .shimmer(RoundedCornerShape(12.dp))
        )
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .shimmer(RoundedCornerShape(12.dp))
            )
        }
    }
}
