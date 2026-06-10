package com.catat.app.presentation.screen.annotation

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.catat.app.domain.model.AnnotationAction
import com.catat.app.domain.model.AnnotationTool
import com.catat.app.domain.model.BlurIntensity
import com.catat.app.presentation.theme.CatatAnimation
import com.catat.app.presentation.theme.CatatColors
import com.catat.app.presentation.theme.CatatShapes
import com.catat.app.presentation.theme.CatatShadows
import com.catat.app.presentation.theme.CatatTextStyles
import com.catat.app.presentation.theme.iosShadow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val toolbarBackground = CatatColors.ToolbarChrome
private val toolbarText = Color.White
private val toolbarMuted = Color(0xFF8E8E93)

@Composable
fun AnnotationScreen(
    screenshotPath: String,
    reportId: Long?,
    onDone: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AnnotationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(screenshotPath, reportId) {
        viewModel.load(screenshotPath, reportId)
    }

    LaunchedEffect(uiState) {
        val saved = uiState as? AnnotationUiState.Saved
        if (saved != null) {
            onDone(saved.reportId)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        when (val state = uiState) {
            AnnotationUiState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CatatColors.Accent)
                }
            }
            is AnnotationUiState.Error -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is AnnotationUiState.Saved -> Unit
            is AnnotationUiState.Editing -> {
                AnnotationEditor(
                    state = state,
                    onBack = onBack,
                    onDone = viewModel::saveAnnotatedBitmap,
                    onToolSelected = viewModel::selectTool,
                    onColorSelected = viewModel::selectColor,
                    onStrokeChanged = viewModel::setStrokeWidth,
                    onBlurIntensityChanged = viewModel::setBlurIntensity,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onStartAction = viewModel::startAction,
                    onUpdateAction = viewModel::updateAction,
                    onFinishAction = viewModel::finishAction,
                    onAddText = viewModel::addText,
                    onDismissTextInput = viewModel::dismissTextInput
                )
            }
        }
    }
}

