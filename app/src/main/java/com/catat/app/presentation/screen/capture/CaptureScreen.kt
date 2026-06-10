package com.catat.app.presentation.screen.capture

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.catat.app.presentation.theme.CatatColors
import com.catat.app.presentation.theme.cardColor
import com.catat.app.presentation.theme.secondaryBackground

@Composable
fun CaptureScreen(
    capturePath: String?,
    reportId: Long?,
    onRetake: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onUseCapture: (String, Long?) -> Unit,
    onBack: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(capturePath) {
        viewModel.acceptCapturePath(capturePath)
    }

    val screenshotPath = state.screenshotPath
    val screenshotBitmap = remember(screenshotPath) {
        screenshotPath?.let { path ->
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }
    val usablePath = screenshotPath.takeIf { screenshotBitmap != null }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CaptureToolbar(onBack = onBack)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                color = Color.Black,
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        screenshotBitmap != null -> {
                            Image(
                                bitmap = screenshotBitmap,
                                contentDescription = "Captured screenshot preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        state.isLoading -> {
                            CircularProgressIndicator(color = CatatColors.Accent)
                        }
                        state.isWaitingForCapture -> {
                            CaptureEmptyState(
                                title = "Waiting for screenshot",
                                subtitle = "Approve screen capture to save a preview."
                            )
                        }
                        else -> {
                            CaptureEmptyState(
                                title = "No screenshot yet",
                                subtitle = "Start a capture or use the floating button."
                            )
                        }
                    }
                }
            }

            CaptureActions(
                hasCapture = usablePath != null,
                onRetake = {
                    viewModel.markRetakeRequested()
                    onRetake()
                },
                onRequestOverlayPermission = onRequestOverlayPermission,
                onUseCapture = {
                    usablePath?.let { onUseCapture(it, reportId) }
                }
            )
        }
    }
}

@Composable
private fun CaptureToolbar(onBack: () -> Unit) {
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
            text = "Capture",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CaptureEmptyState(title: String, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Canvas(modifier = Modifier.size(112.dp)) {
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = size.minDimension * 0.46f,
                center = center
            )
            drawRoundRect(
                color = CatatColors.Accent.copy(alpha = 0.30f),
                topLeft = Offset(size.width * 0.24f, size.height * 0.28f),
                size = Size(size.width * 0.52f, size.height * 0.40f),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = 15.dp.toPx(),
                center = center
            )
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun CaptureActions(
    hasCapture: Boolean,
    onRetake: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onUseCapture: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CaptureActionPill(
                label = "Retake",
                icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                onClick = onRetake,
                filled = false,
                enabled = true,
                modifier = Modifier.weight(1f)
            )
            CaptureActionPill(
                label = "Annotate",
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                onClick = onUseCapture,
                filled = true,
                enabled = hasCapture,
                modifier = Modifier.weight(1f)
            )
        }

        CaptureActionPill(
            label = "Enable Floating Button",
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            onClick = onRequestOverlayPermission,
            filled = false,
            enabled = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CaptureActionPill(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    filled: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val container = if (filled && enabled) CatatColors.Accent else Color.Transparent
    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        filled -> Color.White
        else -> CatatColors.Accent
    }
    Surface(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = container,
        contentColor = content,
        border = if (filled && enabled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                icon()
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
