package com.example.data.api

import com.example.data.BaseResponse
import com.example.data.UserData
import com.example.data.UserResponse
import com.example.data.UpdateUserRequest
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Body

interface UserApiService {
    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @PUT("api/v1/users/me")
    suspend fun updateProfile(@Body request: UpdateUserRequest): Response<okhttp3.ResponseBody>

    @GET("api/v1/users")
    suspend fun getAllUsers(): Response<com.example.data.UserListResponse>

    @PUT("api/v1/users/{id}")
    suspend fun updateUser(
        @retrofit2.http.Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): Response<com.example.data.UserUpdateResponse>
}

