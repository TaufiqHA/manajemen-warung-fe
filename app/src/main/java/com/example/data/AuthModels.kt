package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val token: String,
    val user: UserData
)

@JsonClass(generateAdapter = true)
data class UserData(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    val role: UserRole
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    val success: Boolean,
    @com.squareup.moshi.Json(name = "data") val user: UserData
)

@JsonClass(generateAdapter = true)
data class BaseResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

@JsonClass(generateAdapter = true)
data class UpdateUserRequest(
    @com.squareup.moshi.Json(name = "name") val name: String,
    @com.squareup.moshi.Json(name = "username") val username: String? = null,
    @com.squareup.moshi.Json(name = "email") val email: String? = null,
    @com.squareup.moshi.Json(name = "password") val password: String? = null
)

@JsonClass(generateAdapter = true)
data class UserListResponse(
    val data: List<UserData>? = null
)

@JsonClass(generateAdapter = true)
data class UserUpdateResponse(
    val success: Boolean,
    val message: String? = null,
    val data: UserData? = null
)

