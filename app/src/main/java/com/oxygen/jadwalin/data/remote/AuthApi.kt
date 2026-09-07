package com.oxygen.jadwalin.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val data: AuthData?
)

data class AuthData(
    val user: UserRemote,
    val token: String
)

data class UserRemote(
    val id: Int,
    val email: String,
    val name: String
)

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val name: String)