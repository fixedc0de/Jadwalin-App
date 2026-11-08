package com.oxygen.jadwalin.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface JadwalApi {
    @GET("api/jadwal")
    suspend fun getAllJadwal(): Response<BaseResponse<List<JadwalRemote>>>

    @POST("api/jadwal")
    suspend fun createJadwal(@Body request: JadwalRemote): Response<BaseResponse<JadwalRemote>>

    @PUT("api/jadwal/{android_id}")
    suspend fun updateJadwal(@Path("android_id") id: Int, @Body request: JadwalRemote): Response<BaseResponse<JadwalRemote>>

    @DELETE("api/jadwal/{android_id}")
    suspend fun deleteJadwal(@Path("android_id") id: Int): Response<Unit>
}

data class JadwalRemote(
    @SerializedName("android_id") val android_id: Int,
    @SerializedName("judul") val judul: String,
    @SerializedName("deskripsi") val deskripsi: String?,
    @SerializedName("waktu_mulai") val waktu_mulai: String,
    @SerializedName("kategori") val kategori: String,
    @SerializedName("lokasi") val lokasi: String?,
    @SerializedName("is_active") val is_active: Boolean,
    @SerializedName("is_recurring") val is_recurring: Boolean,
    @SerializedName("hari_dalam_minggu") val hari_dalam_minggu: Int,
    @SerializedName("menit_sebelumnya") val menit_sebelumnya: Int
)
