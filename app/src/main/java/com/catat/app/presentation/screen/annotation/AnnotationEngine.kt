package com.catat.app.presentation.screen.annotation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import com.catat.app.domain.model.AnnotationAction
import com.catat.app.domain.model.AnnotationTool
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class AnnotationEngine @Inject constructor() {

    fun render(baseBitmap: Bitmap, actions: List<AnnotationAction>): Bitmap {
        val output = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(baseBitmap, 0f, 0f, null)

        actions.forEach { action ->
            when (action.tool) {
                AnnotationTool.Arrow -> drawArrow(canvas, action)
                AnnotationTool.Rectangle -> drawRectangle(canvas, action)
                AnnotationTool.Text -> drawText(canvas, action)
                AnnotationTool.Pen -> drawPen(canvas, action)
                AnnotationTool.Blur -> applyBlur(output, canvas, action)
            }
        }

        return output
    }

    private fun drawArrow(canvas: Canvas, action: AnnotationAction) {
        if (action.path.size < 2) return
        val start = action.path.first()
        val end = action.path.last()
        val paint = strokePaint(action)
        canvas.drawLine(start.x, start.y, end.x, end.y, paint)

        val angle = atan2(end.y - start.y, end.x - start.x)
        val arrowLength = max(action.strokeWidth * 5f, 18f)
        val arrowAngle = PI.toFloat() / 6f
        val left = Offset(
            x = end.x - arrowLength * cos(angle - arrowAngle),
            y = end.y - arrowLength * sin(angle - arrowAngle)
        )
        val right = Offset(
            x = end.x - arrowLength * cos(angle + arrowAngle),
            y = end.y - arrowLength * sin(angle + arrowAngle)
        )

        canvas.drawLine(end.x, end.y, left.x, left.y, paint)
        canvas.drawLine(end.x, end.y, right.x, right.y, paint)
    }

    private fun drawRectangle(canvas: Canvas, action: AnnotationAction) {
        val rect = action.path.bounds() ?: return
        canvas.drawRect(rect, strokePaint(action))
    }

    private fun drawText(canvas: Canvas, action: AnnotationAction) {
        val point = action.path.firstOrNull() ?: return
        val text = action.text?.takeIf { it.isNotBlank() } ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = action.color.toArgb()
            style = Paint.Style.FILL
            textSize = max(action.strokeWidth * 9f, 28f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText(text, point.x, point.y, paint)
    }

    private fun drawPen(canvas: Canvas, action: AnnotationAction) {
        if (action.path.size < 2) return
        val paint = strokePaint(action).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        action.path.zipWithNext { start, end ->
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
    }

    private fun applyBlur(output: Bitmap, canvas: Canvas, action: AnnotationAction) {
        val rect = action.path.bounds()?.toClippedRect(output.width, output.height) ?: return
        if (rect.width() <= 1 || rect.height() <= 1) return

        val crop = Bitmap.createBitmap(output, rect.left, rect.top, rect.width(), rect.height())
        val blurred = stackBlur(crop, action.blurIntensity?.radius ?: DEFAULT_BLUR_RADIUS)
        canvas.drawBitmap(blurred, rect.left.toFloat(), rect.top.toFloat(), null)
        crop.recycle()
        blurred.recycle()
    }

    fun stackBlur(source: Bitmap, radius: Int): Bitmap {
        val safeRadius = radius.coerceAtLeast(1)
        val width = source.width
        val height = source.height
        val src = IntArray(width * height)
        val horizontal = IntArray(width * height)
        val out = IntArray(width * height)
        source.getPixels(src, 0, width, 0, 0, width, height)

        val windowSize = safeRadius * 2 + 1
        for (y in 0 until height) {
            var aSum = 0
            var rSum = 0
            var gSum = 0
            var bSum = 0
            for (i in -safeRadius..safeRadius) {
                val pixel = src[y * width + i.coerceIn(0, width - 1)]
                aSum += pixel ushr 24
                rSum += pixel shr 16 and 0xFF
                gSum += pixel shr 8 and 0xFF
                bSum += pixel and 0xFF
            }
            for (x in 0 until width) {
                horizontal[y * width + x] = argb(aSum / windowSize, rSum / windowSize, gSum / windowSize, bSum / windowSize)
                val removeX = (x - safeRadius).coerceIn(0, width - 1)
                val addX = (x + safeRadius + 1).coerceIn(0, width - 1)
                val removePixel = src[y * width + removeX]
                val addPixel = src[y * width + addX]
                aSum += (addPixel ushr 24) - (removePixel ushr 24)
                rSum += (addPixel shr 16 and 0xFF) - (removePixel shr 16 and 0xFF)
                gSum += (addPixel shr 8 and 0xFF) - (removePixel shr 8 and 0xFF)
                bSum += (addPixel and 0xFF) - (removePixel and 0xFF)
            }
        }

        for (x in 0 until width) {
            var aSum = 0
            var rSum = 0
            var gSum = 0
            var bSum = 0
            for (i in -safeRadius..safeRadius) {
                val pixel = horizontal[i.coerceIn(0, height - 1) * width + x]
                aSum += pixel ushr 24
                rSum += pixel shr 16 and 0xFF
                gSum += pixel shr 8 and 0xFF
                bSum += pixel and 0xFF
            }
            for (y in 0 until height) {
                out[y * width + x] = argb(aSum / windowSize, rSum / windowSize, gSum / windowSize, bSum / windowSize)
                val removeY = (y - safeRadius).coerceIn(0, height - 1)
                val addY = (y + safeRadius + 1).coerceIn(0, height - 1)
                val removePixel = horizontal[removeY * width + x]
                val addPixel = horizontal[addY * width + x]
                aSum += (addPixel ushr 24) - (removePixel ushr 24)
                rSum += (addPixel shr 16 and 0xFF) - (removePixel shr 16 and 0xFF)
                gSum += (addPixel shr 8 and 0xFF) - (removePixel shr 8 and 0xFF)
                bSum += (addPixel and 0xFF) - (removePixel and 0xFF)
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(out, 0, width, 0, 0, width, height)
        }
    }

    private fun strokePaint(action: AnnotationAction): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = action.color.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = action.strokeWidth
            strokeCap = Paint.Cap.SQUARE
            strokeJoin = Paint.Join.MITER
        }
    }

    private fun List<Offset>.bounds(): RectF? {
        if (isEmpty()) return null
        val minX = minOf { it.x }
        val minY = minOf { it.y }
        val maxX = maxOf { it.x }
        val maxY = maxOf { it.y }
        return RectF(min(minX, maxX), min(minY, maxY), max(minX, maxX), max(minY, maxY))
    }

    private fun RectF.toClippedRect(width: Int, height: Int): Rect {
        return Rect(
            left.roundToInt().coerceIn(0, width - 1),
            top.roundToInt().coerceIn(0, height - 1),
            right.roundToInt().coerceIn(1, width),
            bottom.roundToInt().coerceIn(1, height)
        )
    }

    private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return (alpha.coerceIn(0, 255) shl 24) or
            (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    private companion object {
        private const val DEFAULT_BLUR_RADIUS = 15
    }
}
