package com.oxygen.jadwalin.data.remote

import android.content.Context
import android.util.Log
import com.oxygen.jadwalin.data.local.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {
    private const val BASE_URL = "https://jadwalin-backend-1zlg.vercel.app/"
    private var okHttpClient: OkHttpClient? = null
    private var retrofit: Retrofit? = null

    private fun getAuthInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            val prefs = AppPreferences(context)
            val token = runBlocking { prefs.tokenFlow.first() }
            val requestBuilder = chain.request().newBuilder()
            
            if (!token.isNullOrEmpty()) {
                // Sangat penting: hapus tanda kutip dan trim
                val cleanToken = token.replace("\"", "").trim()
                requestBuilder.header("Authorization", "Bearer $cleanToken")
                Log.d("NETWORK_DEBUG", "Header ditambahkan: Bearer ${cleanToken.take(10)}... (Panjang: ${cleanToken.length})")
            } else {
                Log.w("NETWORK_DEBUG", "Token TIDAK ditemukan di DataStore")
            }
            
            chain.proceed(requestBuilder.build())
        }
    }

    private fun getOkHttpClient(context: Context): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            val logging = HttpLoggingInterceptor { message ->
                Log.d("OKHTTP_LOG", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(getAuthInterceptor(context))
                .addInterceptor(logging)
                .build()
            okHttpClient = client
            client
        }
    }

    fun getRetrofit(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            val instance = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient(context))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            retrofit = instance
            instance
        }
    }
}