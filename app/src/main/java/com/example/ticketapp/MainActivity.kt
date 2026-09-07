package com.example.ticketapp

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Size
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
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
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
    data class Rolling(
        val ticket: TicketColour?,
        val attemptedColour: TicketColour,
    ) : AppScreen
    data class Result(
        val ticket: TicketColour?,
        val attemptedColour: TicketColour,
    ) : AppScreen
}

class MainActivity : ComponentActivity() {
    private lateinit var soundPool: SoundPool
    private var greenSound = 0
    private var redSound = 0
    private var greenMissSound = 0
    private var redMissSound = 0
    private var drumrollSound = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        greenSound = soundPool.load(this, R.raw.green_win, 1)
        redSound = soundPool.load(this, R.raw.red_win, 1)
        greenMissSound = soundPool.load(this, R.raw.green_miss, 1)
        redMissSound = soundPool.load(this, R.raw.red_miss, 1)
        drumrollSound = soundPool.load(this, R.raw.drumroll, 1)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        enterImmersiveMode()

        setContent {
            TicketAppTheme(dynamicColor = false) {
                TicketApp(
                    onPlayDrumroll = ::playDrumroll,
                    onPlayOutcomeSound = ::playOutcomeSound,
                )
            }
        }
    }

    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
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

    private fun playOutcomeSound(ticket: TicketColour?, attemptedColour: TicketColour) {
        val sound = when (ticket) {
            TicketColour.GREEN -> greenSound
            TicketColour.RED -> redSound
            null -> when (attemptedColour) {
                TicketColour.GREEN -> greenMissSound
                TicketColour.RED -> redMissSound
            }
        }
        soundPool.play(sound, 1f, 1f, 1, 0, 1f)
    }

    private fun playDrumroll() {
        soundPool.play(drumrollSound, 1f, 1f, 1, 0, 1f)
    }
}

@Composable
private fun TicketApp(
    onPlayDrumroll: () -> Unit,
    onPlayOutcomeSound: (TicketColour?, TicketColour) -> Unit,
) {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Ready) }

    when (val current = screen) {
        AppScreen.Ready -> ReadyScreen { colour, probability ->
            val won = Random.nextFloat() < probability
            val result = colour.takeIf { won }
            onPlayDrumroll()
            screen = AppScreen.Rolling(ticket = result, attemptedColour = colour)
        }

        is AppScreen.Rolling -> {
            androidx.compose.runtime.LaunchedEffect(current) {
                delay(950)
                onPlayOutcomeSound(current.ticket, current.attemptedColour)
                screen = AppScreen.Result(
                    ticket = current.ticket,
                    attemptedColour = current.attemptedColour,
                )
            }
            ReadyScreen(onRoll = { _, _ -> })
        }

        is AppScreen.Result -> ResultScreen(
            ticket = current.ticket,
            attemptedColour = current.attemptedColour,
        ) {
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
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
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
    val stepDistancePx = with(density) { 40.dp.toPx() }

    Box(
        modifier = Modifier
            .width(66.dp)
            .height(232.dp)
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
private fun ResultScreen(
    ticket: TicketColour?,
    attemptedColour: TicketColour,
    onReset: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val resultScale = remember(ticket) { Animatable(if (ticket == null) 0.88f else 0.68f) }
    val resultRotation = remember(ticket) {
        Animatable(
            when (ticket) {
                TicketColour.GREEN -> -7f
                TicketColour.RED -> 7f
                null -> 13f
            },
        )
    }
    val effectProgress = remember(ticket) { Animatable(0f) }
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
        launch {
            effectProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            )
        }
        launch {
            resultRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        launch {
            resultScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .clickable(onClick = onReset)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        ResultEffects(
            ticket = ticket,
            attemptedColour = attemptedColour,
            progress = effectProgress.value,
        )

        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = resultScale.value
                scaleY = resultScale.value
                alpha = resultScale.value.coerceIn(0f, 1f)
                rotationZ = resultRotation.value
                if (ticket == TicketColour.RED) {
                    translationX = sin(effectProgress.value * PI.toFloat() * 10f) *
                        (1f - effectProgress.value) * 13f
                }
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (ticket == null) {
                Text(
                    text = if (attemptedColour == TicketColour.GREEN) "☹" else "💨",
                    color = if (attemptedColour == TicketColour.GREEN) {
                        Green.copy(alpha = 0.72f)
                    } else {
                        Cream.copy(alpha = 0.82f)
                    },
                    fontSize = if (attemptedColour == TicketColour.GREEN) 104.sp else 92.sp,
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
private fun ResultEffects(
    ticket: TicketColour?,
    attemptedColour: TicketColour,
    progress: Float,
) {
    Canvas(Modifier.fillMaxSize()) {
        val centre = Offset(size.width / 2f, size.height * 0.46f)
        val fade = (1f - progress).coerceIn(0f, 1f)

        when (ticket) {
            TicketColour.GREEN -> {
                val colours = listOf(Cream, GreenDark, Color(0xFFFFD166), Color(0xFF6EC5FF))
                repeat(46) { index ->
                    val angle = Math.toRadians(index * 137.5).toFloat()
                    val distance = (90f + (index % 8) * 31f) * progress
                    val gravity = progress * progress * (80f + (index % 5) * 13f)
                    val particleCentre = Offset(
                        x = centre.x + cos(angle) * distance,
                        y = centre.y + sin(angle) * distance + gravity,
                    )
                    drawRect(
                        color = colours[index % colours.size].copy(alpha = fade),
                        topLeft = Offset(particleCentre.x - 4f, particleCentre.y - 8f),
                        size = Size(width = 8f, height = 16f),
                    )
                }
            }

            TicketColour.RED -> {
                repeat(22) { index ->
                    val angle = (index / 22f) * PI.toFloat() * 2f
                    val inner = 150f + progress * 32f
                    val outer = inner + 48f * fade
                    drawLine(
                        color = if (index % 2 == 0) Cream.copy(alpha = fade * 0.72f)
                        else RedDark.copy(alpha = fade * 0.72f),
                        start = Offset(
                            centre.x + cos(angle) * inner,
                            centre.y + sin(angle) * inner,
                        ),
                        end = Offset(
                            centre.x + cos(angle) * outer,
                            centre.y + sin(angle) * outer,
                        ),
                        strokeWidth = if (index % 2 == 0) 7f else 4f,
                    )
                }
            }

            null -> {
                if (attemptedColour == TicketColour.GREEN) {
                    repeat(14) { index ->
                        val spread = (index - 6.5f) * 24f
                        drawCircle(
                            color = Green.copy(alpha = fade * 0.26f),
                            radius = 5f + (index % 3) * 3f,
                            center = Offset(
                                centre.x + spread,
                                centre.y + 72f + progress * (55f + (index % 4) * 18f),
                            ),
                        )
                    }
                } else {
                    repeat(12) { index ->
                        val angle = (index / 12f) * PI.toFloat() * 2f
                        val distance = 55f + progress * (65f + (index % 3) * 18f)
                        drawCircle(
                            color = Cream.copy(alpha = fade * 0.30f),
                            radius = 8f + progress * 14f,
                            center = Offset(
                                centre.x + cos(angle) * distance + progress * 28f,
                                centre.y + sin(angle) * distance - progress * 24f,
                            ),
                        )
                    }
                }
            }
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
