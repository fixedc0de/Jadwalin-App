package com.oxygen.jadwalin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oxygen.jadwalin.data.local.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val prefs: AppPreferences) : ViewModel() {

    val autoVibrate: StateFlow<Boolean> = prefs.autoVibrateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val crescendo: StateFlow<Boolean> = prefs.crescendoFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reminderMinutes: StateFlow<Int> = prefs.reminderMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val defaultSnooze: StateFlow<Int> = prefs.defaultSnoozeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val alarmSoundUri: StateFlow<String?> = prefs.alarmSoundUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setAutoVibrate(enabled: Boolean) {
        viewModelScope.launch { prefs.saveAutoVibrate(enabled) }
    }

    fun setCrescendo(enabled: Boolean) {
        viewModelScope.launch { prefs.saveCrescendo(enabled) }
    }
    
    fun setReminderMinutes(minutes: Int) {
        viewModelScope.launch { prefs.saveReminderMinutes(minutes) }
    }

    fun setDefaultSnooze(minutes: Int) {
        viewModelScope.launch { prefs.saveDefaultSnooze(minutes) }
    }

    fun setAlarmSoundUri(uri: String?) {
        viewModelScope.launch { prefs.saveAlarmSoundUri(uri) }
    }
}

class SettingsViewModelFactory(private val prefs: AppPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
