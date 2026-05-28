package com.example.data

enum class UserRole(val displayName: String) {
    ADMIN_TOKO("Admin Toko"),
    ADMIN_KANTOR("Admin Kantor"),
    OWNER("Owner")
}

data class User(
    val id: String,
    val username: String,
    val role: UserRole
)
