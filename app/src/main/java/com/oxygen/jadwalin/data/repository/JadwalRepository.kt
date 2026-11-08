package com.oxygen.jadwalin.data.repository

import android.util.Log
import com.oxygen.jadwalin.data.local.JadwalDao
import com.oxygen.jadwalin.data.local.JadwalEntity
import com.oxygen.jadwalin.data.remote.JadwalApi
import com.oxygen.jadwalin.data.remote.JadwalRemote
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class JadwalRepository(
    private val dao: JadwalDao,
    private val api: JadwalApi? = null
) {
    fun getAllJadwal(): Flow<List<JadwalEntity>> = dao.getAllJadwal()

    suspend fun syncAll() {
        if (api == null) {
            Log.d("SYNC_DEBUG", "API client is null, skip sync.")
            return
        }
        Log.d("SYNC_DEBUG", ">>> MEMULAI SINKRONISASI TOTAL")
        delay(500) 

        try {
            val localData = dao.getAllJadwal().first()
            Log.d("SYNC_DEBUG", "Jumlah data lokal ditemukan: ${localData.size}")
            if (localData.isNotEmpty()) {
                localData.forEach { jadwal ->
                    Log.d("SYNC_DEBUG", "Mengunggah jadwal: ${jadwal.judul} (ID: ${jadwal.id})")
                    val response = api.createJadwal(jadwal.toRemote())
                    Log.d("SYNC_DEBUG", "Respon upload ${jadwal.judul}: code=${response.code()}")
                    if (response.code() == 409) {
                        api.updateJadwal(jadwal.id, jadwal.toRemote())
                    }
                }
            } else {
                Log.d("SYNC_DEBUG", "Database lokal kosong, tidak ada data yang di-upload.")
            }
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "Gagal Upload: ${e.message}", e)
        }

        try {
            Log.d("SYNC_DEBUG", "Mengambil data dari server...")
            val resp = api.getAllJadwal()
            Log.d("SYNC_DEBUG", "Respon server getAllJadwal: code=${resp.code()}, isSuccessful=${resp.isSuccessful}")
            if (resp.isSuccessful) {
                val remoteList = resp.body()?.data ?: emptyList()
                Log.d("SYNC_DEBUG", "Jumlah data dari server: ${remoteList.size}")
                remoteList.forEach { remote ->
                    dao.insertJadwal(remote.toEntity())
                    Log.d("SYNC_DEBUG", "Memasukkan data server ke lokal: ${remote.judul}")
                }
            } else {
                Log.e("SYNC_DEBUG", "Gagal fetch server, error code: ${resp.code()}, errorBody: ${resp.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "Gagal Download/Format: ${e.message}", e)
        }
        Log.d("SYNC_DEBUG", "<<< SINKRONISASI SELESAI")
    }

    suspend fun simpanJadwal(jadwal: JadwalEntity) {
        dao.insertJadwal(jadwal)
        try {
            val resp = api?.createJadwal(jadwal.toRemote())
            if (resp?.isSuccessful == true) {
                Log.d("SYNC_DEBUG", "Berhasil simpan ke cloud: ${jadwal.judul}")
            } else {
                Log.e("SYNC_DEBUG", "Gagal simpan ke cloud: code=${resp?.code()}, error=${resp?.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "Network Error simpan: ${e.message}", e)
        }
    }

    suspend fun updateJadwal(jadwal: JadwalEntity) {
        dao.updateJadwal(jadwal)
        try {
            val resp = api?.updateJadwal(jadwal.id, jadwal.toRemote())
            if (resp?.isSuccessful == true) {
                Log.d("SYNC_DEBUG", "Berhasil update ke cloud: ${jadwal.judul}")
            } else {
                Log.e("SYNC_DEBUG", "Gagal update ke cloud: code=${resp?.code()}, error=${resp?.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "Network Error update: ${e.message}", e)
        }
    }

    suspend fun hapusJadwal(jadwal: JadwalEntity) {
        dao.deleteJadwal(jadwal)
        try {
            val resp = api?.deleteJadwal(jadwal.id)
            if (resp?.isSuccessful == true) {
                Log.d("SYNC_DEBUG", "Berhasil hapus dari cloud ID: ${jadwal.id}")
            } else {
                Log.e("SYNC_DEBUG", "Gagal hapus dari cloud: code=${resp?.code()}, error=${resp?.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "Network Error hapus: ${e.message}", e)
        }
    }

    suspend fun fetchFromRemote() {
        try {
            val resp = api?.getAllJadwal()
            if (resp?.isSuccessful == true) {
                resp.body()?.data?.forEach { remote ->
                    dao.insertJadwal(remote.toEntity())
                }
            }
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "Gagal fetch remote: ${e.message}")
        }
    }

    private fun JadwalEntity.toRemote() = JadwalRemote(
        android_id = id,
        judul = judul,
        deskripsi = deskripsi,
        waktu_mulai = waktu_mulai,
        kategori = kategori,
        lokasi = lokasi,
        is_active = is_active,
        is_recurring = isRecurring,
        hari_dalam_minggu = hariDalamMinggu,
        menit_sebelumnya = menitSebelumnya
    )

    private fun JadwalRemote.toEntity() = JadwalEntity(
        id = android_id,
        judul = judul,
        deskripsi = deskripsi,
        waktu_mulai = waktu_mulai,
        kategori = kategori,
        lokasi = lokasi,
        is_active = is_active,
        isRecurring = is_recurring,
        hariDalamMinggu = hari_dalam_minggu,
        menitSebelumnya = menit_sebelumnya
    )
}
