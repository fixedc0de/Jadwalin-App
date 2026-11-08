package com.oxygen.jadwalin.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {
    companion object {
        private val REMINDER_KEY = intPreferencesKey("default_reminder_minutes")
        private val ALARM_SOUND_KEY = stringPreferencesKey("alarm_sound_uri")
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val AUTO_VIBRATE_KEY = booleanPreferencesKey("auto_vibrate")
        private val CRESCENDO_KEY = booleanPreferencesKey("crescendo_volume")
        private val DEFAULT_SNOOZE_KEY = intPreferencesKey("default_snooze_minutes")
    }

    // Token JWT
    val tokenFlow: Flow<String?> = context.appDataStore.data.map { it[TOKEN_KEY] }
    suspend fun saveToken(token: String?) {
        context.appDataStore.edit { prefs ->
            if (token == null) prefs.remove(TOKEN_KEY)
            else prefs[TOKEN_KEY] = token
        }
    }

    // Nama User
    val userNameFlow: Flow<String> = context.appDataStore.data.map { it[USER_NAME_KEY] ?: "User" }
    suspend fun saveUserName(name: String) {
        context.appDataStore.edit { it[USER_NAME_KEY] = name }
    }

    // Email User
    val userEmailFlow: Flow<String> = context.appDataStore.data.map { it[USER_EMAIL_KEY] ?: "" }
    suspend fun saveUserEmail(email: String) {
        context.appDataStore.edit { it[USER_EMAIL_KEY] = email }
    }

    // Reminder
    val reminderMinutesFlow: Flow<Int> = context.appDataStore.data.map { it[REMINDER_KEY] ?: 30 }
    suspend fun saveReminderMinutes(minutes: Int) {
        context.appDataStore.edit { it[REMINDER_KEY] = minutes }
    }

    // Alarm Sound
    val alarmSoundUriFlow: Flow<String?> = context.appDataStore.data.map { it[ALARM_SOUND_KEY] }
    suspend fun saveAlarmSoundUri(uri: String?) {
        context.appDataStore.edit { prefs ->
            if (uri != null) prefs[ALARM_SOUND_KEY] = uri
            else prefs.remove(ALARM_SOUND_KEY)
        }
    }

    // Auto Vibrate
    val autoVibrateFlow: Flow<Boolean> = context.appDataStore.data.map { it[AUTO_VIBRATE_KEY] ?: true }
    suspend fun saveAutoVibrate(enabled: Boolean) {
        context.appDataStore.edit { it[AUTO_VIBRATE_KEY] = enabled }
    }

    // Crescendo
    val crescendoFlow: Flow<Boolean> = context.appDataStore.data.map { it[CRESCENDO_KEY] ?: true }
    suspend fun saveCrescendo(enabled: Boolean) {
        context.appDataStore.edit { it[CRESCENDO_KEY] = enabled }
    }

    // Default Snooze Minutes
    val defaultSnoozeFlow: Flow<Int> = context.appDataStore.data.map { it[DEFAULT_SNOOZE_KEY] ?: 10 }
    suspend fun saveDefaultSnooze(minutes: Int) {
        context.appDataStore.edit { it[DEFAULT_SNOOZE_KEY] = minutes }
    }

    suspend fun clearAll() {
        context.appDataStore.edit { it.clear() }
    }
}