@Composable
private fun AnnotationEditor(
    state: AnnotationUiState.Editing,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onToolSelected: (AnnotationTool) -> Unit,
    onColorSelected: (Color) -> Unit,
    onStrokeChanged: (Float) -> Unit,
    onBlurIntensityChanged: (BlurIntensity) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onStartAction: (Offset) -> Unit,
    onUpdateAction: (Offset) -> Unit,
    onFinishAction: () -> Unit,
    onAddText: (String) -> Unit,
    onDismissTextInput: () -> Unit
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var textValue by remember(state.pendingTextPosition) { mutableStateOf("") }
    var showColorControls by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnnotationTopBar(
                isSaving = state.isSaving,
                onBack = onBack,
                onDone = onDone
            )
            AnnotationCanvas(
                state = state,
                zoom = zoom,
                pan = pan,
                onTransform = { zoomChange, panChange ->
                    val nextZoom = (zoom * zoomChange).coerceIn(1f, 3f)
                    zoom = nextZoom
                    pan = if (nextZoom > 1f) pan + panChange else Offset.Zero
                },
                onStartAction = onStartAction,
                onUpdateAction = onUpdateAction,
                onFinishAction = onFinishAction,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 118.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = state.errorMessage.orEmpty(),
                    color = CatatColors.Destructive,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xDD1C1C1E))
                        .padding(12.dp)
                )
            }

            AnimatedVisibility(
                visible = showColorControls,
                enter = slideInVertically(animationSpec = CatatAnimation.tweenDefault()) { it / 2 } +
                    fadeIn(animationSpec = CatatAnimation.tweenDefault()),
                exit = slideOutVertically(animationSpec = CatatAnimation.tweenDefault()) { it / 2 } +
                    fadeOut(animationSpec = CatatAnimation.tweenDefault())
            ) {
                ColorAndStrokePanel(
                    selectedColor = state.selectedColor,
                    strokeWidth = state.strokeWidth,
                    onColorSelected = onColorSelected,
                    onStrokeChanged = onStrokeChanged
                )
            }

            AnimatedVisibility(
                visible = state.selectedTool == AnnotationTool.Blur,
                enter = slideInVertically(animationSpec = CatatAnimation.tweenDefault()) { it / 2 } +
                    fadeIn(animationSpec = CatatAnimation.tweenDefault()),
                exit = slideOutVertically(animationSpec = CatatAnimation.tweenDefault()) { it / 2 } +
                    fadeOut(animationSpec = CatatAnimation.tweenDefault())
            ) {
                BlurIntensityPanel(
                    intensity = state.blurIntensity,
                    onIntensityChanged = onBlurIntensityChanged
                )
            }

            AnnotationToolbar(
                selectedTool = state.selectedTool,
                selectedColor = state.selectedColor,
                canUndo = state.actions.isNotEmpty(),
                canRedo = state.redoActions.isNotEmpty(),
                isSaving = state.isSaving,
                onToolSelected = onToolSelected,
                onUndo = onUndo,
                onRedo = onRedo,
                onToggleColorControls = { showColorControls = !showColorControls }
            )
        }
    }

    if (state.pendingTextPosition != null) {
        AlertDialog(
            onDismissRequest = onDismissTextInput,
            title = { Text("Add text") },
            text = {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { onAddText(textValue) }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissTextInput) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AnnotationTopBar(
    isSaving: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 8.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = "Annotate",
            color = Color.White,
            style = CatatTextStyles.Title3,
            modifier = Modifier.align(Alignment.Center)
        )
        TextButton(
            onClick = onDone,
            enabled = !isSaving,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(
                text = if (isSaving) "Saving..." else "Done",
                color = if (isSaving) toolbarMuted else CatatColors.Accent,
                style = CatatTextStyles.Body,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AnnotationCanvas(
    state: AnnotationUiState.Editing,
    zoom: Float,
    pan: Offset,
    onTransform: (Float, Offset) -> Unit,
    onStartAction: (Offset) -> Unit,
    onUpdateAction: (Offset) -> Unit,
    onFinishAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(state.baseBitmap) { state.baseBitmap.asImageBitmap() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .onSizeChanged { canvasSize = it }
            .pointerInput(state.selectedTool, state.baseBitmap, zoom, pan, canvasSize) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var drawing = false
                    var pressed: List<PointerInputChange>
                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                        toBitmapOffset(down.position, pan, zoom, canvasSize, state.baseBitmap.width, state.baseBitmap.height)
                            ?.let {
                                onStartAction(it)
                                drawing = true
                            }
                    }

                    do {
                        val event = awaitPointerEvent()
                        pressed = event.changes.filter { it.pressed }
                        if (pressed.size > 1) {
                            onTransform(event.calculateZoom(), event.calculatePan())
                            event.changes.forEach(PointerInputChange::consume)
                        } else {
                            val change = pressed.firstOrNull()
                            if (change != null && change.positionChanged()) {
                                toBitmapOffset(change.position, pan, zoom, canvasSize, state.baseBitmap.width, state.baseBitmap.height)
                                    ?.let(onUpdateAction)
                                change.consume()
                            }
                        }
                    } while (pressed.isNotEmpty())

                    if (drawing) {
                        onFinishAction()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = pan.x
                    translationY = pan.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        ) {
            val layout = imageLayout(size.width, size.height, state.baseBitmap.width, state.baseBitmap.height)
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(layout.left.roundToInt(), layout.top.roundToInt()),
                dstSize = IntSize(layout.width.roundToInt(), layout.height.roundToInt())
            )
            (state.actions + listOfNotNull(state.inFlightAction)).forEach { action ->
                drawAnnotationAction(action, layout, state.baseBitmap.width, state.baseBitmap.height)
            }
        }
    }
}

@Composable
private fun AnnotationToolbar(
    selectedTool: AnnotationTool,
    selectedColor: Color,
    canUndo: Boolean,
    canRedo: Boolean,
    isSaving: Boolean,
    onToolSelected: (AnnotationTool) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleColorControls: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .iosShadow(CatatShadows.shadowLg, cornerRadius = 999.dp)
            .clip(CatatShapes.full),
        color = toolbarBackground,
        contentColor = Color.White,
        shape = CatatShapes.full
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolButton(AnnotationTool.Arrow, selectedTool, onToolSelected) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
                ToolButton(AnnotationTool.Rectangle, selectedTool, onToolSelected) {
                    Icon(Icons.Default.CropSquare, contentDescription = null)
                }
                ToolButton(AnnotationTool.Text, selectedTool, onToolSelected) {
                    Icon(Icons.Default.TextFields, contentDescription = null)
                }
                ToolButton(AnnotationTool.Pen, selectedTool, onToolSelected) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
                ToolButton(AnnotationTool.Blur, selectedTool, onToolSelected) {
                    Icon(Icons.Default.BlurOn, contentDescription = null)
                }
            }

            ToolbarIconButton(
                enabled = canUndo && !isSaving,
                onClick = onUndo
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
            ToolbarIconButton(
                enabled = canRedo && !isSaving,
                onClick = onRedo
            ) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
            }
            ColorIndicator(
                color = selectedColor,
                onClick = onToggleColorControls
            )
        }
    }
}

