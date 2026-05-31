package com.example.data.api

import com.example.data.BaseResponse
import com.example.data.UpdateWarungRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT

interface WarungSettingApiService {
    @PUT("api/v1/settings/warung")
    suspend fun updateWarungSetting(
        @Body request: UpdateWarungRequest
    ): Response<BaseResponse<Any>>
}
