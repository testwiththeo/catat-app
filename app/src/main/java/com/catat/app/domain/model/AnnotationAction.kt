package com.catat.app.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

sealed class AnnotationTool {
    object Arrow : AnnotationTool()
    object Rectangle : AnnotationTool()
    object Text : AnnotationTool()
    object Pen : AnnotationTool()
    object Blur : AnnotationTool()
}

sealed class BlurIntensity(val radius: Int) {
    object Light : BlurIntensity(8)
    object Medium : BlurIntensity(15)
    object Heavy : BlurIntensity(30)
}

data class AnnotationAction(
    val id: Long = System.nanoTime(),
    val tool: AnnotationTool,
    val path: List<Offset> = emptyList(),
    val color: Color = Color.Red,
    val strokeWidth: Float = 4f,
    val text: String? = null,
    val blurIntensity: BlurIntensity? = null
)
