package com.focuslock.app.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuslock.app.FocusLockApp
import com.focuslock.app.domain.ActiveState
import com.focuslock.app.domain.SessionEvent
import com.focuslock.app.domain.ScreensaverStyle
import com.focuslock.app.ui.design.CosmicBackground
import com.focuslock.app.ui.design.DesignTokens.Motion
import com.focuslock.app.ui.design.DesignTokens.Palette
import com.focuslock.app.ui.design.DesignTokens.Space
import com.focuslock.app.ui.design.DesignTokens.Type
import com.focuslock.app.ui.design.EyebrowText
import com.focuslock.app.ui.design.NumericDisplay

class FocusActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContent { FocusLockTheme { FocusScreen(onExit = { finish() }) } }
    }

    override fun onBackPressed() {
        if ((application as FocusLockApp).container.sessionManager.isActiveNow()) return
        super.onBackPressed()
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }
}

@Composable
private fun FocusScreen(onExit: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
        ScreensaverVisual(style)
        Column(
            Modifier.fillMaxSize().padding(Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EyebrowText(state.session?.name ?: "Focus session")
                Spacer(Modifier.height(Space.sm))
                Text(
                    if (state.isFullMode) "Apps are disabled until the timer ends"
                    else "Apps are blocked until the timer ends",
                    style = TextStyle(fontSize = Type.Caption, color = Palette.TextSecondary)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NumericDisplay(formatDuration(state.remainingMs))
                Spacer(Modifier.height(Space.sm))
                Text(
                    "Blocked attempts: ${state.blockedAttempts}",
                    style = TextStyle(fontSize = Type.Caption, color = Palette.TextSecondary)
                )
            }

            TextButton(onClick = { showPin = true }) {
                Text("End session with PIN", color = Palette.TextSecondary)
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
                    if (pinError) {
                        Text("Incorrect PIN", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (container.sessionManager.abortWithPin(pin)) showPin = false else pinError = true
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showPin = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ScreensaverVisual(style: ScreensaverStyle) {
    CosmicBackground(ambient = false)
    val pulse by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.ambientMs),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient pulse"
    )
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension * 0.18f
        val primary = when (style) {
            ScreensaverStyle.BREATHING -> Palette.Accent
            ScreensaverStyle.RAIN -> Palette.Success
            else -> Palette.AccentSoft
        }
        drawCircle(
            color = primary.copy(alpha = 0.10f * pulse),
            radius = base * 3.2f * pulse,
            center = center
        )
        drawCircle(
            color = primary.copy(alpha = 0.16f),
            radius = base * 2.1f,
            center = center
        )
        drawCircle(
            color = Palette.Hairline.copy(alpha = 0.8f),
            radius = base * 1.35f,
            center = center
        )
        drawCircle(
            color = primary.copy(alpha = 0.55f),
            radius = base * 0.82f * pulse,
            center = center
        )
    }
}
