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
            val response = com.example.data.api.RetrofitClient.getUserApiService(context).getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                val userResponse = response.body()!!
                tokenManager.saveUser(userResponse.user.id, userResponse.user.username, userResponse.user.role.name)
                Result.success(userResponse)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Gagal mengambil data user"
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
