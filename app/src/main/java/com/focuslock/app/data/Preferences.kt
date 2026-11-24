package com.focuslock.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val DEFAULT_MARKERS = setOf(
    "Reels", "reels", "Shorts", "shorts", "REEL", "ShortsPlayer",
    "Instagram Reels", "YouTube Shorts"
)

private val Context.focusDataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_lock_prefs")

class Prefs(private val context: Context) {
    private val ds = context.applicationContext.focusDataStore

    companion object {
        private val ACTIVE_SESSION = longPreferencesKey("active_session_id")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val PIN_HASH = stringPreferencesKey("pin_hash")
        private val PIN_SALT = stringPreferencesKey("pin_salt")
        private val MARKERS = stringSetPreferencesKey("feature_markers")
        private val WHITELIST = stringSetPreferencesKey("whitelist_packages")
        private val FULL_MODE = booleanPreferencesKey("full_mode_requested")
    }

    val activeSessionId: Flow<Long?> = ds.data.map { it[ACTIVE_SESSION] }
    val onboardingDone: Flow<Boolean> = ds.data.map { it[ONBOARDING_DONE] == true }
    val markers: Flow<Set<String>> = ds.data.map { it[MARKERS] ?: DEFAULT_MARKERS }
    val whitelist: Flow<Set<String>> = ds.data.map { it[WHITELIST] ?: emptySet() }
    val pinInfo: Flow<Pair<String?, String?>> = ds.data.map { it[PIN_HASH] to it[PIN_SALT] }

    suspend fun setActiveSessionId(id: Long?) {
        ds.edit { prefs ->
            if (id == null) prefs.remove(ACTIVE_SESSION) else prefs[ACTIVE_SESSION] = id
        }
    }

    suspend fun setOnboardingDone() {
        ds.edit { it[ONBOARDING_DONE] = true }
    }

    suspend fun setPin(hash: String?, salt: String?) {
        ds.edit { prefs ->
            if (hash == null || salt == null) {
                prefs.remove(PIN_HASH); prefs.remove(PIN_SALT)
            } else {
                prefs[PIN_HASH] = hash; prefs[PIN_SALT] = salt
            }
        }
    }

    suspend fun setMarkers(values: Set<String>) {
        ds.edit { it[MARKERS] = values }
    }

    suspend fun setWhitelist(values: Set<String>) {
        ds.edit { it[WHITELIST] = values }
    }

    suspend fun setFullModeRequested(value: Boolean) {
        ds.edit { it[FULL_MODE] = value }
    }

}
