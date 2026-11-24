package com.focuslock.app.data

import kotlinx.coroutines.flow.Flow

class FocusRepository(private val db: FocusDatabase) {
    private val sessions = db.sessionDao()
    private val blockedApps = db.blockedAppDao()
    private val profiles = db.musicProfileDao()
    private val events = db.blockEventDao()

    val allSessions = sessions.observeAll()
    val completedSessions = sessions.observeCompleted()
    val completedCount = sessions.observeCompletedCount()
    val musicProfiles = profiles.observeAll()

    suspend fun createSession(session: SessionEntity): Long = sessions.insert(session)

    suspend fun updateSession(session: SessionEntity) = sessions.update(session)

    suspend fun getSession(id: Long): SessionEntity? = sessions.get(id)

    suspend fun activeSession(): SessionEntity? = sessions.getByStatus(SessionStatus.ACTIVE)

    suspend fun saveBlockedApps(items: List<BlockedAppEntity>) = blockedApps.insertAll(items)

    suspend fun getBlockedApps(sessionId: Long): List<BlockedAppEntity> = blockedApps.getForSession(sessionId)

    suspend fun saveOriginalState(sessionId: Long, pkg: String, state: Int) =
        blockedApps.saveOriginalState(sessionId, pkg, state)

    suspend fun clearBlockedApps(sessionId: Long) = blockedApps.deleteForSession(sessionId)

    suspend fun addProfile(profile: MusicProfileEntity): Long = profiles.insert(profile)

    suspend fun getProfile(id: Long): MusicProfileEntity? = profiles.get(id)

    suspend fun deleteProfile(profile: MusicProfileEntity) = profiles.delete(profile)

    suspend fun logBlock(sessionId: Long, pkg: String, kind: String) = events.insert(
        BlockEventEntity(sessionId = sessionId, packageName = pkg, kind = kind)
    )

    suspend fun blockCount(sessionId: Long): Int = events.countForSession(sessionId)

    suspend fun mostBlockedPackage(): String? = events.mostBlockedPackage()

    suspend fun totalFocusMs(): Long = sessions.completedOnce().sumOf { it.actualDurationMs }

    fun observeActiveSession(): Flow<SessionEntity?> = sessions.observeByStatus(SessionStatus.ACTIVE)
}
