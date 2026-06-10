package com.catat.app.presentation.screen.export

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.catat.app.domain.model.ExportFormat
import com.catat.app.presentation.theme.CatatColors
import com.catat.app.presentation.theme.CatatShapes
import com.catat.app.presentation.theme.CatatShadows
import com.catat.app.presentation.theme.CatatTextStyles
import com.catat.app.presentation.theme.cardColor
import com.catat.app.presentation.theme.iosShadow
import com.catat.app.presentation.theme.previewBackground
import com.catat.app.presentation.theme.secondaryBackground
import com.catat.app.presentation.theme.shimmer

@Composable
fun ExportScreen(
    reportId: Long,
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(reportId) {
        viewModel.load(reportId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val state = uiState) {
                ExportUiState.Loading -> ExportLoading()
                ExportUiState.NotFound -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Report not found", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is ExportUiState.Content -> {
                    LaunchedEffect(state.message) {
                        if (!state.message.isNullOrBlank()) {
                            snackbarHostState.showSnackbar(state.message)
                        }
                    }
                    ExportContent(
                        state = state,
                        onBack = onBack,
                        onFormatSelected = viewModel::selectFormat,
                        onCopy = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.copyToClipboard()
                        },
                        onShare = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.createShareIntent()?.let { shareIntent ->
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share report")
                                )
                            }
                        },
                        onSaveFile = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.saveAsFile()
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) { data ->
            ExportSnackbar(data = data)
        }
    }
}

@Composable
private fun ExportContent(
    state: ExportUiState.Content,
    onBack: () -> Unit,
    onFormatSelected: (ExportFormat) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSaveFile: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ExportTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormatSelector(
                selectedFormat = state.selectedFormat,
                onFormatSelected = onFormatSelected
            )
            PreviewCard(
                preview = state.preview,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.40f)
            )
            Spacer(Modifier.weight(1f))
        }

        ExportActions(
            isWorking = state.isWorking,
            onCopy = onCopy,
            onShare = onShare,
            onSaveFile = onSaveFile
        )
    }
}

@Composable
private fun ExportTopBar(onBack: () -> Unit) {
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
            text = "Export",
            style = CatatTextStyles.Title3
        )
    }
}

@Composable
private fun FormatSelector(
    selectedFormat: ExportFormat,
    onFormatSelected: (ExportFormat) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        exportFormats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { format ->
                    FormatPill(
                        format = format,
                        selected = format == selectedFormat,
                        onClick = { onFormatSelected(format) },
                        modifier = Modifier
                            .width(80.dp)
                            .height(64.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatPill(
    format: ExportFormat,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(CatatShapes.md)
            .clickable(onClick = onClick),
        shape = CatatShapes.md,
        color = if (selected) CatatColors.Accent else MaterialTheme.colorScheme.cardColor(),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, CatatColors.Accent)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FormatGlyph(format = format, selected = selected)
            Spacer(Modifier.height(6.dp))
            Text(
                text = format.label(),
                style = CatatTextStyles.Footnote,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FormatGlyph(format: ExportFormat, selected: Boolean) {
    val color = when (format) {
        ExportFormat.Jira -> Color(0xFF0052CC)
        ExportFormat.GitHub -> Color(0xFF111111)
        ExportFormat.Linear -> Color(0xFF2C2C2E)
        ExportFormat.Markdown -> Color(0xFFFF9500)
    }
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White.copy(alpha = 0.20f) else color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = format.shortLabel(),
            color = if (selected) Color.White else color,
            style = CatatTextStyles.Caption1,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PreviewCard(preview: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .heightIn(min = 220.dp)
            .iosShadow(CatatShadows.shadowSm, cornerRadius = 12.dp)
            .clip(CatatShapes.md),
        shape = CatatShapes.md,
        color = MaterialTheme.colorScheme.previewBackground()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PREVIEW",
                style = CatatTextStyles.Footnote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PreviewText(preview = preview)
            }
        }
    }
}

@Composable
private fun PreviewText(preview: String) {
    var inCode = false
    preview.lines().forEach { line ->
        val trimmed = line.trim()
        val isFence = trimmed == "```" || trimmed == "{noformat}"
        if (isFence) inCode = !inCode
        val isHeading = trimmed.startsWith("#") ||
            trimmed.startsWith("h2.") ||
            (trimmed.startsWith("*") && trimmed.endsWith("*") && trimmed.length > 2)

        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = if (isHeading) 16.sp else 15.sp,
                fontFamily = if (inCode || isFence) FontFamily.Monospace else FontFamily.Default
            ),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ExportActions(
    isWorking: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSaveFile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExportActionButton(
            label = "Copy",
            icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
            enabled = !isWorking,
            onClick = onCopy,
            style = ActionStyle.OutlinedAccent,
            modifier = Modifier.weight(1f)
        )
        ExportActionButton(
            label = "Share",
            icon = { Icon(Icons.Default.Share, contentDescription = null) },
            enabled = !isWorking,
            onClick = onShare,
            style = ActionStyle.FilledAccent,
            modifier = Modifier.weight(1f)
        )
        ExportActionButton(
            label = "Save",
            icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
            enabled = !isWorking,
            onClick = onSaveFile,
            style = ActionStyle.OutlinedNeutral,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ExportActionButton(
    label: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
    style: ActionStyle,
    modifier: Modifier = Modifier
) {
    val container = when (style) {
        ActionStyle.FilledAccent -> CatatColors.Accent
        else -> Color.Transparent
    }
    val contentColor = when (style) {
        ActionStyle.FilledAccent -> Color.White
        ActionStyle.OutlinedAccent -> CatatColors.Accent
        ActionStyle.OutlinedNeutral -> MaterialTheme.colorScheme.onSurface
    }
    val border = when (style) {
        ActionStyle.FilledAccent -> null
        ActionStyle.OutlinedAccent -> BorderStroke(1.dp, CatatColors.Accent)
        ActionStyle.OutlinedNeutral -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Surface(
        modifier = modifier
            .height(50.dp)
            .clip(CatatShapes.md)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CatatShapes.md,
        color = if (enabled) container else MaterialTheme.colorScheme.secondaryBackground(),
        contentColor = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (enabled) border else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                icon()
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = CatatTextStyles.Callout,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ExportSnackbar(data: SnackbarData) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .iosShadow(CatatShadows.shadowLg, cornerRadius = 16.dp)
            .clip(CatatShapes.lg),
        color = Color(0xEE1C1C1E),
        contentColor = Color.White,
        shape = CatatShapes.lg,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CatatColors.Success)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = data.visuals.message,
                style = CatatTextStyles.Footnote
            )
        }
    }
}

@Composable
private fun ExportLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shimmer(RoundedCornerShape(999.dp))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shimmer(RoundedCornerShape(999.dp))
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .shimmer(RoundedCornerShape(12.dp))
        )
    }
}

private enum class ActionStyle {
    OutlinedAccent,
    FilledAccent,
    OutlinedNeutral
}

private fun ExportFormat.label(): String = when (this) {
    ExportFormat.Jira -> "Jira"
    ExportFormat.GitHub -> "GitHub"
    ExportFormat.Linear -> "Linear"
    ExportFormat.Markdown -> "Markdown"
}

private fun ExportFormat.shortLabel(): String = when (this) {
    ExportFormat.Jira -> "J"
    ExportFormat.GitHub -> "G"
    ExportFormat.Linear -> "L"
    ExportFormat.Markdown -> "M"
}

private val exportFormats = listOf(
    ExportFormat.Jira,
    ExportFormat.GitHub,
    ExportFormat.Linear,
    ExportFormat.Markdown
)
