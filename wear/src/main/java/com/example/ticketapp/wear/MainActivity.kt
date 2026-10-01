package com.example.ticketapp.wear

import com.example.ticketapp.relay.Destination
import com.example.ticketapp.relay.TicketEvent
import com.example.ticketapp.relay.WatchTransport
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.ambient.AmbientModeSupport
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val SESSION_PREFS = "ticket_toss_watch"
private const val SESSION_ACTIVE = "session_active"

private val Green = Color(0xFF2ECC71)
private val GreenDark = Color(0xFF123D28)
private val Red = Color(0xFFFF4D58)
private val RedDark = Color(0xFF4B171C)
private val Ink = Color(0xFF090B0D)
private val Paper = Color(0xFFF6F0DF)

class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    private var isAmbient by mutableStateOf(false)
    private lateinit var ambientController: AmbientModeSupport.AmbientController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ambientController = AmbientModeSupport.attach(this)

        val preferences = getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
        setContent {
            TicketTossWatchApp(
                initiallyActive = preferences.getBoolean(SESSION_ACTIVE, false),
                isAmbient = isAmbient,
                onSessionChanged = { active ->
                    preferences.edit().putBoolean(SESSION_ACTIVE, active).apply()
                    if (!active) finishAndRemoveTask()
                },
            )
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback =
        object : AmbientModeSupport.AmbientCallback() {
            override fun onEnterAmbient(ambientDetails: Bundle) {
                isAmbient = true
            }

            override fun onExitAmbient() {
                isAmbient = false
            }
        }
}

private sealed interface WatchScreen {
    data object Welcome : WatchScreen
    data object Ready : WatchScreen
    data class Rolling(val colour: TicketColour) : WatchScreen
    data class Result(val outcome: TicketOutcome) : WatchScreen
    data object EndConfirm : WatchScreen
    data object Settings : WatchScreen
    data class Delivery(val text: String) : WatchScreen
}

