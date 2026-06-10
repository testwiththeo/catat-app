package com.catat.app.presentation.theme

import android.graphics.Paint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catat.app.R

object CatatColors {
    val LightBackground = Color(0xFFF2F2F7)
    val LightCardBackground = Color(0xFFFFFFFF)
    val LightTextPrimary = Color(0xFF1C1C1E)
    val LightTextSecondary = Color(0xFF3A3A3C)
    val LightTextTertiary = Color(0xFF8A8A8E)
    val LightSeparator = Color(0xFFC6C6C8)

    val DarkBackground = Color(0xFF1C1C1E)
    val DarkCardBackground = Color(0xFF2C2C2E)
    val DarkTextPrimary = Color(0xFFFFFFFF)
    val DarkTextSecondary = Color(0xFFAEAEB2)
    val DarkTextTertiary = Color(0xFF636366)
    val DarkSeparator = Color(0xFF38383A)

    val Accent = Color(0xFF0A84FF)
    val AccentDark = Color(0xFF0063CE)
    val Destructive = Color(0xFFFF453A)
    val Success = Color(0xFF30D158)
    val Warning = Color(0xFFFFD60A)
    val PreviewBackground = Color(0xFFFAFAFA)
    val ToolbarChrome = Color(0xF22C2C2E)
}

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

object CatatTextStyles {
    val LargeTitle = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 41.sp,
        letterSpacing = 0.sp
    )
    val Title1 = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    )
    val Title2 = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
    val Title3 = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    )
    val Body = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )
    val Callout = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    )
    val Subheadline = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
    val Footnote = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )
    val Caption1 = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
    val Caption2 = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 13.sp,
        letterSpacing = 0.sp
    )
}

object CatatShapes {
    val xs = RoundedCornerShape(6.dp)
    val sm = RoundedCornerShape(10.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(20.dp)
    val full = RoundedCornerShape(999.dp)
}

@Immutable
data class CatatShadow(
    val offsetY: Dp,
    val blur: Dp,
    val alpha: Float
)

object CatatShadows {
    val shadowSm = CatatShadow(offsetY = 1.dp, blur = 3.dp, alpha = 0.12f)
    val shadowMd = CatatShadow(offsetY = 2.dp, blur = 6.dp, alpha = 0.15f)
    val shadowLg = CatatShadow(offsetY = 4.dp, blur = 12.dp, alpha = 0.18f)
    val shadowXl = CatatShadow(offsetY = 8.dp, blur = 24.dp, alpha = 0.22f)
}

object CatatAnimation {
    const val StaggerDelayMillis = 50
    val springStiff = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 200f
    )
    val springBouncy = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 150f
    )
    val springDefault = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    fun <T> tweenDefault() = tween<T>(
        durationMillis = 350,
        easing = FastOutSlowInEasing
    )
}

private val LightColorScheme = lightColorScheme(
    primary = CatatColors.Accent,
    onPrimary = Color.White,
    secondary = CatatColors.LightTextSecondary,
    onSecondary = CatatColors.LightCardBackground,
    background = CatatColors.LightBackground,
    onBackground = CatatColors.LightTextPrimary,
    surface = CatatColors.LightCardBackground,
    onSurface = CatatColors.LightTextPrimary,
    surfaceVariant = CatatColors.LightBackground,
    onSurfaceVariant = CatatColors.LightTextSecondary,
    outline = CatatColors.LightSeparator,
    outlineVariant = CatatColors.LightSeparator.copy(alpha = 0.55f),
    error = CatatColors.Destructive,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = CatatColors.Accent,
    onPrimary = Color.White,
    secondary = CatatColors.DarkTextSecondary,
    onSecondary = CatatColors.DarkCardBackground,
    background = CatatColors.DarkBackground,
    onBackground = CatatColors.DarkTextPrimary,
    surface = CatatColors.DarkCardBackground,
    onSurface = CatatColors.DarkTextPrimary,
    surfaceVariant = CatatColors.DarkBackground,
    onSurfaceVariant = CatatColors.DarkTextSecondary,
    outline = CatatColors.DarkSeparator,
    outlineVariant = CatatColors.DarkSeparator,
    error = CatatColors.Destructive,
    onError = Color.White
)

private val CatatMaterialTypography = Typography(
    displayLarge = CatatTextStyles.LargeTitle,
    headlineLarge = CatatTextStyles.Title1,
    headlineMedium = CatatTextStyles.Title2,
    titleLarge = CatatTextStyles.Title3,
    titleMedium = CatatTextStyles.Subheadline,
    bodyLarge = CatatTextStyles.Body,
    bodyMedium = CatatTextStyles.Callout,
    bodySmall = CatatTextStyles.Footnote,
    labelLarge = CatatTextStyles.Body.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = CatatTextStyles.Caption1,
    labelSmall = CatatTextStyles.Caption2
)

private val CatatMaterialShapes = Shapes(
    extraSmall = CatatShapes.xs,
    small = CatatShapes.sm,
    medium = CatatShapes.md,
    large = CatatShapes.lg,
    extraLarge = CatatShapes.xl
)

@Composable
fun CatatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = CatatMaterialTypography,
        shapes = CatatMaterialShapes,
        content = content
    )
}

@Composable
fun ColorScheme.cardColor(): Color = surface

@Composable
fun ColorScheme.secondaryBackground(): Color = background

@Composable
fun ColorScheme.tertiaryText(): Color =
    if (isSystemInDarkTheme()) CatatColors.DarkTextTertiary else CatatColors.LightTextTertiary

@Composable
fun ColorScheme.separatorColor(): Color =
    if (isSystemInDarkTheme()) CatatColors.DarkSeparator else CatatColors.LightSeparator

@Composable
fun ColorScheme.previewBackground(): Color =
    if (isSystemInDarkTheme()) CatatColors.DarkCardBackground else CatatColors.PreviewBackground

fun Modifier.iosShadow(
    shadow: CatatShadow = CatatShadows.shadowSm,
    cornerRadius: Dp = 12.dp
): Modifier = composed {
    val density = LocalDensity.current
    drawBehind {
        val radiusPx = with(density) { cornerRadius.toPx() }
        val blurPx = with(density) { shadow.blur.toPx() }
        val offsetYPx = with(density) { shadow.offsetY.toPx() }
        drawIntoCanvas { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.Black.copy(alpha = 0.01f).toArgb()
                setShadowLayer(
                    blurPx,
                    0f,
                    offsetYPx,
                    Color.Black.copy(alpha = shadow.alpha).toArgb()
                )
            }
            canvas.nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                radiusPx,
                radiusPx,
                paint
            )
        }
    }
}

fun Modifier.appleCard(
    cornerRadius: Dp = 12.dp,
    shadow: CatatShadow = CatatShadows.shadowSm
): Modifier = composed {
    iosShadow(shadow = shadow, cornerRadius = cornerRadius)
        .clip(RoundedCornerShape(cornerRadius))
        .background(MaterialTheme.colorScheme.cardColor())
}

fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
    pressedAlpha: Float = 0.9f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    scale(if (pressed) pressedScale else 1f)
        .alpha(if (pressed) pressedAlpha else 1f)
}

@Composable
fun rememberPressInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }

fun Modifier.shimmer(shape: RoundedCornerShape = CatatShapes.md): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    val base = MaterialTheme.colorScheme.background
    val highlight = if (isSystemInDarkTheme()) Color(0xFF38383A) else Color(0xFFE5E5EA)
    clip(shape).background(
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(offset, 0f),
            end = Offset(offset + 420f, 0f)
        )
    )
}

@Composable
fun appleButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = CatatColors.Accent,
    contentColor = Color.White
)
