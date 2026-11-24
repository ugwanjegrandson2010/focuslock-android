package com.focuslock.app.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuslock.app.FocusLockApp
import com.focuslock.app.domain.ActiveState
import com.focuslock.app.domain.SessionEvent
import com.focuslock.app.domain.ScreensaverStyle
import kotlinx.coroutines.delay

class FocusActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContent {
            FocusLockTheme {
                FocusScreen(onExit = { finish() })
            }
        }
    }

    override fun onBackPressed() {
        // No bypass while a focus session is active.
        if ((application as FocusLockApp).container.sessionManager.isActiveNow()) return
        super.onBackPressed()
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }
}

@Composable
private fun FocusScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FocusActivity
    val container = (context.applicationContext as FocusLockApp).container
    val state by container.sessionManager.state.collectAsStateWithLifecycle(ActiveState())
    var showPin by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (container.blocker.isFullMode()) {
            try { activity.startLockTask() } catch (e: Exception) {}
        }
        container.sessionManager.events.collect { event ->
            when (event) {
                is SessionEvent.Finished -> {
                    try { activity.stopLockTask() } catch (e: Exception) {}
                    container.sessionManager.clearCurrent()
                    onExit()
                }
                else -> Unit
            }
        }
    }

    val style = runCatching {
        ScreensaverStyle.valueOf(state.session?.screensaverStyle ?: "GRADIENT")
    }.getOrDefault(ScreensaverStyle.GRADIENT)

    Box(Modifier.fillMaxSize()) {
        Screensaver(style)
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                state.session?.name ?: "Focus session",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatDuration(state.remainingMs),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
                Text(
                    if (state.isFullMode) "Full mode · Apps blocked" else "Standard mode · Apps blocked",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = { showPin = true }) {
                    Text("End session with PIN", color = Color.White.copy(alpha = 0.7f))
                }
                Text(
                    "Blocked attempts: ${state.blockedAttempts}",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showPin) {
        AlertDialog(
            onDismissRequest = { showPin = false },
            title = { Text("Emergency PIN") },
            text = {
                Column {
                    Text("Enter the PIN you set for this session.")
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it; pinError = false },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    if (pinError) Text("Incorrect PIN", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (container.sessionManager.abortWithPin(pin)) {
                        showPin = false
                    } else {
                        pinError = true
                    }
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showPin = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Screensaver(style: ScreensaverStyle) {
    when (style) {
        ScreensaverStyle.GRADIENT -> {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B1020), Color(0xFF1B2750), Color(0xFF101828))
                    )
                )
            )
        }
        ScreensaverStyle.BREATHING -> {
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breathing"
            )
            Box(
                Modifier.fillMaxSize().background(Color(0xFF070B16)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size((180 * scale).dp)) {
                    drawCircle(color = Color(0xFF3F5BB7).copy(alpha = 0.55f))
                }
            }
        }
        ScreensaverStyle.RAIN -> {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFF050A18), Color(0xFF0E2038)))
                )
            ) {
                var offset by remember { mutableStateOf(0f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(50)
                        offset = (offset + 2f) % 40f
                    }
                }
                Canvas(Modifier.fillMaxSize()) {
                    for (x in 0..size.width.toInt() step 40) {
                        val y = (x * 7 + offset * 3) % size.height
                        drawLine(
                            color = Color(0xFF7C9CFF).copy(alpha = 0.25f),
                            start = androidx.compose.ui.geometry.Offset(x.toFloat(), y),
                            end = androidx.compose.ui.geometry.Offset(x.toFloat(), y + 40f),
                            strokeWidth = 2f
                        )
                    }
                }
            }
        }
    }
}
