package com.example.data.repository

import android.content.Context
import com.example.data.UserResponse
import com.example.data.api.RetrofitClient
import com.example.utils.TokenManager

class UserRepository(private val context: Context) {
    private val userApiService = RetrofitClient.getUserApiService(context)
    private val tokenManager = TokenManager(context)

    suspend fun getCurrentUser(): Result<UserResponse> {
        return try {
            val response = userApiService.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                val userResponse = response.body()!!
                
                // Update local cached user info
                tokenManager.saveUser(
                    id = userResponse.user.id,
                    username = userResponse.user.username,
                    role = userResponse.user.role.name
                )
                
                Result.success(userResponse)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to get user profile"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLocalUserId(): String? = tokenManager.getUserId()
    fun getLocalUserName(): String? = tokenManager.getUserName()
    fun getLocalUserRole(): String? = tokenManager.getUserRole()
}