@Composable
private fun ToolButton(
    tool: AnnotationTool,
    selectedTool: AnnotationTool,
    onToolSelected: (AnnotationTool) -> Unit,
    content: @Composable () -> Unit
) {
    val selected = tool::class == selectedTool::class
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable { onToolSelected(tool) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .alpha(if (selected) 1f else 0.78f),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalContentColor provides if (selected) toolbarText else toolbarMuted
            ) {
                content()
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(16.dp)
                    .height(2.dp)
                    .clip(CatatShapes.full)
                    .background(CatatColors.Accent)
            )
        }
    }
}

@Composable
private fun ToolbarIconButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.3f),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalContentColor provides toolbarMuted
        ) {
            Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

@Composable
private fun ColorIndicator(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
        )
    }
}

@Composable
private fun ColorAndStrokePanel(
    selectedColor: Color,
    strokeWidth: Float,
    onColorSelected: (Color) -> Unit,
    onStrokeChanged: (Float) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .iosShadow(CatatShadows.shadowLg, cornerRadius = 999.dp),
        color = toolbarBackground,
        contentColor = Color.White,
        shape = CatatShapes.full
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colorSwatches.forEach { color ->
                ColorSwatch(
                    color = color,
                    selected = color == selectedColor,
                    onClick = { onColorSelected(color) }
                )
            }
            RainbowSwatch()
            Spacer(Modifier.weight(1f))
            StrokeDots(
                strokeWidth = strokeWidth,
                onStrokeChanged = onStrokeChanged
            )
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 2.dp else 0.5.dp,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun RainbowSwatch() {
    Canvas(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
    ) {
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                )
            ),
            radius = 11.dp.toPx()
        )
        drawCircle(
            color = Color.White,
            radius = 11.dp.toPx(),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun StrokeDots(strokeWidth: Float, onStrokeChanged: (Float) -> Unit) {
    val options = listOf(4f, 8f, 16f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { width ->
            val selected = strokeWidth.roundToInt() == width.roundToInt()
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onStrokeChanged(width) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width.dp)
                        .clip(CircleShape)
                        .then(
                            if (selected) {
                                Modifier.background(Color.White)
                            } else {
                                Modifier.border(1.dp, toolbarMuted, CircleShape)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun BlurIntensityPanel(
    intensity: BlurIntensity,
    onIntensityChanged: (BlurIntensity) -> Unit
) {
    Surface(
        color = toolbarBackground,
        contentColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Blur",
                color = toolbarMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(48.dp)
            )
            Slider(
                value = intensity.toSliderValue(),
                onValueChange = { onIntensityChanged(it.toBlurIntensity()) },
                valueRange = 0f..2f,
                steps = 1,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = CatatColors.Accent,
                    activeTrackColor = CatatColors.Accent,
                    inactiveTrackColor = toolbarMuted.copy(alpha = 0.35f)
                )
            )
            Text(
                text = intensity.label(),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(54.dp)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAnnotationAction(
    action: AnnotationAction,
    layout: Rect,
    bitmapWidth: Int,
    bitmapHeight: Int
) {
    val points = action.path.map { point ->
        Offset(
            x = layout.left + point.x / bitmapWidth.toFloat() * layout.width,
            y = layout.top + point.y / bitmapHeight.toFloat() * layout.height
        )
    }
    if (points.isEmpty()) return
    val stroke = Stroke(width = action.strokeWidth)

    when (action.tool) {
        AnnotationTool.Arrow -> {
            if (points.size < 2) return
            val start = points.first()
            val end = points.last()
            drawLine(action.color, start, end, strokeWidth = action.strokeWidth)
            val angle = atan2(end.y - start.y, end.x - start.x)
            val arrowLength = (action.strokeWidth * 5f).coerceAtLeast(18f)
            val arrowAngle = PI.toFloat() / 6f
            val left = Offset(
                x = end.x - arrowLength * cos(angle - arrowAngle),
                y = end.y - arrowLength * sin(angle - arrowAngle)
            )
            val right = Offset(
                x = end.x - arrowLength * cos(angle + arrowAngle),
                y = end.y - arrowLength * sin(angle + arrowAngle)
            )
            drawLine(action.color, end, left, strokeWidth = action.strokeWidth)
            drawLine(action.color, end, right, strokeWidth = action.strokeWidth)
        }
        AnnotationTool.Rectangle -> {
            if (points.size < 2) return
            val rect = Rect(points.first(), points.last()).normalize()
            drawRect(action.color, rect.topLeft, rect.size, style = stroke)
        }
        AnnotationTool.Text -> {
            val text = action.text ?: return
            drawContext.canvas.nativeCanvas.drawText(
                text,
                points.first().x,
                points.first().y,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = action.color.toArgb()
                    textSize = (action.strokeWidth * 9f).coerceAtLeast(28f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
            )
        }
        AnnotationTool.Pen -> {
            points.zipWithNext { start, end ->
                drawLine(action.color, start, end, strokeWidth = action.strokeWidth)
            }
        }
        AnnotationTool.Blur -> {
            if (points.size < 2) return
            val rect = Rect(points.first(), points.last()).normalize()
            drawRect(Color.White.copy(alpha = 0.18f), rect.topLeft, rect.size)
            drawRect(Color.White.copy(alpha = 0.85f), rect.topLeft, rect.size, style = stroke)
        }
    }
}

private fun toBitmapOffset(
    position: Offset,
    pan: Offset,
    zoom: Float,
    canvasSize: IntSize,
    bitmapWidth: Int,
    bitmapHeight: Int
): Offset? {
    if (canvasSize.width == 0 || canvasSize.height == 0) return null
    val untransformed = (position - pan) / zoom
    val layout = imageLayout(
        canvasWidth = canvasSize.width.toFloat(),
        canvasHeight = canvasSize.height.toFloat(),
        bitmapWidth = bitmapWidth,
        bitmapHeight = bitmapHeight
    )
    if (!layout.contains(untransformed)) return null
    return Offset(
        x = ((untransformed.x - layout.left) / layout.width * bitmapWidth).coerceIn(0f, bitmapWidth.toFloat()),
        y = ((untransformed.y - layout.top) / layout.height * bitmapHeight).coerceIn(0f, bitmapHeight.toFloat())
    )
}

private fun imageLayout(
    canvasWidth: Float,
    canvasHeight: Float,
    bitmapWidth: Int,
    bitmapHeight: Int
): Rect {
    val scale = min(canvasWidth / bitmapWidth.toFloat(), canvasHeight / bitmapHeight.toFloat())
    val width = bitmapWidth * scale
    val height = bitmapHeight * scale
    val left = (canvasWidth - width) / 2f
    val top = (canvasHeight - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun Rect.normalize(): Rect {
    return Rect(
        left = minOf(left, right),
        top = minOf(top, bottom),
        right = maxOf(left, right),
        bottom = maxOf(top, bottom)
    )
}

private fun BlurIntensity.toSliderValue(): Float = when (this) {
    BlurIntensity.Light -> 0f
    BlurIntensity.Medium -> 1f
    BlurIntensity.Heavy -> 2f
}

private fun Float.toBlurIntensity(): BlurIntensity = when (roundToInt()) {
    0 -> BlurIntensity.Light
    2 -> BlurIntensity.Heavy
    else -> BlurIntensity.Medium
}

private fun BlurIntensity.label(): String = when (this) {
    BlurIntensity.Light -> "Light"
    BlurIntensity.Medium -> "Medium"
    BlurIntensity.Heavy -> "Heavy"
}

private val colorSwatches = listOf(
    Color(0xFFFF3B30),
    Color(0xFFFFCC00),
    Color(0xFF34C759),
    Color(0xFF007AFF),
    Color.White,
    Color.Black
)
