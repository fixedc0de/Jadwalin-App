package com.oxygen.jadwalin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jadwal")
data class JadwalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val judul: String,
    val deskripsi: String?,
    val waktu_mulai: String, // Format ISO: yyyy-MM-dd'T'HH:mm:ss
    val kategori: String,
    val lokasi: String?,
    val isRecurring: Boolean = false,
    val hariDalamMinggu: Int = -1,
    val menitSebelumnya: Int = 30
)