package com.catat.app.presentation.screen.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.catat.app.presentation.theme.CatatAnimation
import com.catat.app.presentation.theme.CatatColors
import com.catat.app.presentation.theme.CatatShapes
import com.catat.app.presentation.theme.CatatTextStyles
import com.catat.app.presentation.theme.pressScale
import com.catat.app.presentation.theme.rememberPressInteractionSource
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private val onboardingPages = listOf(
    OnboardingPage(
        title = "Capture",
        body = "Use the floating button to capture the exact screen where a bug appears.",
        illustration = OnboardingIllustration.Capture
    ),
    OnboardingPage(
        title = "Annotate",
        body = "Mark the issue with arrows, shapes, pen notes, text, and blur.",
        illustration = OnboardingIllustration.Annotate
    ),
    OnboardingPage(
        title = "Export",
        body = "Turn every report into Jira, GitHub, Linear, or Markdown in one tap.",
        illustration = OnboardingIllustration.Export
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val buttonInteractionSource = rememberPressInteractionSource()

    fun complete() {
        viewModel.completeOnboarding()
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = ::complete) {
                Text(
                    "Skip",
                    style = CatatTextStyles.Caption1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            val page = onboardingPages[pageIndex]
            val pageOffset = (
                (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
            )
            OnboardingPageContent(
                page = page,
                pageOffset = pageOffset,
                modifier = Modifier.fillMaxSize()
            )
        }

        PageDots(currentPage = pagerState.currentPage, count = onboardingPages.size)

        Spacer(Modifier.height(24.dp))

        OnboardingButton(
            label = if (pagerState.currentPage == onboardingPages.lastIndex) "Get Started" else "Next",
            interactionSource = buttonInteractionSource,
            onClick = {
                if (pagerState.currentPage == onboardingPages.lastIndex) {
                    complete()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            }
        )
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageOffset: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f),
                contentAlignment = Alignment.Center
            ) {
                OnboardingIllustrationView(
                    illustration = page.illustration,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = pageOffset * widthPx * 0.30f
                            alpha = (1f - pageOffset.absoluteValue * 0.35f).coerceIn(0.55f, 1f)
                        }
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = page.title,
                style = CatatTextStyles.Title2,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = page.body,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                style = CatatTextStyles.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PageDots(currentPage: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val size = animateDpAsState(
                targetValue = if (index == currentPage) 8.dp else 8.dp,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
                label = "onboardingDotSize$index"
            )
            Box(
                modifier = Modifier
                    .size(size.value)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            CatatColors.Accent
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
            )
        }
    }
}

@Composable
private fun OnboardingButton(
    label: String,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(120.dp)
            .height(50.dp)
            .pressScale(interactionSource, pressedScale = 0.95f)
            .clip(CatatShapes.full)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = CatatColors.Accent,
        contentColor = Color.White,
        shape = CatatShapes.full
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = CatatTextStyles.Body,
                color = Color.White
            )
        }
    }
}

@Composable
private fun OnboardingIllustrationView(
    illustration: OnboardingIllustration,
    modifier: Modifier = Modifier
) {
    val secondaryBackground = MaterialTheme.colorScheme.surface
    val separator = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier) {
        val bgRadius = size.minDimension * 0.43f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CatatColors.Accent.copy(alpha = 0.17f),
                    CatatColors.Accent.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = center,
                radius = bgRadius * 1.4f
            ),
            radius = bgRadius * 1.4f,
            center = center
        )

        val phoneWidth = size.width * 0.48f
        val phoneHeight = size.height * 0.68f
        val phoneLeft = (size.width - phoneWidth) / 2f
        val phoneTop = size.height * 0.12f
        val corner = 26.dp.toPx()

        drawRoundRect(
            color = secondaryBackground,
            topLeft = Offset(phoneLeft, phoneTop),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = separator.copy(alpha = 0.8f),
            topLeft = Offset(phoneLeft, phoneTop),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1.5.dp.toPx())
        )

        when (illustration) {
            OnboardingIllustration.Capture -> {
                drawRoundRect(
                    color = CatatColors.Accent.copy(alpha = 0.18f),
                    topLeft = Offset(phoneLeft + 26.dp.toPx(), phoneTop + 40.dp.toPx()),
                    size = Size(phoneWidth - 52.dp.toPx(), 70.dp.toPx()),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                drawCircle(
                    color = CatatColors.Accent,
                    radius = 24.dp.toPx(),
                    center = Offset(phoneLeft + phoneWidth + 4.dp.toPx(), phoneTop + phoneHeight * 0.60f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(phoneLeft + phoneWidth + 4.dp.toPx(), phoneTop + phoneHeight * 0.60f)
                )
            }

            OnboardingIllustration.Annotate -> {
                val start = Offset(phoneLeft + 40.dp.toPx(), phoneTop + phoneHeight * 0.65f)
                val end = Offset(phoneLeft + phoneWidth - 42.dp.toPx(), phoneTop + phoneHeight * 0.34f)
                drawLine(CatatColors.Accent, start, end, strokeWidth = 4.dp.toPx())
                drawLine(CatatColors.Accent, end, end + Offset(-19.dp.toPx(), 2.dp.toPx()), strokeWidth = 4.dp.toPx())
                drawRoundRect(
                    color = CatatColors.Warning.copy(alpha = 0.20f),
                    topLeft = Offset(phoneLeft + 38.dp.toPx(), phoneTop + 42.dp.toPx()),
                    size = Size(phoneWidth * 0.52f, 58.dp.toPx()),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = CatatColors.Success,
                    radius = 10.dp.toPx(),
                    center = Offset(phoneLeft + phoneWidth - 46.dp.toPx(), phoneTop + phoneHeight - 42.dp.toPx())
                )
                drawCircle(
                    color = CatatColors.Accent,
                    radius = 10.dp.toPx(),
                    center = Offset(phoneLeft + phoneWidth - 76.dp.toPx(), phoneTop + phoneHeight - 42.dp.toPx())
                )
            }

            OnboardingIllustration.Export -> {
                repeat(3) { index ->
                    drawRoundRect(
                        color = listOf(CatatColors.Accent, CatatColors.Success, CatatColors.Warning)[index].copy(alpha = 0.22f),
                        topLeft = Offset(phoneLeft + 32.dp.toPx(), phoneTop + 48.dp.toPx() + index * 50.dp.toPx()),
                        size = Size(phoneWidth - 64.dp.toPx(), 30.dp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
                drawCircle(
                    color = CatatColors.Accent,
                    radius = 25.dp.toPx(),
                    center = Offset(size.width * 0.70f, phoneTop + phoneHeight * 0.76f)
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.70f - 10.dp.toPx(), phoneTop + phoneHeight * 0.76f),
                    end = Offset(size.width * 0.70f + 11.dp.toPx(), phoneTop + phoneHeight * 0.76f),
                    strokeWidth = 3.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.70f + 3.dp.toPx(), phoneTop + phoneHeight * 0.76f - 8.dp.toPx()),
                    end = Offset(size.width * 0.70f + 11.dp.toPx(), phoneTop + phoneHeight * 0.76f),
                    strokeWidth = 3.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.70f + 3.dp.toPx(), phoneTop + phoneHeight * 0.76f + 8.dp.toPx()),
                    end = Offset(size.width * 0.70f + 11.dp.toPx(), phoneTop + phoneHeight * 0.76f),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
    }
}

private data class OnboardingPage(
    val title: String,
    val body: String,
    val illustration: OnboardingIllustration
)

private enum class OnboardingIllustration {
    Capture,
    Annotate,
    Export
}
