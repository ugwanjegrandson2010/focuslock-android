package com.focuslock.app.ui

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
import com.focuslock.app.ui.design.CosmicBackground
import com.focuslock.app.ui.design.DisplayHeading
import com.focuslock.app.ui.design.EyebrowText
import com.focuslock.app.ui.design.GlassPanel
import com.focuslock.app.ui.design.HairlineRule
import com.focuslock.app.ui.design.NumericDisplay
import com.focuslock.app.ui.design.PrimaryAction
import com.focuslock.app.ui.design.RowsLabel
import com.focuslock.app.ui.design.StatBlock
import com.focuslock.app.ui.design.StatusPill
import com.focuslock.app.ui.design.DesignTokens.Palette
import com.focuslock.app.ui.design.DesignTokens.Space
import com.focuslock.app.ui.design.DesignTokens.Type
import com.focuslock.app.util.Apps
import com.focuslock.app.util.Permissions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
                navController.navigate("summary/${event.sessionId}") { launchSingleTop = true }
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
        ) { backStack -> SummaryScreen(backStack.arguments?.getLong("id") ?: -1L, navController) }
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
        containerColor = Color.Transparent,
        bottomBar = {
            if (route in setOf("sessions", "history", "settings")) {
                NavigationBar(
                    containerColor = Palette.Void.copy(alpha = 0.9f),
                    contentColor = Palette.TextPrimary
                ) {
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
            .padding(vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = TextStyle(fontSize = Type.Body, color = Palette.TextPrimary))
            Text(
                description,
                style = TextStyle(fontSize = Type.Caption, color = Palette.TextSecondary)
            )
            Text(
                if (granted) "Granted" else "Not granted. Tap to open settings",
                style = TextStyle(
                    fontSize = Type.Caption,
                    color = if (granted) Palette.Success else Palette.Danger
                )
            )
        }
        StatusPill(if (granted) "On" else "Off", active = granted)
    }
}

