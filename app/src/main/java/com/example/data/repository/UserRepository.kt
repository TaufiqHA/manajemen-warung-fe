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
                tokenManager.saveUser(
                    userResponse.user.id,
                    userResponse.user.name,
                    userResponse.user.username,
                    userResponse.user.role.name
                )
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
    fun getLocalUserName(): String? = tokenManager.getUserDisplayName()
    fun getLocalUserUsername(): String? = tokenManager.getUserName()
    fun getLocalUserRole(): String? = tokenManager.getUserRole()

    suspend fun updateProfile(newName: String): Result<Unit> {
        return try {
            val username = getLocalUserUsername() ?: ""
            val request = com.example.data.UpdateUserRequest(name = newName, username = username)
            val response = com.example.data.api.RetrofitClient.getUserApiService(context).updateProfile(request)
            
            if (response.isSuccessful) {
                tokenManager.saveUser(
                    id = getLocalUserId() ?: "",
                    name = newName,
                    username = username,
                    role = getLocalUserRole() ?: ""
                )
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Gagal memperbarui profil"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getAllUsers(): Result<List<UserData>> {
        return try {
            val response = com.example.data.api.RetrofitClient.getUserApiService(context).getAllUsers()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception("Gagal mengambil daftar user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserById(userId: String, name: String, email: String?, password: String?): Result<Unit> {
        return try {
            val request = com.example.data.UpdateUserRequest(name = name, email = email, password = password)
            val response = com.example.data.api.RetrofitClient.getUserApiService(context).updateUser(userId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Gagal mengubah data user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
