package com.oxygen.jadwalin.data.remote

data class BaseResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)
