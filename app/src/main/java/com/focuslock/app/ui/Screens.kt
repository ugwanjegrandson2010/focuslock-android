package com.focuslock.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.focuslock.app.FocusLockApp
import com.focuslock.app.data.MusicProfileEntity
import com.focuslock.app.data.SessionStatus
import com.focuslock.app.domain.ActiveState
import com.focuslock.app.domain.MusicSource
import com.focuslock.app.domain.SessionConfig
import com.focuslock.app.domain.SessionEvent
import com.focuslock.app.domain.ScreensaverStyle
import com.focuslock.app.util.Apps
import com.focuslock.app.util.Permissions
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val container = (context.applicationContext as FocusLockApp).container
    val onboardingDone by container.prefs.onboardingDone.collectAsStateWithLifecycle(initialValue = true)

    LaunchedEffect(Unit) {
        container.sessionManager.events.collect { event ->
            if (event is SessionEvent.Finished) {
                navController.navigate("summary/${event.sessionId}") {
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone) "sessions" else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(onDone = {
                navController.navigate("sessions") { popUpTo("onboarding") { inclusive = true } }
            })
        }
        composable("sessions") { SessionsScreen(navController) }
        composable("create") { CreateSessionScreen(navController) }
        composable("history") { HistoryScreen(navController) }
        composable("settings") { SettingsScreen() }
        composable(
            "summary/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            SummaryScreen(backStack.arguments?.getLong("id") ?: -1L, navController)
        }
    }
}

