package com.example.ticketapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ticketapp.ui.theme.TicketAppTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

private val greenProbabilitySteps = listOf(0.15f, 0.35f, 0.60f, 0.80f, 1.00f)
private val redProbabilitySteps = listOf(0.10f, 0.25f, 0.50f, 0.75f, 1.00f)

private val Background = Color(0xFF111318)
private val Panel = Color(0xFF1B1E25)
private val Green = Color(0xFF37C977)
private val GreenDark = Color(0xFF123B29)
private val Red = Color(0xFFFF5D64)
private val RedDark = Color(0xFF481D22)
private val Cream = Color(0xFFF5F0E6)
private val Muted = Color(0xFF8B9099)

private enum class TicketColour {
    GREEN,
    RED,
}

private sealed interface AppScreen {
    data object Ready : AppScreen
    data class Result(val ticket: TicketColour?) : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        enterImmersiveMode()

        setContent {
            TicketAppTheme(dynamicColor = false) {
                TicketApp()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun TicketApp() {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Ready) }

    when (val current = screen) {
        AppScreen.Ready -> ReadyScreen { colour, probability ->
            val won = Random.nextFloat() < probability
            screen = AppScreen.Result(ticket = colour.takeIf { won })
        }

        is AppScreen.Result -> ResultScreen(current.ticket) {
            screen = AppScreen.Ready
        }
    }
}

@Composable
private fun ReadyScreen(onRoll: (TicketColour, Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 22.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "TICKET TOSS",
                color = Cream,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            )
            Spacer(Modifier.height(22.dp))
            TicketCard(
                colour = Green,
                darkColour = GreenDark,
                eyebrow = "GOOD CALL",
                title = "GREEN TICKET",
            )
            Spacer(Modifier.height(16.dp))
            TicketCard(
                colour = Red,
                darkColour = RedDark,
                eyebrow = "TRY AGAIN",
                title = "RED TICKET",
            )
        }

        GesturePanel(
            modifier = Modifier.align(Alignment.BottomEnd),
            onRoll = onRoll,
        )
    }
}

@Composable
private fun TicketCard(
    colour: Color,
    darkColour: Color,
    eyebrow: String,
    title: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawTicket(colour, darkColour)
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 34.dp),
        ) {
            Text(
                text = eyebrow,
                color = darkColour.copy(alpha = 0.78f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                text = title,
                color = darkColour,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            text = "●  ●  ●",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 22.dp),
            color = darkColour.copy(alpha = 0.52f),
            fontSize = 11.sp,
        )
    }
}

