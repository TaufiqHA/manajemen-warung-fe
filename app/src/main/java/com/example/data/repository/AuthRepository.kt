package com.example.data.repository

import android.content.Context
import com.example.data.LoginRequest
import com.example.data.LoginResponse
import com.example.data.api.RetrofitClient
import com.example.utils.TokenManager
import retrofit2.Response

class AuthRepository(private val context: Context) {
    private val authApiService = RetrofitClient.getAuthApiService(context)
    private val tokenManager = TokenManager(context)

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = authApiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                
                // Save token and user info locally
                tokenManager.saveToken(loginResponse.token)
                tokenManager.saveUser(
                    id = loginResponse.user.id,
                    username = loginResponse.user.username,
                    role = loginResponse.user.role.name
                )
                
                Result.success(loginResponse)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Login failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            val response = authApiService.logout()
            // Always clear token and user details on local regardless of API status
            tokenManager.clearAll()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Logout API failed but local credentials cleared"))
            }
        } catch (e: Exception) {
            // Always clear local data even if network fails
            tokenManager.clearAll()
            Result.failure(e)
        }
    }

    fun getLocalToken(): String? = tokenManager.getToken()
    fun isUserLoggedIn(): Boolean = !tokenManager.getToken().isNullOrEmpty()
}
