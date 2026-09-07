package com.oxygen.jadwalin.data.remote

import retrofit2.Response
import retrofit2.http.*

interface JadwalApi {
    @GET("api/jadwal")
    suspend fun getAllJadwal(): Response<List<JadwalRemote>>

    @POST("api/jadwal")
    suspend fun createJadwal(@Body request: JadwalRemote): Response<JadwalRemote>

    @PUT("api/jadwal/{android_id}")
    suspend fun updateJadwal(@Path("android_id") id: Int, @Body request: JadwalRemote): Response<JadwalRemote>

    @DELETE("api/jadwal/{android_id}")
    suspend fun deleteJadwal(@Path("android_id") id: Int): Response<Unit>
}

data class JadwalRemote(
    val android_id: Int,
    val judul: String,
    val deskripsi: String?,
    val waktu_mulai: String,
    val kategori: String,
    val lokasi: String?,
    val isRecurring: Boolean,
    val hariDalamMinggu: Int,
    val menitSebelumnya: Int
)