package com.oxygen.jadwalin.data.repository

import android.util.Log
import com.oxygen.jadwalin.data.local.JadwalDao
import com.oxygen.jadwalin.data.local.JadwalEntity
import com.oxygen.jadwalin.data.remote.JadwalApi
import com.oxygen.jadwalin.data.remote.JadwalRemote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class JadwalRepository(
    private val dao: JadwalDao,
    private val api: JadwalApi? = null
) {
    fun getAllJadwal(): Flow<List<JadwalEntity>> = dao.getAllJadwal()

    // Fungsi untuk Sinkronisasi Total
    suspend fun syncAll() {
        try {
            // 0. Cek apakah ada API/Internet
            if (api == null) return

            // 1. Upload data lokal ke server
            val localData = dao.getAllJadwal().first()
            localData.forEach { jadwal ->
                try {
                    val response = api.createJadwal(jadwal.toRemote())
                    if (response.code() == 409) {
                        // Jika sudah ada (Conflict), coba update saja
                        api.updateJadwal(jadwal.id, jadwal.toRemote())
                    }
                } catch (e: Exception) {
                    Log.w("SYNC_DEBUG", "Item ${jadwal.id} gagal diunggah: ${e.message}")
                }
            }

            // 2. Download data dari server ke lokal
            val resp = api?.getAllJadwal()
            if (resp?.isSuccessful == true) {
                resp.body()?.forEach { remote ->
                    dao.insertJadwal(remote.toEntity())
                }
            }
            Log.d("SYNC_DEBUG", "Sinkronisasi selesai")
        } catch (e: Exception) {
            Log.e("SYNC_ERROR", "Gagal sinkronisasi: ${e.message}")
        }
    }

    suspend fun simpanJadwal(jadwal: JadwalEntity) {
        dao.insertJadwal(jadwal)
        try {
            val response = api?.createJadwal(jadwal.toRemote())
            if (response?.isSuccessful == true) {
                Log.d("SYNC_DEBUG", "Simpan berhasil disinkronkan ke backend")
            } else {
                Log.e("SYNC_ERROR", "Gagal sync simpan: ${response?.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("SYNC_ERROR", "Gagal sync simpan (Network): ${e.message}")
        }
    }

    suspend fun updateJadwal(jadwal: JadwalEntity) {
        dao.updateJadwal(jadwal)
        try {
            val response = api?.updateJadwal(jadwal.id, jadwal.toRemote())
            if (response?.isSuccessful == true) {
                Log.d("SYNC_DEBUG", "Update berhasil disinkronkan ke backend")
            } else {
                Log.e("SYNC_ERROR", "Gagal sync update: ${response?.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("SYNC_ERROR", "Gagal sync update (Network): ${e.message}")
        }
    }

    suspend fun hapusJadwal(jadwal: JadwalEntity) {
        dao.deleteJadwal(jadwal)
        try {
            val response = api?.deleteJadwal(jadwal.id)
            if (response?.isSuccessful == true) {
                Log.d("SYNC_DEBUG", "Hapus berhasil disinkronkan ke backend")
            } else {
                Log.e("SYNC_ERROR", "Gagal sync hapus: ${response?.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("SYNC_ERROR", "Gagal sync hapus (Network): ${e.message}")
        }
    }

    suspend fun fetchFromRemote() {
        try {
            val resp = api?.getAllJadwal()
            if (resp?.isSuccessful == true) {
                resp.body()?.forEach { remote ->
                    dao.insertJadwal(remote.toEntity())
                }
            }
        } catch (e: Exception) {
            Log.e("SYNC_ERROR", "Gagal fetch remote: ${e.message}")
        }
    }

    private fun JadwalEntity.toRemote() = JadwalRemote(
        android_id = id,
        judul = judul,
        deskripsi = deskripsi,
        waktu_mulai = waktu_mulai,
        kategori = kategori,
        lokasi = lokasi,
        isRecurring = isRecurring,
        hariDalamMinggu = hariDalamMinggu,
        menitSebelumnya = menitSebelumnya
    )

    private fun JadwalRemote.toEntity() = JadwalEntity(
        id = android_id,
        judul = judul,
        deskripsi = deskripsi,
        waktu_mulai = waktu_mulai,
        kategori = kategori,
        lokasi = lokasi,
        isRecurring = isRecurring,
        hariDalamMinggu = hariDalamMinggu,
        menitSebelumnya = menitSebelumnya
    )
}