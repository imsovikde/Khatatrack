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

    /**
     * Observe dark mode preference as a Flow. Defaults to false (light mode).
     */
    fun isDarkMode(context: Context): Flow<Boolean> =
        context.appPreferencesDataStore.data.map { prefs ->
            prefs[KEY_DARK_MODE] ?: false
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
     */
    suspend fun setDarkMode(context: Context, enabled: Boolean) {
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
