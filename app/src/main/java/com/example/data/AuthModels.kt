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
    val username: String,
    val email: String,
    val role: UserRole
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    val user: UserData
)
