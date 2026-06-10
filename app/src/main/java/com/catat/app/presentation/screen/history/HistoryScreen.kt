package com.catat.app.presentation.screen.history

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.ReportStatus
import com.catat.app.presentation.theme.CatatAnimation
import com.catat.app.presentation.theme.CatatColors
import com.catat.app.presentation.theme.CatatShapes
import com.catat.app.presentation.theme.CatatShadows
import com.catat.app.presentation.theme.CatatTextStyles
import com.catat.app.presentation.theme.cardColor
import com.catat.app.presentation.theme.iosShadow
import com.catat.app.presentation.theme.pressScale
import com.catat.app.presentation.theme.rememberPressInteractionSource
import com.catat.app.presentation.theme.secondaryBackground
import com.catat.app.presentation.theme.separatorColor
import com.catat.app.presentation.theme.tertiaryText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    onNewCapture: () -> Unit,
    onOpenReport: (Long) -> Unit,
    onSettings: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reports = viewModel.reports.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val fabInteractionSource = rememberPressInteractionSource()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                HistorySnackbar(data = data)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewCapture,
                modifier = Modifier
                    .size(56.dp)
                    .iosShadow(CatatShadows.shadowLg, cornerRadius = 999.dp)
                    .pressScale(fabInteractionSource, pressedScale = 0.95f),
                interactionSource = fabInteractionSource,
                shape = CircleShape,
                containerColor = CatatColors.Accent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New capture", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        val content = uiState as? HistoryUiState.Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            HistoryHeader(onSettings = onSettings)

            if (content != null) {
                SearchPill(
                    query = content.searchQuery,
                    onQueryChanged = viewModel::updateSearchQuery
                )
                FilterChips(
                    selectedFilter = content.filter,
                    onFilterSelected = viewModel::selectFilter
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    reports.loadState.refresh is LoadState.Loading -> {
                        HistoryLoading(modifier = Modifier.fillMaxSize())
                    }
                    reports.itemCount == 0 -> {
                        EmptyHistory(
                            modifier = Modifier.align(Alignment.Center),
                            onNewCapture = onNewCapture
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                count = reports.itemCount,
                                key = { index -> reports[index]?.id ?: index }
                            ) { index ->
                                val report = reports[index] ?: return@items
                                StaggeredReportItem(index = index) {
                                    DismissibleReportRow(
                                        report = report,
                                        onOpenReport = onOpenReport,
                                        onDelete = {
                                            viewModel.archiveReport(report)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Report deleted",
                                                    actionLabel = "Undo",
                                                    withDismissAction = true
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.undoArchive(report)
                                                } else {
                                                    viewModel.clearPendingUndo()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeader(onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Reports",
            style = CatatTextStyles.LargeTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.tertiaryText(),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SearchPill(query: String, onQueryChanged: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .iosShadow(CatatShadows.shadowSm, cornerRadius = 999.dp)
            .clip(CatatShapes.full)
            .background(MaterialTheme.colorScheme.cardColor())
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiaryText(),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            singleLine = true,
            textStyle = CatatTextStyles.Callout.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isBlank()) {
                        Text(
                            "Search Reports",
                            style = CatatTextStyles.Callout,
                            color = MaterialTheme.colorScheme.tertiaryText()
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun FilterChips(
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 14.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            Surface(
                modifier = Modifier
                    .height(32.dp)
                    .clip(CatatShapes.full)
                    .clickable { onFilterSelected(filter) },
                shape = CatatShapes.full,
                color = if (selected) CatatColors.Accent else MaterialTheme.colorScheme.cardColor(),
                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                border = if (!selected || filter == HistoryFilter.All) {
                    BorderStroke(1.dp, if (filter == HistoryFilter.All) CatatColors.Accent else Color.Transparent)
                } else {
                    null
                }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        style = CatatTextStyles.Footnote,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StaggeredReportItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay((index * CatatAnimation.StaggerDelayMillis).coerceAtMost(450).toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(
            animationSpec = tween(260),
            initialOffsetY = { 20 }
        )
    ) {
        content()
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleReportRow(
    report: BugReport,
    onOpenReport: (Long) -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CatatShapes.md)
                    .background(CatatColors.Destructive)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        ReportCard(report = report, onClick = { onOpenReport(report.id) })
    }
}

@Composable
private fun ReportCard(report: BugReport, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .iosShadow(CatatShadows.shadowSm, cornerRadius = 12.dp)
            .pressScale(interactionSource)
            .clip(CatatShapes.md)
            .background(MaterialTheme.colorScheme.cardColor())
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReportThumbnail(report = report)
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = report.title.ifBlank { "Untitled bug report" },
                style = CatatTextStyles.Title3,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatDate(report.updatedAt)} · ${report.appInfo?.packageName ?: "Unknown app"}",
                style = CatatTextStyles.Footnote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            StatusBadge(status = report.status)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiaryText(),
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun ReportThumbnail(report: BugReport) {
    val path = report.annotatedScreenshotPath ?: report.screenshotPaths.firstOrNull()
    val image = remember(path) {
        path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CatatShapes.xs)
            .background(MaterialTheme.colorScheme.secondaryBackground()),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = CatatColors.Accent.copy(alpha = 0.15f),
                    topLeft = Offset(size.width * 0.20f, size.height * 0.20f),
                    size = Size(size.width * 0.60f, size.height * 0.60f),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                drawLine(
                    color = CatatColors.Accent,
                    start = Offset(size.width * 0.30f, size.height * 0.64f),
                    end = Offset(size.width * 0.70f, size.height * 0.36f),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ReportStatus) {
    val (label, color) = when (status) {
        ReportStatus.DRAFT -> "Draft" to CatatColors.Accent
        ReportStatus.EXPORTED -> "Exported" to CatatColors.Success
        ReportStatus.SYNCED -> "Synced" to CatatColors.Success
        ReportStatus.ARCHIVED -> "Archived" to MaterialTheme.colorScheme.tertiaryText()
    }
    Surface(
        shape = CatatShapes.xs,
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            style = CatatTextStyles.Caption2,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier, onNewCapture: () -> Unit) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyIllustration()
        Text(
            text = "No Reports Yet",
            style = CatatTextStyles.Title1,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            text = "Tap the floating button to capture your first bug report.",
            style = CatatTextStyles.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "? How to use",
            style = CatatTextStyles.Footnote,
            color = CatatColors.Accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 14.dp)
                .clip(CatatShapes.full)
                .clickable(onClick = onNewCapture)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun EmptyIllustration() {
    Canvas(modifier = Modifier.size(80.dp)) {
        drawCircle(
            color = CatatColors.Accent.copy(alpha = 0.12f),
            radius = size.minDimension * 0.48f,
            center = center
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * 0.28f, size.height * 0.18f),
            size = Size(size.width * 0.44f, size.height * 0.58f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawRoundRect(
            color = CatatColors.Accent.copy(alpha = 0.22f),
            topLeft = Offset(size.width * 0.36f, size.height * 0.34f),
            size = Size(size.width * 0.28f, size.height * 0.10f),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )
        drawCircle(
            color = CatatColors.Accent,
            radius = 12.dp.toPx(),
            center = Offset(size.width * 0.68f, size.height * 0.66f)
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.68f - 5.dp.toPx(), size.height * 0.66f),
            end = Offset(size.width * 0.68f + 5.dp.toPx(), size.height * 0.66f),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.68f, size.height * 0.66f - 5.dp.toPx()),
            end = Offset(size.width * 0.68f, size.height * 0.66f + 5.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
private fun HistoryLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(CatatShapes.md)
                    .background(MaterialTheme.colorScheme.cardColor())
            )
        }
    }
}

@Composable
private fun HistorySnackbar(data: SnackbarData) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .clip(CatatShapes.full),
        color = Color(0xEE1C1C1E),
        contentColor = Color.White,
        shape = CatatShapes.full,
        shadowElevation = 8.dp
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
                style = CatatTextStyles.Footnote,
                modifier = Modifier.weight(1f, fill = false)
            )
            data.visuals.actionLabel?.let { label ->
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = { data.performAction() }) {
                    Text(label, color = CatatColors.Accent, style = CatatTextStyles.Footnote)
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
}
