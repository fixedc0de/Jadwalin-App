package com.oxygen.jadwalin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oxygen.jadwalin.data.repository.JadwalRepository

class JadwalViewModelFactory(private val repository: JadwalRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JadwalViewModel::class.java)) {
            return JadwalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}