@Composable
private fun MainScaffold(
    navController: androidx.navigation.NavHostController,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    val current by navController.currentBackStackEntryAsState()
    val route = current?.destination?.route
    Scaffold(
        bottomBar = {
            if (route in setOf("sessions", "history", "settings")) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == "sessions",
                        onClick = { navController.navigate("sessions") },
                        icon = { Icon(Icons.Default.PlayArrow, null) },
                        label = { Text("Sessions") }
                    )
                    NavigationBarItem(
                        selected = route == "history",
                        onClick = { navController.navigate("history") },
                        icon = { Icon(Icons.Default.History, null) },
                        label = { Text("History") }
                    )
                    NavigationBarItem(
                        selected = route == "settings",
                        onClick = { navController.navigate("settings") },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding -> content(padding) }
}

@Composable
private fun PermissionRow(
    label: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted) { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (granted) "Granted" else "Not granted — tap to open settings",
                style = MaterialTheme.typography.labelSmall,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun permissionRows(context: android.content.Context): List<Triple<String, String, () -> Unit>> = listOf(
    Triple(
        "Accessibility access",
        "Lets Focus Lock notice blocked apps and return you to your session.",
        { context.startActivity(Permissions.accessibilityIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    ),
    Triple(
        "Display over other apps",
        "Shows the blocking screen above other apps in Standard mode.",
        { context.startActivity(Permissions.overlayIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    ),
    Triple(
        "Notifications",
        "Shows the live countdown during your session.",
        { context.startActivity(Permissions.notificationIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    ),
    Triple(
        "Usage access",
        "Helps detect which app is in the foreground.",
        { context.startActivity(Permissions.usageAccessIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    ),
    Triple(
        "Ignore battery optimizations",
        "Keeps your session timer running reliably.",
        { context.startActivity(Permissions.batteryIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    )
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as FocusLockApp).container
    val scope = rememberCoroutineScope()
    val rows = permissionRows(context)
    val granted = listOf(
        Permissions.isAccessibilityEnabled(context),
        Permissions.canDrawOverlays(context),
        Permissions.notificationsEnabled(context),
        Permissions.hasUsageAccess(context),
        Permissions.ignoringBatteryOptimizations(context)
    )
    val fullMode = container.blocker.isFullMode()

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp)) {
            Text("Set up Focus Lock", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                if (fullMode) "Full mode is active. Apps can be truly blocked and the focus screen cannot be bypassed."
                else "Standard mode: blocked apps are dismissed and covered, but the app cannot forcibly stop them or prevent uninstall. Full mode requires one-time device-owner setup (see Settings).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rows.indices.toList()) { index ->
                    PermissionRow(rows[index].first, rows[index].second, granted[index], rows[index].third)
                }
            }
            Button(
                onClick = { scope.launch { container.prefs.setOnboardingDone(); onDone() } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continue") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val container = (context.applicationContext as FocusLockApp).container
    val active by container.sessionManager.state.collectAsStateWithLifecycle(ActiveState())
    val sessions by container.repository.allSessions.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    MainScaffold(navController) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            TopAppBar(title = { Text("Focus sessions") })
            if (active.isActive) {
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(active.session?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                        Text(
                            formatDuration(active.remainingMs),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (active.isFullMode) "Full mode · Apps blocked" else "Standard mode · Apps blocked",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            context.startActivity(Intent(context, FocusActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }) { Text("Open focus screen") }
                    }
                }
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sessions, key = { it.id }) { session ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            scope.launch {
                                val rules = container.repository.getBlockedApps(session.id)
                                val config = SessionConfig(
                                    name = session.name,
                                    durationMs = session.plannedDurationMs,
                                    blockedPackages = rules.filter { it.kind == "FULL" }.map { it.packageName },
                                    featureTargets = rules.filter { it.kind == "FEATURE" }.map { it.packageName },
                                    musicProfileId = session.musicProfileId,
                                    screensaverStyle = ScreensaverStyle.valueOf(session.screensaverStyle)
                                )
                                container.sessionManager.start(config)
                            }
                        }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(session.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${formatDuration(session.plannedDurationMs)} · ${session.status}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text("Repeat ▸")
                        }
                    }
                }
                if (sessions.isEmpty()) {
                    item { Text("No sessions yet. Create your first focus session.") }
                }
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            FloatingActionButton(
                onClick = { navController.navigate("create") },
                modifier = Modifier.padding(16.dp)
            ) { Icon(Icons.Default.Add, "New session") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionScreen(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val container = (context.applicationContext as FocusLockApp).container
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("25") }
    var startIn by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedApps by remember { mutableStateOf<Set<String>>(emptySet()) }
    var features by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showApps by remember { mutableStateOf(false) }
    var style by remember { mutableStateOf(ScreensaverStyle.GRADIENT) }
    var profileId by remember { mutableStateOf<Long?>(null) }
    var profileExpanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    val profiles by container.repository.musicProfiles.collectAsStateWithLifecycle(emptyList())
    val apps = remember { Apps.launchableApps(context) }
    val filteredApps = apps.filter {
        it.label.contains(search, true) || it.packageName.contains(search, true)
    }

    MainScaffold(navController) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            item {
                TopAppBar(title = { Text("New focus session") })
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Session name") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = minutes, onValueChange = { minutes = it },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = startIn, onValueChange = { startIn = it },
                    label = { Text("Start in (minutes, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Apps to block", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    label = { Text("Search apps") }, modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { showApps = !showApps }) {
                    Text(if (showApps) "Hide app list (${selectedApps.size} selected)" else "Show app list (${selectedApps.size} selected)")
                }
                if (showApps) {
                    Column(Modifier.height(240.dp)) {
                        LazyColumn {
                            items(filteredApps, key = { it.packageName }) { app ->
                                val checked = app.packageName in selectedApps
                                Row(
                                    Modifier.fillMaxWidth().toggleable(value = checked) {
                                        selectedApps = if (checked) selectedApps - app.packageName else selectedApps + app.packageName
                                    }.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = null)
                                    Spacer(Modifier.height(4.dp))
                                    Text(app.label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Short-form feeds", style = MaterialTheme.typography.titleMedium)
                FeatureChip("Instagram Reels", "com.instagram.android", features) { features = it }
                FeatureChip("YouTube Shorts", "com.google.android.youtube", features) { features = it }
                Text("Best-effort detection — may change when apps update.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("Focus music", style = MaterialTheme.typography.titleMedium)
                Box {
                    Button(onClick = { profileExpanded = true }) {
                        Text(profiles.firstOrNull { it.id == profileId }?.name ?: "None")
                    }
                    DropdownMenu(expanded = profileExpanded, onDismissRequest = { profileExpanded = false }) {
                        DropdownMenuItem(text = { Text("None") }, onClick = {
                            profileId = null; profileExpanded = false
                        })
                        profiles.forEach { profile ->
                            DropdownMenuItem(text = { Text(profile.name) }, onClick = {
                                profileId = profile.id; profileExpanded = false
                            })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Screensaver", style = MaterialTheme.typography.titleMedium)
                Row {
                    ScreensaverStyle.entries.forEach { option ->
                        FilterChip(
                            selected = style == option,
                            onClick = { style = option },
                            label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin, onValueChange = { pin = it },
                    label = { Text("Emergency PIN (optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                Row {
                    Button(onClick = {
                        val duration = (minutes.toLongOrNull() ?: 0) * 60_000L
                        val delay = (startIn.toLongOrNull() ?: 0) * 60_000L
                        if (name.isBlank()) { error = "Please enter a name"; return@Button }
                        if (duration <= 0) { error = "Duration must be positive"; return@Button }
                        val config = SessionConfig(
                            name = name.trim(),
                            durationMs = duration,
                            scheduledStartAt = if (delay > 0) System.currentTimeMillis() + delay else null,
                            blockedPackages = selectedApps.toList(),
                            featureTargets = features.toList(),
                            musicProfileId = profileId,
                            screensaverStyle = style,
                            pin = pin.ifBlank { null }
                        )
                        scope.launch {
                            try {
                                container.sessionManager.start(config)
                                navController.popBackStack()
                            } catch (e: Exception) {
                                error = e.message ?: "Could not save session"
                            }
                        }
                    }) { Text("Start now") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        val duration = (minutes.toLongOrNull() ?: 0) * 60_000L
                        val delay = (startIn.toLongOrNull() ?: 0) * 60_000L
                        if (name.isBlank() || duration <= 0 || delay <= 0) {
                            error = "Enter a name, duration and start time"; return@TextButton
                        }
                        scope.launch {
                            try {
                                container.sessionManager.start(
                                    SessionConfig(
                                        name = name.trim(), durationMs = duration,
                                        scheduledStartAt = System.currentTimeMillis() + delay,
                                        blockedPackages = selectedApps.toList(),
                                        featureTargets = features.toList(),
                                        musicProfileId = profileId,
                                        screensaverStyle = style,
                                        pin = pin.ifBlank { null }
                                    )
                                )
                                navController.popBackStack()
                            } catch (e: Exception) {
                                error = e.message ?: "Could not schedule session"
                            }
                        }
                    }) { Text("Schedule") }
                }
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun FeatureChip(label: String, pkg: String, current: Set<String>, update: (Set<String>) -> Unit) {
    val checked = pkg in current
    FilterChip(
        selected = checked,
        onClick = { update(if (checked) current - pkg else current + pkg) },
        label = { Text(label) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val container = (context.applicationContext as FocusLockApp).container
    val sessions by container.repository.completedSessions.collectAsStateWithLifecycle(emptyList())
    val count by container.repository.completedCount.collectAsStateWithLifecycle(0)
    var total by remember { mutableStateOf(0L) }
    var mostBlocked by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        total = container.repository.totalFocusMs()
        mostBlocked = container.repository.mostBlockedPackage()
    }
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    MainScaffold(navController) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            TopAppBar(title = { Text("History & stats") })
            Row(Modifier.fillMaxWidth()) {
                StatCard("Sessions", count.toString(), Modifier.weight(1f))
                StatCard("Focus time", formatDuration(total), Modifier.weight(1f))
            }
            mostBlocked?.let { Text("Most blocked: $it", style = MaterialTheme.typography.bodySmall) }
            LazyColumn(Modifier.weight(1f)) {
                items(sessions, key = { it.id }) { session ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(session.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${formatter.format(Date(session.completedAt ?: session.createdAt))} · " +
                                    formatDuration(session.actualDurationMs) + " · " +
                                    if (session.abortedByPin) "Aborted" else "Completed",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (sessions.isEmpty()) item { Text("No completed sessions yet.") }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier.padding(end = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as FocusLockApp).container
    val scope = rememberCoroutineScope()
    val rows = permissionRows(context)
    val granted = listOf(
        Permissions.isAccessibilityEnabled(context),
        Permissions.canDrawOverlays(context),
        Permissions.notificationsEnabled(context),
        Permissions.hasUsageAccess(context),
        Permissions.ignoringBatteryOptimizations(context)
    )
    var markers by remember { mutableStateOf("") }
    var profileName by remember { mutableStateOf("") }
    var profileUrl by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { markers = container.prefs.markers.first().joinToString("\n") }

    Surface(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.padding(16.dp)) {
            item {
                TopAppBar(title = { Text("Settings") })
                Text(
                    if (container.blocker.isFullMode()) "Full mode active" else "Standard mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
                rows.indices.forEach { index ->
                    PermissionRow(rows[index].first, rows[index].second, granted[index], rows[index].third)
                }
                Spacer(Modifier.height(16.dp))
                Text("Detection markers", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = markers, onValueChange = { markers = it },
                    label = { Text("One marker per line") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3
                )
                TextButton(onClick = {
                    scope.launch {
                        container.prefs.setMarkers(markers.lines().map { it.trim() }.filter { it.isNotBlank() }.toSet())
                        message = "Markers saved"
                    }
                }) { Text("Save markers") }
                Spacer(Modifier.height(16.dp))
                Text("Streaming music profile", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = profileName, onValueChange = { profileName = it },
                    label = { Text("Profile name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = profileUrl, onValueChange = { profileUrl = it },
                    label = { Text("HTTP(S) audio URL") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = {
                    scope.launch {
                        if (profileName.isNotBlank() && profileUrl.startsWith("http")) {
                            container.repository.addProfile(
                                MusicProfileEntity(
                                    name = profileName.trim(), source = MusicSource.STREAMING.name,
                                    location = profileUrl.trim()
                                )
                            )
                            profileName = ""; profileUrl = ""; message = "Profile saved"
                        } else message = "Enter a name and an http(s) URL"
                    }
                }) { Text("Save profile") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    scope.launch {
                        container.repository.addProfile(
                            MusicProfileEntity(
                                name = "Bundled ambient", source = MusicSource.BUNDLED.name,
                                location = "audio/focus_ambient.wav"
                            )
                        )
                        message = "Bundled profile added"
                    }
                }) { Text("Add bundled ambient profile") }
                Spacer(Modifier.height(16.dp))
                Text("Full mode setup", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Install a signed build on a device with no accounts, then run:\n" +
                        "adb shell dpm set-device-owner com.focuslock.app/.blocking.DeviceAdminReceiver\n" +
                        "To remove:\n" +
                        "adb shell dpm remove-active-admin com.focuslock.app/.blocking.DeviceAdminReceiver",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text("All session data stays on your device. No analytics, no accounts, no tracking.",
                    style = MaterialTheme.typography.bodySmall)
                Text(message, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(id: Long, navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val container = (context.applicationContext as FocusLockApp).container
    var session by remember { mutableStateOf<com.focuslock.app.data.SessionEntity?>(null) }
    var blocks by remember { mutableStateOf(0) }
    LaunchedEffect(id) {
        session = container.repository.getSession(id)
        blocks = container.repository.blockCount(id)
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(24.dp)) {
            TopAppBar(title = { Text("Session complete") })
            val s = session
            if (s == null) {
                CircularProgressIndicator()
            } else {
                Text(s.name, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Text("Status: ${if (s.status == SessionStatus.ABORTED) "Aborted" else "Completed"}")
                Text("Planned: ${formatDuration(s.plannedDurationMs)}")
                Text("Actual: ${formatDuration(s.actualDurationMs)}")
                Text("Blocked attempts: $blocks")
                Spacer(Modifier.height(24.dp))
                Button(onClick = { navController.navigate("sessions") { popUpTo("sessions") { inclusive = true } } }) {
                    Text("Done")
                }
            }
        }
    }
}
