package com.example.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Top-level property extension for DataStore singleton
private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

object AppPreferencesManager {

    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    private val KEY_SECURITY_PIN = stringPreferencesKey("security_pin")

    // SharedPreferences mirror for synchronous reads (eliminates cold-start theme flash)
    private const val PREFS_MIRROR = "app_prefs_mirror"
    private const val MIRROR_DARK_MODE = "dark_mode"

    /**
     * Synchronous read of dark mode — called before the first Compose frame
     * to prevent a light-theme flash on cold start.
     * Reads from a SharedPreferences mirror updated by [setDarkMode].
     * Defaults to true (dark) so new installs start in dark mode.
     */
    fun isDarkModeSync(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_MIRROR, Context.MODE_PRIVATE)
            .getBoolean(MIRROR_DARK_MODE, true) // default: dark mode
    }

    /**
     * Observe dark mode preference as a Flow. Defaults to true (dark mode).
     */
    fun isDarkMode(context: Context): Flow<Boolean> =
        context.appPreferencesDataStore.data.map { prefs ->
            prefs[KEY_DARK_MODE] ?: true
        }

    /**
     * Observe app lock enabled preference as a Flow. Defaults to false.
     */
    fun isAppLockEnabled(context: Context): Flow<Boolean> =
        context.appPreferencesDataStore.data.map { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] ?: false
        }

    /**
     * Observe security PIN as a Flow. Defaults to "1234".
     */
    fun securityPin(context: Context): Flow<String> =
        context.appPreferencesDataStore.data.map { prefs ->
            prefs[KEY_SECURITY_PIN] ?: "1234"
        }

    /**
     * Persist dark mode preference.
     * Also mirrors to SharedPreferences for synchronous cold-start reads.
     */
    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        // Mirror to SharedPreferences for sync reads on cold start
        context.getSharedPreferences(PREFS_MIRROR, Context.MODE_PRIVATE)
            .edit().putBoolean(MIRROR_DARK_MODE, enabled).apply()
        context.appPreferencesDataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = enabled
        }
    }

    /**
     * Persist app lock enabled preference.
     */
    suspend fun setAppLockEnabled(context: Context, enabled: Boolean) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    /**
     * Persist security PIN.
     */
    suspend fun setSecurityPin(context: Context, pin: String) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[KEY_SECURITY_PIN] = pin
        }
    }
}
