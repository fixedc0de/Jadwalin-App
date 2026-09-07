package com.oxygen.jadwalin.data.repository

import com.oxygen.jadwalin.data.local.AppPreferences
import com.oxygen.jadwalin.data.remote.AuthApi
import com.oxygen.jadwalin.data.remote.LoginRequest
import com.oxygen.jadwalin.data.remote.RegisterRequest

class AuthRepository(private val api: AuthApi, private val prefs: AppPreferences) {
    suspend fun login(email: String, pass: String): Boolean {
        return try {
            val resp = api.login(LoginRequest(email, pass))
            if (resp.isSuccessful) {
                resp.body()?.data?.token?.let {
                    prefs.saveToken(it)
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
                resp.body()?.data?.token?.let {
                    prefs.saveToken(it)
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logout() {
        prefs.saveToken(null)
    }
}