@Composable
private fun TicketTossWatchApp(
    initiallyActive: Boolean,
    isAmbient: Boolean,
    onSessionChanged: (Boolean) -> Unit,
) {
    var screen: WatchScreen by remember {
        mutableStateOf(if (initiallyActive) WatchScreen.Ready else WatchScreen.Welcome)
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE) }
    var destination by remember { mutableStateOf(Destination.restore(prefs.getString("destination", null))) }
    var connection by remember { mutableStateOf("Checking phone…") }
    var activeEvent by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val audio = remember { WatchAudio(context) }
    val haptics = LocalHapticFeedback.current
    DisposableEffect(Unit) {
        val lifecycle = (context as LifecycleOwner).lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) audio.stop()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); audio.stop() }
    }
    LaunchedEffect(isAmbient) { audio.ambient = isAmbient; if (isAmbient) audio.stop() }
    LaunchedEffect(destination, screen is WatchScreen.Ready, isAmbient) {
        if (destination.pc && screen is WatchScreen.Ready && !isAmbient) {
            while (true) {
                connection = when (WatchTransport.request(context)) {
                    "PC_READY" -> "Phone connected · PC ready"
                    "PC_OFFLINE" -> "Phone connected · PC unavailable"
                    "PHONE_OFFLINE" -> "Phone unavailable"
                    else -> "Connection unavailable"
                }
                delay(10000)
            }
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Ink,
        ) {
            AnimatedContent(targetState = screen, label = "watch-screen") { current ->
                when (current) {
                    WatchScreen.Welcome -> WelcomeScreen {
                        onSessionChanged(true)
                        screen = WatchScreen.Ready
                    }

                    WatchScreen.Ready -> ReadyScreen(
                        isAmbient = isAmbient,
                        status = if (destination.pc) connection else "",
                        onSettings = { screen = WatchScreen.Settings },
                        onRoll = roll@{ colour, index ->
                            if (screen != WatchScreen.Ready) return@roll
                            val outcome = TicketRoll.roll(colour, index)
                            val mode = destination
                            val event = TicketEvent.create(colour.name, outcome.awarded, outcome.probability, "WATCH", mode)
                            activeEvent = event.id
                            screen = WatchScreen.Rolling(colour)
                            scope.launch {
                                if (mode.sound) audio.play(R.raw.drumroll)
                                if (mode.pc) launch {
                                    val result = WatchTransport.deliver(context, event)
                                    if (activeEvent != event.id) return@launch
                                    connection = if (result == "DELIVERED") "Delivered to PC" else "PC delivery failed"
                                    if (screen is WatchScreen.Delivery) screen = WatchScreen.Delivery(connection)
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                delay(1000)
                                if (mode.local) haptics.performHapticFeedback(
                                    if (outcome.awarded) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
                                screen = if (mode.local) WatchScreen.Result(outcome) else WatchScreen.Delivery("Sending to PC…")
                                audio.outcome(outcome, mode.sound)
                                if (activeEvent == event.id && screen !is WatchScreen.EndConfirm && screen !is WatchScreen.Settings) screen = WatchScreen.Ready
                            }
                        },
                        onEnd = { screen = WatchScreen.EndConfirm },
                    )

                    is WatchScreen.Rolling -> RollingScreen(current.colour)
                    is WatchScreen.Result -> ResultScreen(
                        outcome = current.outcome,
                        isAmbient = isAmbient,
                        deliveryStatus = if (destination.pc) connection else "",
                        onDismiss = { audio.stop(); screen = WatchScreen.Ready },
                    )

                    WatchScreen.Settings -> Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Destination.entries.forEach { mode ->
                            TextButton(modifier = Modifier.height(32.dp), onClick = {
                                destination = mode
                                prefs.edit().putString("destination", mode.storedName("WATCH")).apply()
                                screen = WatchScreen.Ready
                            }) { Text((if (mode == destination) "✓ " else "") + when (mode) {
                                Destination.LOCAL_ONLY -> "Watch only"
                                Destination.PC_ONLY -> "PC only"
                                Destination.LOCAL_AND_PC -> "Watch and PC"
                            }, fontSize = 12.sp) }
                        }
                        TextButton(modifier = Modifier.height(32.dp), onClick = { screen = WatchScreen.Ready }) { Text("BACK") }
                    }
                    is WatchScreen.Delivery -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(current.text, color = Paper, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                    WatchScreen.EndConfirm -> EndSessionScreen(
                        onKeepGoing = { screen = WatchScreen.Ready },
                        onEnd = { onSessionChanged(false) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen(onBegin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("TICKET", color = Paper, fontWeight = FontWeight.Black, fontSize = 25.sp)
        Text("TOSS", color = Green, fontWeight = FontWeight.Black, fontSize = 25.sp)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onBegin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Ink),
        ) {
            Text("BEGIN CLASS", fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Stays ready until you end the session",
            color = Paper.copy(alpha = 0.58f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReadyScreen(
    isAmbient: Boolean,
    onRoll: (TicketColour, Int) -> Unit,
    status: String,
    onSettings: () -> Unit,
    onEnd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (status.isBlank()) "TICKET TOSS" else status,
            modifier = Modifier.pointerInput(isAmbient) {
                if (!isAmbient) detectTapGestures(onLongPress = { onSettings() })
            },
            color = Paper.copy(alpha = if (isAmbient) 0.55f else 0.82f),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(6.dp))
        TicketGestureButton(
            colour = TicketColour.GREEN,
            enabled = !isAmbient,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onRoll = onRoll,
        )
        Spacer(Modifier.height(7.dp))
        TicketGestureButton(
            colour = TicketColour.RED,
            enabled = !isAmbient,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onRoll = onRoll,
        )
        Spacer(Modifier.height(5.dp))
        Button(
            onClick = onEnd,
            enabled = !isAmbient,
            modifier = Modifier.height(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Paper.copy(alpha = 0.55f),
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Paper.copy(alpha = 0.25f),
            ),
        ) {
            Text("END", fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TicketGestureButton(
    colour: TicketColour,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onRoll: (TicketColour, Int) -> Unit,
) {
    val currentRoll by rememberUpdatedState(onRoll)
    val haptics = LocalHapticFeedback.current
    val thresholdPx = with(LocalDensity.current) { 22.dp.toPx() }
    var selectedIndex by remember(colour) { mutableStateOf(TicketRoll.defaultIndex(colour)) }
    var pressed by remember { mutableStateOf(false) }
    val emphasis by animateFloatAsState(if (pressed) 1f else 0.78f, label = "press-emphasis")
    val base = if (colour == TicketColour.GREEN) GreenDark else RedDark
    val accent = if (colour == TicketColour.GREEN) Green else Red
    val label = if (colour == TicketColour.GREEN) "GREEN" else "RED"

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.42f)
            .background(base.copy(alpha = emphasis), RoundedCornerShape(28.dp))
            .pointerInput(colour, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val defaultIndex = TicketRoll.defaultIndex(colour)
                    selectedIndex = defaultIndex
                    pressed = true
                    var currentIndex = defaultIndex
                    var isPressed = true

                    while (isPressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val upwardTravel = down.position.y - change.position.y
                        val nextIndex = (defaultIndex + (upwardTravel / thresholdPx).roundToInt())
                            .coerceIn(0, 4)

                        if (nextIndex != currentIndex) {
                            currentIndex = nextIndex
                            selectedIndex = nextIndex
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        isPressed = change.pressed
                        change.consume()
                    }

                    pressed = false
                    currentRoll(colour, currentIndex)
                    selectedIndex = defaultIndex
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                color = accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                if (pressed) "release to toss" else "tap · slide · release",
                color = Paper.copy(alpha = if (pressed) 0.85f else 0.48f),
                fontSize = 9.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(top = 5.dp),
            ) {
                repeat(5) { index ->
                    Box(
                        Modifier
                            .size(if (index == selectedIndex) 6.dp else 3.dp)
                            .background(
                                if (index == selectedIndex) accent else Paper.copy(alpha = 0.25f),
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun RollingScreen(colour: TicketColour) {
    val accent = if (colour == TicketColour.GREEN) Green else Red
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("TOSSING…", color = accent, fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

@Composable
private fun ResultScreen(
    outcome: TicketOutcome,
    isAmbient: Boolean,
    deliveryStatus: String,
    onDismiss: () -> Unit,
) {
    val accent = if (outcome.colour == TicketColour.GREEN) Green else Red
    val background = if (outcome.awarded) accent else Color(0xFF272A2D)


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .pointerInput(isAmbient) {
                if (!isAmbient) {
                    awaitEachGesture {
                        awaitFirstDown()
                        onDismiss()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (deliveryStatus.isNotBlank()) Text(deliveryStatus,
                color = if (outcome.awarded) Ink else Paper, fontSize = 10.sp)
            Text(
                if (outcome.awarded) "TICKET!" else "NOPE",
                color = if (outcome.awarded) Ink else Paper,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                if (outcome.awarded) {
                    if (outcome.colour == TicketColour.GREEN) "★  ★  ★" else "☠  ☠  ☠"
                } else {
                    if (outcome.colour == TicketColour.GREEN) "aww…" else "phew!"
                },
                color = if (outcome.awarded) Ink.copy(alpha = 0.7f) else accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EndSessionScreen(
    onKeepGoing: () -> Unit,
    onEnd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "END CLASS?",
            color = Paper,
            fontWeight = FontWeight.Black,
            fontSize = 21.sp,
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onKeepGoing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Ink),
        ) {
            Text("KEEP GOING", fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
        Button(
            onClick = onEnd,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Red),
        ) {
            Text("END SESSION", fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}
