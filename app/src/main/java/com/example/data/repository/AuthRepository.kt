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
            val response = com.example.data.api.RetrofitClient.getAuthApiService(context)
                .login(LoginRequest(email = email, password = password))
            
            if (response.isSuccessful && response.body() != null) {
                val baseResponse = response.body()!!
                if (baseResponse.success && baseResponse.data != null) {
                    val loginResponse = baseResponse.data
                    tokenManager.saveToken(loginResponse.token)
                    tokenManager.saveUser(
                        loginResponse.user.id,
                        loginResponse.user.name,
                        loginResponse.user.username,
                        loginResponse.user.role.name
                    )
                    Result.success(loginResponse)
                } else {
                    Result.failure(Exception(baseResponse.message ?: "Login gagal, data tidak ditemukan"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Email atau kata sandi salah"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            com.example.data.api.RetrofitClient.getAuthApiService(context).logout()
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
