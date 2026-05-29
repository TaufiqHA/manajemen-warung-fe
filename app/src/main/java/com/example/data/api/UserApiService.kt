package com.example.data.api

import com.example.data.UserResponse
import retrofit2.Response
import retrofit2.http.GET

interface UserApiService {
    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): Response<UserResponse>
}
