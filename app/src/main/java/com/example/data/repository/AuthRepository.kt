package com.example.data.repository

import android.content.Context
import com.example.data.LoginRequest
import com.example.data.LoginResponse
import com.example.data.UserData
import com.example.data.UserRole
import com.example.utils.TokenManager
import kotlinx.coroutines.delay

class AuthRepository(private val context: Context) {
    private val tokenManager = TokenManager(context)

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            delay(1000) // Mock network delay
            
            // Simple hardcoded mock validation
            val (mockUser, mockToken) = when (email) {
                "owner@warung.com" -> Pair(
                    UserData("1", "Owner Budi", email, UserRole.OWNER),
                    "mock_token_owner_123"
                )
                "admin@warung.com" -> Pair(
                    UserData("2", "Admin Siti", email, UserRole.ADMIN_TOKO),
                    "mock_token_admin_123"
                )
                "adminkantor@warung.com" -> Pair(
                    UserData("3", "Admin Kantor Joko", email, UserRole.ADMIN_KANTOR),
                    "mock_token_adminkantor_123"
                )
                else -> null
            } ?: throw Exception("Email atau kata sandi salah")

            if (password != "password") {
                throw Exception("Email atau kata sandi salah")
            }

            tokenManager.saveToken(mockToken)
            tokenManager.saveUser(mockUser.id, mockUser.username, mockUser.role.name)

            Result.success(LoginResponse(token = mockToken, user = mockUser))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            delay(500)
            tokenManager.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            tokenManager.clearAll()
            Result.failure(e)
        }
    }

    fun getLocalToken(): String? = tokenManager.getToken()
    fun isUserLoggedIn(): Boolean = !tokenManager.getToken().isNullOrEmpty()
}
