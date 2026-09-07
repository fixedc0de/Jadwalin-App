package com.oxygen.jadwalin.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {
    companion object {
        private val REMINDER_KEY = intPreferencesKey("default_reminder_minutes")
        private val ALARM_SOUND_KEY = stringPreferencesKey("alarm_sound_uri")
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    }

    // Token JWT untuk Backend
    val tokenFlow: Flow<String?> = context.appDataStore.data
        .map { it[TOKEN_KEY] }

    suspend fun saveToken(token: String?) {
        context.appDataStore.edit { prefs ->
            if (token == null) prefs.remove(TOKEN_KEY)
            else prefs[TOKEN_KEY] = token
        }
    }

    // Membaca waktu pengingat default (Default: 30 menit)
    val reminderMinutesFlow: Flow<Int> = context.appDataStore.data
        .map { preferences -> preferences[REMINDER_KEY] ?: 30 }

    // Menyimpan waktu pengingat default
    suspend fun saveReminderMinutes(minutes: Int) {
        context.appDataStore.edit { preferences ->
            preferences[REMINDER_KEY] = minutes
        }
    }

    // Membaca URI suara alarm (Default: null, berarti sistem default)
    val alarmSoundUriFlow: Flow<String?> = context.appDataStore.data
        .map { preferences -> preferences[ALARM_SOUND_KEY] }

    // Menyimpan URI suara alarm
    suspend fun saveAlarmSoundUri(uri: String?) {
        context.appDataStore.edit { preferences ->
            if (uri != null) {
                preferences[ALARM_SOUND_KEY] = uri
            } else {
                preferences.remove(ALARM_SOUND_KEY)
            }
        }
    }
}