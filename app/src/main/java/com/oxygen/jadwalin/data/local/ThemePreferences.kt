package com.oxygen.jadwalin.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

class ThemePreferences(private val context: Context) {
    companion object {
        private val THEME_KEY = intPreferencesKey("theme_mode")
        const val MODE_SYSTEM = 0; const val MODE_LIGHT = 1; const val MODE_DARK = 2
    }
    val themeModeFlow: Flow<Int> = context.themeDataStore.data.map { it[THEME_KEY] ?: MODE_SYSTEM }
    suspend fun saveThemeMode(mode: Int) { context.themeDataStore.edit { it[THEME_KEY] = mode } }
}