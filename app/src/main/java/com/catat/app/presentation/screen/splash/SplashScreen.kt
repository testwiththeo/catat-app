package com.catat.app.presentation.screen.splash

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.catat.app.presentation.screen.splash.SplashViewModel.SplashDestination
import com.catat.app.presentation.theme.CatatColors
import com.catat.app.presentation.theme.CatatTextStyles
import com.catat.app.presentation.theme.tertiaryText
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onOnboardingRequired: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsState()
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "splashFade"
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    LaunchedEffect(destination) {
        when (destination) {
            SplashDestination.Onboarding -> onOnboardingRequired()
            SplashDestination.History -> onNavigateToHistory()
            SplashDestination.Loading -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .alpha(alpha)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CatatSplashIcon(modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Catat",
                style = CatatTextStyles.LargeTitle,
                color = CatatColors.LightTextPrimary
            )
            Text(
                text = "Bug Report Companion",
                style = CatatTextStyles.Footnote,
                color = MaterialTheme.colorScheme.tertiaryText(),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        BouncingDots(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )
    }
}

@Composable
private fun CatatSplashIcon(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.drawBehind {
            drawCircle(
                color = CatatColors.Accent.copy(alpha = 0.28f),
                radius = size.minDimension * 0.56f,
                center = center
            )
            drawCircle(
                color = CatatColors.Accent.copy(alpha = 0.16f),
                radius = size.minDimension * 0.72f,
                center = center
            )
        }
    ) {
        val radius = size.minDimension / 2f
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(CatatColors.Accent, CatatColors.AccentDark),
                start = Offset(size.width * 0.15f, 0f),
                end = Offset(size.width * 0.85f, size.height)
            ),
            radius = radius,
            center = center
        )

        val apertureRadius = size.minDimension * 0.24f
        drawCircle(
            color = Color.White.copy(alpha = 0.95f),
            radius = apertureRadius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
        repeat(6) { index ->
            val angle = (index * 60f - 18f) * PI.toFloat() / 180f
            val start = Offset(
                x = center.x + cos(angle) * apertureRadius * 0.34f,
                y = center.y + sin(angle) * apertureRadius * 0.34f
            )
            val end = Offset(
                x = center.x + cos(angle + 0.42f) * apertureRadius * 1.15f,
                y = center.y + sin(angle + 0.42f) * apertureRadius * 1.15f
            )
            drawLine(
                color = Color.White,
                start = start,
                end = end,
                strokeWidth = 3.dp.toPx()
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.94f),
            radius = size.minDimension * 0.055f,
            center = center
        )
    }
}

@Composable
private fun BouncingDots(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val transition = rememberInfiniteTransition(label = "splashDot$index")
            val offsetY by transition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 300, delayMillis = index * 100),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "splashDotOffset$index"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { translationY = offsetY }
                    .clip(CircleShape)
                    .background(CatatColors.Accent)
            )
        }
    }
}
