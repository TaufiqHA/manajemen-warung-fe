package com.example.data.repository

import android.content.Context
import com.example.data.UserData
import com.example.data.UserResponse
import com.example.data.UserRole
import com.example.utils.TokenManager
import kotlinx.coroutines.delay

class UserRepository(private val context: Context) {
    private val tokenManager = TokenManager(context)

    suspend fun getCurrentUser(): Result<UserResponse> {
        return try {
            delay(500)
            val userId = tokenManager.getUserId()
            val userName = tokenManager.getUserName()
            val userRoleStr = tokenManager.getUserRole()
            
            if (userId != null && userName != null && userRoleStr != null) {
                val role = try { UserRole.valueOf(userRoleStr) } catch(e: Exception) { UserRole.ADMIN_TOKO }
                val mockUser = UserData(id = userId, username = userName, email = "mock@warung.com", role = role)
                Result.success(UserResponse(user = mockUser))
            } else {
                Result.failure(Exception("User not logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLocalUserId(): String? = tokenManager.getUserId()
    fun getLocalUserName(): String? = tokenManager.getUserName()
    fun getLocalUserRole(): String? = tokenManager.getUserRole()
}