private fun DrawScope.drawTicket(colour: Color, darkColour: Color) {
    val notch = 14.dp.toPx()
    val radius = 22.dp.toPx()
    val path = Path().apply {
        moveTo(radius, 0f)
        lineTo(size.width - radius, 0f)
        quadraticTo(size.width, 0f, size.width, radius)
        lineTo(size.width, size.height / 2f - notch)
        quadraticTo(size.width - notch, size.height / 2f, size.width, size.height / 2f + notch)
        lineTo(size.width, size.height - radius)
        quadraticTo(size.width, size.height, size.width - radius, size.height)
        lineTo(radius, size.height)
        quadraticTo(0f, size.height, 0f, size.height - radius)
        lineTo(0f, size.height / 2f + notch)
        quadraticTo(notch, size.height / 2f, 0f, size.height / 2f - notch)
        lineTo(0f, radius)
        quadraticTo(0f, 0f, radius, 0f)
        close()
    }
    drawPath(path, colour)
    drawRoundRect(
        color = darkColour.copy(alpha = 0.26f),
        topLeft = Offset(18.dp.toPx(), 18.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(
            width = size.width - 36.dp.toPx(),
            height = size.height - 36.dp.toPx(),
        ),
        cornerRadius = CornerRadius(14.dp.toPx()),
        style = Stroke(width = 2.dp.toPx()),
    )
}

@Composable
private fun GesturePanel(
    modifier: Modifier = Modifier,
    onRoll: (TicketColour, Float) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var greenStep by remember { mutableIntStateOf(2) }
    var redStep by remember { mutableIntStateOf(2) }

    Row(
        modifier = modifier
            .background(Panel.copy(alpha = 0.96f), RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GestureTrack(
            colour = Green,
            selectedStep = greenStep,
            accessibilityLabel = "Green ticket control",
            onStepChanged = {
                greenStep = it
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onReleased = { step ->
                greenStep = 2
                onRoll(TicketColour.GREEN, greenProbabilitySteps[step])
            },
        )
        GestureTrack(
            colour = Red,
            selectedStep = redStep,
            accessibilityLabel = "Red ticket control",
            onStepChanged = {
                redStep = it
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onReleased = { step ->
                redStep = 2
                onRoll(TicketColour.RED, redProbabilitySteps[step])
            },
        )
    }
}

@Composable
private fun GestureTrack(
    colour: Color,
    selectedStep: Int,
    accessibilityLabel: String,
    onStepChanged: (Int) -> Unit,
    onReleased: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val stepDistancePx = with(density) { 28.dp.toPx() }

    Box(
        modifier = Modifier
            .width(54.dp)
            .height(168.dp)
            .semantics { contentDescription = accessibilityLabel }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val startY = down.position.y
                    var currentStep = 2
                    var pressed = true

                    while (pressed) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null) {
                            pressed = false
                        } else {
                            val verticalTravel = startY - change.position.y
                            val nextStep = (2 + (verticalTravel / stepDistancePx).roundToInt())
                                .coerceIn(0, redProbabilitySteps.lastIndex)
                            if (nextStep != currentStep) {
                                currentStep = nextStep
                                onStepChanged(currentStep)
                            }
                            change.consume()
                            pressed = change.pressed
                        }
                    }
                    onReleased(currentStep)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centreX = size.width / 2f
            val spacing = size.height / 6f
            val top = spacing
            drawLine(
                color = Muted.copy(alpha = 0.34f),
                start = Offset(centreX, top),
                end = Offset(centreX, top + spacing * 4),
                strokeWidth = 2.dp.toPx(),
            )
            repeat(5) { visualIndex ->
                val probabilityIndex = 4 - visualIndex
                val isSelected = probabilityIndex == selectedStep
                drawCircle(
                    color = if (isSelected) colour else Muted.copy(alpha = 0.58f),
                    radius = if (isSelected) 7.dp.toPx() else 3.5.dp.toPx(),
                    center = Offset(centreX, top + spacing * visualIndex),
                )
                if (isSelected) {
                    drawCircle(
                        color = colour.copy(alpha = 0.22f),
                        radius = 14.dp.toPx(),
                        center = Offset(centreX, top + spacing * visualIndex),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(ticket: TicketColour?, onReset: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val resultScale = remember(ticket) { Animatable(0.82f) }
    val background = when (ticket) {
        TicketColour.GREEN -> Green
        TicketColour.RED -> Red
        null -> Background
    }
    val foreground = when (ticket) {
        TicketColour.GREEN -> GreenDark
        TicketColour.RED -> RedDark
        null -> Cream
    }

    androidx.compose.runtime.LaunchedEffect(ticket) {
        if (ticket == null) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(80)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        resultScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .clickable(onClick = onReset)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = resultScale.value
                scaleY = resultScale.value
                alpha = resultScale.value.coerceIn(0f, 1f)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (ticket == null) {
                Text(
                    text = "×",
                    color = Muted,
                    fontSize = 116.sp,
                    fontWeight = FontWeight.Light,
                )
                Text(
                    text = "NO TICKET",
                    color = foreground,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            } else {
                AwardedTicket(ticket = ticket, ink = foreground)
            }
            Spacer(Modifier.height(34.dp))
            Text(
                text = "TAP TO RESET",
                color = foreground.copy(alpha = 0.62f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
        }
    }
}

@Composable
private fun AwardedTicket(ticket: TicketColour, ink: Color) {
    val title = if (ticket == TicketColour.GREEN) "GREEN" else "RED"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawTicket(Cream, ink)
            val dividerX = size.width * 0.76f
            drawLine(
                color = ink.copy(alpha = 0.38f),
                start = Offset(dividerX, 24.dp.toPx()),
                end = Offset(dividerX, size.height - 24.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
            )
            repeat(5) { index ->
                drawCircle(
                    color = ink.copy(alpha = 0.65f),
                    radius = 4.dp.toPx(),
                    center = Offset(
                        x = size.width * 0.88f,
                        y = size.height * 0.30f + index * 22.dp.toPx(),
                    ),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 34.dp, end = 100.dp),
        ) {
            Text(
                text = "YOU GOT A",
                color = ink.copy(alpha = 0.72f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
            )
            Text(
                text = title,
                color = ink,
                fontSize = 49.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "TICKET!",
                color = ink,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
