package com.oxygen.jadwalin.data.repository

import android.content.Context
import android.util.Log
import com.oxygen.jadwalin.data.local.AppPreferences
import com.oxygen.jadwalin.data.local.JadwalDao
import com.oxygen.jadwalin.data.remote.AuthApi
import com.oxygen.jadwalin.data.remote.LoginRequest
import com.oxygen.jadwalin.data.remote.RegisterRequest
import com.oxygen.jadwalin.util.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val api: AuthApi,
    private val prefs: AppPreferences,
    private val dao: JadwalDao? = null,
    private val context: Context? = null
) {
    
    val userName: Flow<String> = prefs.userNameFlow
    val userEmail: Flow<String> = prefs.userEmailFlow

    suspend fun login(email: String, pass: String): Boolean {
        return try {
            val resp = api.login(LoginRequest(email, pass))
            if (resp.isSuccessful) {
                val data = resp.body()?.data
                if (data != null) {
                    prefs.saveToken(data.token)
                    prefs.saveUserName(data.user.name)
                    prefs.saveUserEmail(data.user.email)
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun register(email: String, pass: String, name: String): Boolean {
        return try {
            val resp = api.register(RegisterRequest(email, pass, name))
            if (resp.isSuccessful) {
                val data = resp.body()?.data
                if (data != null) {
                    prefs.saveToken(data.token)
                    prefs.saveUserName(data.user.name)
                    prefs.saveUserEmail(data.user.email)
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logout() {
        try {
            if (dao != null && context != null) {
                val localList = dao.getAllJadwal().first()
                localList.forEach { jadwal ->
                    AlarmScheduler.cancelAlarm(context, jadwal.id)
                }
                dao.deleteAllJadwal()
                Log.d("LOGOUT_DEBUG", "Semua alarm dibatalkan dan database lokal dibersihkan.")
            }
        } catch (e: Exception) {
            Log.e("LOGOUT_DEBUG", "Gagal membersihkan data lokal saat logout: ${e.message}")
        }
        prefs.clearAll()
    }
}