@Composable
private fun permissionRows(context: android.content.Context) = listOf(
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
    val isFullMode = container.blocker.isFullMode()

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        Column(Modifier.padding(Space.lg).fillMaxSize()) {
            Spacer(Modifier.height(Space.xl))
            EyebrowText("Focus Lock")
            DisplayHeading("Claim your attention.", Modifier.padding(top = Space.sm))
            Text(
                if (isFullMode) {
                    "Full mode is active. Apps can be truly blocked and the focus screen cannot be bypassed."
                } else {
                    "Standard mode dismisses blocked apps and covers the screen. It cannot forcibly stop apps or prevent uninstall. Full mode needs one-time device setup, described in Settings."
                },
                style = TextStyle(fontSize = Type.Body, color = Palette.TextSecondary),
                modifier = Modifier.padding(top = Space.md)
            )
            Spacer(Modifier.height(Space.lg))
            LazyColumn(Modifier.weight(1f)) {
                items(rows.indices.toList()) { index ->
                    PermissionRow(rows[index].first, rows[index].second, granted[index], rows[index].third)
                    HairlineRule()
                }
            }
            PrimaryAction(
                text = "Continue",
                onClick = { scope.launch { container.prefs.setOnboardingDone(); onDone() } },
                modifier = Modifier.fillMaxWidth()
            )
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

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        MainScaffold(navController) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = Space.lg)) {
                TopAppBar(
                    title = { EyebrowText("Focus sessions") },
                    actions = {
                        TextButton(onClick = { navController.navigate("create") }) {
                            Text("Add Session", color = Palette.AccentSoft)
                        }
                    }
                )

                if (active.isActive) {
                    GlassPanel(Modifier.fillMaxWidth().padding(top = Space.md)) {
                        Column(Modifier.padding(Space.lg)) {
                            EyebrowText("In progress")
                            Text(
                                active.session?.name.orEmpty(),
                                style = TextStyle(fontSize = Type.Headline, color = Palette.TextPrimary)
                            )
                            NumericDisplay(formatDuration(active.remainingMs))
                            Row {
                                StatusPill(if (active.isFullMode) "Full mode" else "Standard mode")
                                Spacer(Modifier.width(Space.sm))
                                StatusPill("Apps blocked", active = false)
                            }
                            Spacer(Modifier.height(Space.md))
                            PrimaryAction(
                                text = "Open focus screen",
                                onClick = {
                                    context.startActivity(
                                        Intent(context, FocusActivity::class.java)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Column(Modifier.padding(top = Space.md)) {
                        EyebrowText("Your focus space")
                        DisplayHeading(nameFromHour())
                        Text(
                            "Name a session, choose what to block, then let the timer carry you.",
                            style = TextStyle(fontSize = Type.Body, color = Palette.TextSecondary)
                        )
                        Spacer(Modifier.height(Space.md))
                        PrimaryAction(
                            text = "Add Session",
                            onClick = { navController.navigate("create") }
                        )
                    }
                }

                Spacer(Modifier.height(Space.lg))
                EyebrowText("Saved sessions")
                LazyColumn(Modifier.weight(1f)) {
                    items(sessions, key = { it.id }) { session ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val rules = container.repository.getBlockedApps(session.id)
                                        container.sessionManager.start(
                                            SessionConfig(
                                                name = session.name,
                                                durationMs = session.plannedDurationMs,
                                                blockedPackages = rules.filter { it.kind == "FULL" }.map { it.packageName },
                                                featureTargets = rules.filter { it.kind == "FEATURE" }.map { it.packageName },
                                                musicProfileId = session.musicProfileId,
                                                screensaverStyle = runCatching {
                                                    ScreensaverStyle.valueOf(session.screensaverStyle)
                                                }.getOrDefault(ScreensaverStyle.GRADIENT)
                                            )
                                        )
                                    }
                                }
                                .padding(vertical = Space.md)
                        ) {
                            RowsLabel(
                                text = session.name,
                                trailing = "${formatDuration(session.plannedDurationMs)}  ${session.status.lowercase()}"
                            )
                            HairlineRule()
                        }
                    }
                    if (sessions.isEmpty()) {
                        item {
                            Text(
                                "No sessions saved yet. Your first one is one tap away.",
                                style = TextStyle(fontSize = Type.Body, color = Palette.TextSecondary),
                                modifier = Modifier.padding(vertical = Space.lg)
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { navController.navigate("create") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Space.lg),
            containerColor = Palette.Accent,
            contentColor = Palette.Void
        ) { Icon(Icons.Default.Add, "Add session") }
    }
}

@Composable
private fun nameFromHour(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning."
        in 12..17 -> "Good afternoon."
        in 18..22 -> "Good evening."
        else -> "Still awake."
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

    fun buildConfig(scheduledOnly: Boolean): SessionConfig? {
        val duration = (minutes.toLongOrNull() ?: 0) * 60_000L
        val delay = (startIn.toLongOrNull() ?: 0) * 60_000L
        if (name.isBlank()) { error = "Enter a session name"; return null }
        if (duration <= 0) { error = "Duration must be positive"; return null }
        if (scheduledOnly && delay <= 0) { error = "Enter a start delay in minutes"; return null }
        return SessionConfig(
            name = name.trim(),
            durationMs = duration,
            scheduledStartAt = if (delay > 0) System.currentTimeMillis() + delay else null,
            blockedPackages = selectedApps.toList(),
            featureTargets = features.toList(),
            musicProfileId = profileId,
            screensaverStyle = style,
            pin = pin.ifBlank { null }
        )
    }

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        MainScaffold(navController) { padding ->
            LazyColumn(
                Modifier.padding(padding).fillMaxSize().padding(horizontal = Space.lg)
            ) {
                item {
                    TopAppBar(title = { EyebrowText("New focus session") })
                    DisplayHeading("Set the terms.", Modifier.padding(bottom = Space.lg))

                    GlassPanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(Space.lg)) {
                            OutlinedTextField(
                                value = name, onValueChange = { name = it },
                                label = { Text("Session name") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true
                            )
                            Spacer(Modifier.height(Space.md))
                            OutlinedTextField(
                                value = minutes, onValueChange = { minutes = it },
                                label = { Text("Duration in minutes") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(Space.md))
                            OutlinedTextField(
                                value = startIn, onValueChange = { startIn = it },
                                label = { Text("Start in minutes (optional)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(Space.lg))
                    EyebrowText("Apps to block")
                    GlassPanel(Modifier.fillMaxWidth().padding(top = Space.sm)) {
                        Column(Modifier.padding(Space.lg)) {
                            OutlinedTextField(
                                value = search, onValueChange = { search = it },
                                label = { Text("Search installed apps") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(onClick = { showApps = !showApps }) {
                                Text(if (showApps) "Hide list (${selectedApps.size} selected)" else "Show list (${selectedApps.size} selected)")
                            }
                            if (showApps) {
                                Column(Modifier.height(220.dp)) {
                                    LazyColumn {
                                        items(filteredApps, key = { it.packageName }) { app ->
                                            val checked = app.packageName in selectedApps
                                            Row(
                                                Modifier.fillMaxWidth().toggleable(value = checked) {
                                                    selectedApps = if (checked) selectedApps - app.packageName else selectedApps + app.packageName
                                                }.padding(Space.sm),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(checked = checked, onCheckedChange = null)
                                                Spacer(Modifier.width(Space.sm))
                                                Text(app.label)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(Space.lg))
                    EyebrowText("Short-form feeds")
                    Row {
                        FeatureChip("Instagram Reels", "com.instagram.android", features) { features = it }
                        Spacer(Modifier.width(Space.sm))
                        FeatureChip("YouTube Shorts", "com.google.android.youtube", features) { features = it }
                    }
                    Text(
                        "Detection is best effort and may change when apps update.",
                        style = TextStyle(fontSize = Type.Caption, color = Palette.TextSecondary)
                    )

                    Spacer(Modifier.height(Space.lg))
                    EyebrowText("Focus music")
                    Box {
                        TextButton(onClick = { profileExpanded = true }) {
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

                    Spacer(Modifier.height(Space.lg))
                    EyebrowText("Screensaver")
                    Row {
                        ScreensaverStyle.entries.forEach { option ->
                            FilterChip(
                                selected = style == option,
                                onClick = { style = option },
                                label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Palette.Accent,
                                    selectedLabelColor = Palette.Void
                                ),
                                modifier = Modifier.padding(end = Space.sm)
                            )
                        }
                    }

                    Spacer(Modifier.height(Space.lg))
                    EyebrowText("Emergency PIN")
                    OutlinedTextField(
                        value = pin, onValueChange = { pin = it },
                        label = { Text("Optional, 4 to 6 digits") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    error?.let {
                        Spacer(Modifier.height(Space.sm))
                        Text(it, color = Palette.Danger, style = TextStyle(fontSize = Type.Caption))
                    }

                    Spacer(Modifier.height(Space.lg))
                    Row {
                        PrimaryAction(
                            text = "Start now",
                            onClick = {
                                buildConfig(false)?.let { config ->
                                    scope.launch {
                                        try {
                                            container.sessionManager.start(config)
                                            navController.popBackStack()
                                        } catch (e: Exception) {
                                            error = e.message ?: "Could not start session"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(Space.md))
                        TextButton(onClick = {
                            buildConfig(true)?.let { config ->
                                scope.launch {
                                    try {
                                        container.sessionManager.start(config)
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        error = e.message ?: "Could not schedule session"
                                    }
                                }
                            }
                        }) { Text("Schedule") }
                    }
                    Spacer(Modifier.height(Space.xxl))
                }
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
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Palette.Accent,
            selectedLabelColor = Palette.Void
        )
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

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        MainScaffold(navController) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = Space.lg)) {
                TopAppBar(title = { EyebrowText("History and stats") })
                DisplayHeading("The evidence.", Modifier.padding(bottom = Space.lg))
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Space.lg)) {
                        StatBlock("Total focus time", formatDuration(total), leading = true)
                        HairlineRule()
                        Row {
                            StatBlock("Sessions", count.toString(), modifier = Modifier.weight(1f))
                            StatBlock("Most blocked", mostBlocked ?: "None", modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(Space.lg))
                EyebrowText("Completed sessions")
                LazyColumn(Modifier.weight(1f)) {
                    items(sessions, key = { it.id }) { session ->
                        Column(Modifier.fillMaxWidth().padding(vertical = Space.md)) {
                            RowsLabel(
                                text = session.name,
                                trailing = formatDuration(session.actualDurationMs)
                            )
                            Text(
                                "${formatter.format(Date(session.completedAt ?: session.createdAt))}  " +
                                    if (session.abortedByPin) "Aborted" else "Completed",
                                style = TextStyle(fontSize = Type.Caption, color = Palette.TextSecondary)
                            )
                            HairlineRule()
                        }
                    }
                    if (sessions.isEmpty()) {
                        item {
                            Text(
                                "No completed sessions yet.",
                                style = TextStyle(fontSize = Type.Body, color = Palette.TextSecondary),
                                modifier = Modifier.padding(vertical = Space.lg)
                            )
                        }
                    }
                }
            }
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

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = Space.lg)) {
            item {
                TopAppBar(title = { EyebrowText("Settings") })
                DisplayHeading("Control the room.", Modifier.padding(bottom = Space.lg))
                StatusPill(if (container.blocker.isFullMode()) "Full mode active" else "Standard mode")
                Spacer(Modifier.height(Space.lg))

                EyebrowText("Permissions")
                rows.indices.forEach { index ->
                    PermissionRow(rows[index].first, rows[index].second, granted[index], rows[index].third)
                    HairlineRule()
                }

                Spacer(Modifier.height(Space.lg))
                EyebrowText("Detection markers")
                OutlinedTextField(
                    value = markers, onValueChange = { markers = it },
                    label = { Text("One marker per line") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3
                )
                TextButton(onClick = {
                    scope.launch {
                        container.prefs.setMarkers(
                            markers.lines().map { it.trim() }.filter { it.isNotBlank() }.toSet()
                        )
                        message = "Markers saved"
                    }
                }) { Text("Save markers") }

                Spacer(Modifier.height(Space.lg))
                EyebrowText("Streaming music profile")
                OutlinedTextField(
                    value = profileName, onValueChange = { profileName = it },
                    label = { Text("Profile name") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = profileUrl, onValueChange = { profileUrl = it },
                    label = { Text("HTTP or HTTPS audio URL") }, modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = {
                    scope.launch {
                        if (profileName.isNotBlank() && profileUrl.startsWith("http")) {
                            container.repository.addProfile(
                                MusicProfileEntity(
                                    name = profileName.trim(),
                                    source = MusicSource.STREAMING.name,
                                    location = profileUrl.trim()
                                )
                            )
                            profileName = ""; profileUrl = ""; message = "Profile saved"
                        } else {
                            message = "Enter a name and an http(s) URL"
                        }
                    }
                }) { Text("Save profile") }
                TextButton(onClick = {
                    scope.launch {
                        container.repository.addProfile(
                            MusicProfileEntity(
                                name = "Bundled ambient",
                                source = MusicSource.BUNDLED.name,
                                location = "audio/focus_ambient.wav"
                            )
                        )
                        message = "Bundled profile added"
                    }
                }) { Text("Add bundled ambient profile") }

                Spacer(Modifier.height(Space.lg))
                EyebrowText("Full mode setup")
                Text(
                    "Install a signed build on a device with no accounts, then run:\n" +
                        "adb shell dpm set-device-owner com.focuslock.app/.blocking.DeviceAdminReceiver\n" +
                        "To remove:\n" +
                        "adb shell dpm remove-active-admin com.focuslock.app/.blocking.DeviceAdminReceiver",
                    style = TextStyle(fontSize = Type.Caption, color = Palette.TextSecondary)
                )

                Spacer(Modifier.height(Space.lg))
                EyebrowText("About")
                Text(
                    "All session data stays on your device. No analytics, no accounts, no tracking.",
                    style = TextStyle(fontSize = Type.Caption, color = Palette.TextSecondary)
                )
                Text(message, color = Palette.Success, style = TextStyle(fontSize = Type.Caption))
                Spacer(Modifier.height(Space.xxl))
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

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        Column(Modifier.padding(Space.lg).fillMaxSize()) {
            TopAppBar(title = { EyebrowText("Session complete") })
            val s = session
            if (s == null) {
                CircularProgressIndicator()
            } else {
                DisplayHeading(s.name)
                Spacer(Modifier.height(Space.md))
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Space.lg)) {
                        StatusPill(if (s.status == SessionStatus.ABORTED) "Aborted" else "Completed")
                        Spacer(Modifier.height(Space.md))
                        StatBlock("Planned", formatDuration(s.plannedDurationMs))
                        HairlineRule()
                        StatBlock("Actual", formatDuration(s.actualDurationMs), leading = true)
                        HairlineRule()
                        StatBlock("Blocked attempts", blocks.toString())
                    }
                }
                Spacer(Modifier.height(Space.lg))
                PrimaryAction(
                    text = "Done",
                    onClick = { navController.navigate("sessions") { popUpTo("sessions") { inclusive = true } } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
