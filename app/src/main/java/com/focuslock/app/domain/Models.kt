package com.focuslock.app.domain

import com.focuslock.app.data.SessionEntity

enum class BlockKind { FULL, FEATURE }
enum class MusicSource { BUNDLED, LOCAL, STREAMING }
enum class ScreensaverStyle { GRADIENT, BREATHING, RAIN }

data class AppInfo(
    val packageName: String,
    val label: String
)

data class SessionConfig(
    val name: String,
    val durationMs: Long,
    val scheduledStartAt: Long? = null,
    val blockedPackages: List<String> = emptyList(),
    val featureTargets: List<String> = emptyList(),
    val musicProfileId: Long? = null,
    val screensaverStyle: ScreensaverStyle = ScreensaverStyle.GRADIENT,
    val pin: String? = null
)

data class ActiveState(
    val session: SessionEntity? = null,
    val remainingMs: Long = 0,
    val isActive: Boolean = false,
    val isFullMode: Boolean = false,
    val blockedAttempts: Int = 0,
    val lastBlockedPackage: String? = null
)

sealed class SessionEvent {
    data object Started : SessionEvent()
    data class Finished(val sessionId: Long, val status: String) : SessionEvent()
    data class Blocked(val packageName: String, val kind: String) : SessionEvent()
    data object NeedsHome : SessionEvent()
}
