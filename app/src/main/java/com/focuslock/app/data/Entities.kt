package com.focuslock.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "scheduled_start_at") val scheduledStartAt: Long? = null,
    @ColumnInfo(name = "started_at") val startedAt: Long? = null,
    @ColumnInfo(name = "deadline_elapsed") val deadlineElapsed: Long? = null,
    @ColumnInfo(name = "planned_duration_ms") val plannedDurationMs: Long,
    @ColumnInfo(name = "actual_duration_ms") val actualDurationMs: Long = 0,
    val status: String = SessionStatus.SCHEDULED,
    @ColumnInfo(name = "screensaver_style") val screensaverStyle: String = "GRADIENT",
    @ColumnInfo(name = "music_profile_id") val musicProfileId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "blocking_applied") val blockingApplied: Boolean = false,
    @ColumnInfo(name = "finish_started") val finishStarted: Boolean = false,
    @ColumnInfo(name = "aborted_by_pin") val abortedByPin: Boolean = false
)

object SessionStatus {
    const val SCHEDULED = "SCHEDULED"
    const val ACTIVE = "ACTIVE"
    const val COMPLETED = "COMPLETED"
    const val ABORTED = "ABORTED"
}

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    /** FULL or FEATURE */
    val kind: String = "FULL",
    @ColumnInfo(name = "original_enabled_state") val originalEnabledState: Int? = null
)

@Entity(tableName = "music_profiles")
data class MusicProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** BUNDLED, LOCAL or STREAMING */
    val source: String,
    val location: String,
    val volume: Float = 0.7f
)

@Entity(tableName = "block_events")
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    /** APP or FEATURE */
    val kind: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY created_at DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = :status LIMIT 1")
    suspend fun getByStatus(status: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = :status LIMIT 1")
    fun observeByStatus(status: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE status = 'COMPLETED' ORDER BY completed_at DESC")
    fun observeCompleted(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status = 'COMPLETED'")
    suspend fun completedOnce(): List<SessionEntity>

    @Query("UPDATE sessions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT COUNT(*) FROM sessions WHERE status = 'COMPLETED'")
    fun observeCompletedCount(): Flow<Int>
}

@Dao
interface BlockedAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BlockedAppEntity>)

    @Query("SELECT * FROM blocked_apps WHERE session_id = :sessionId")
    suspend fun getForSession(sessionId: Long): List<BlockedAppEntity>

    @Query("UPDATE blocked_apps SET original_enabled_state = :state WHERE session_id = :sessionId AND package_name = :pkg")
    suspend fun saveOriginalState(sessionId: Long, pkg: String, state: Int)

    @Query("DELETE FROM blocked_apps WHERE session_id = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}

@Dao
interface MusicProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: MusicProfileEntity): Long

    @Query("SELECT * FROM music_profiles ORDER BY id DESC")
    fun observeAll(): Flow<List<MusicProfileEntity>>

    @Query("SELECT * FROM music_profiles WHERE id = :id")
    suspend fun get(id: Long): MusicProfileEntity?

    @Delete
    suspend fun delete(profile: MusicProfileEntity)
}

@Dao
interface BlockEventDao {
    @Insert
    suspend fun insert(event: BlockEventEntity)

    @Query("SELECT COUNT(*) FROM block_events WHERE session_id = :sessionId")
    suspend fun countForSession(sessionId: Long): Int

    @Query("SELECT package_name FROM block_events WHERE kind = 'APP' GROUP BY package_name ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun mostBlockedPackage(): String?
}

@Database(
    entities = [SessionEntity::class, BlockedAppEntity::class, MusicProfileEntity::class, BlockEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun musicProfileDao(): MusicProfileDao
    abstract fun blockEventDao(): BlockEventDao

    companion object {
        @Volatile
        private var INSTANCE: FocusDatabase? = null
        fun get(context: Context): FocusDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, FocusDatabase::class.java, "focus_lock.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
