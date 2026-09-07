package com.oxygen.jadwalin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygen.jadwalin.data.local.JadwalEntity
import com.oxygen.jadwalin.data.repository.JadwalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// State untuk UI
sealed class JadwalUiState {
    data object Loading : JadwalUiState()
    data class Success(val data: List<JadwalEntity>) : JadwalUiState()
    data class Error(val msg: String) : JadwalUiState()
}

class JadwalViewModel(private val repo: JadwalRepository) : ViewModel() {

    // Menggunakan stateIn untuk mengubah Flow dari repository menjadi StateFlow yang reactive
    val uiState: StateFlow<JadwalUiState> = repo.getAllJadwal()
        .map { list -> JadwalUiState.Success(list) as JadwalUiState }
        .catch { e -> emit(JadwalUiState.Error(e.message ?: "Terjadi kesalahan")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = JadwalUiState.Loading
        )

    // 1. Tambah jadwal baru
    fun simpanJadwal(jadwal: JadwalEntity) {
        viewModelScope.launch {
            repo.simpanJadwal(jadwal)
        }
    }

    // 3. Ubah/Update jadwal yang sudah ada
    fun updateJadwal(jadwal: JadwalEntity) {
        viewModelScope.launch {
            repo.updateJadwal(jadwal)
        }
    }

    // 4. Hapus jadwal
    fun hapusJadwal(jadwal: JadwalEntity) {
        viewModelScope.launch {
            repo.hapusJadwal(jadwal)
        }
    }

    // Sinkronisasi total (Upload & Download)
    fun syncData() {
        viewModelScope.launch {
            repo.syncAll()
        }
    }

    // Refresh data dari remote (hanya download)
    fun refreshData() {
        viewModelScope.launch {
            repo.fetchFromRemote()
        }
    }
}