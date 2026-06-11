package com.example.data.api

import com.example.ui.screens.BiayaOperasional
import com.example.data.BaseResponse
import retrofit2.Response
import retrofit2.http.*

interface ExpenseApiService {
    @GET("api/v1/expenses")
    suspend fun getExpenses(
        @Query("filter") filter: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<BaseResponse<List<BiayaOperasional>>>

    @POST("api/v1/expenses")
    suspend fun addExpense(
        @Body expense: BiayaOperasional
    ): Response<BaseResponse<BiayaOperasional>>

    @PUT("api/v1/expenses/{id}")
    suspend fun updateExpense(
        @Path("id") id: String,
        @Body expense: BiayaOperasional
    ): Response<BaseResponse<BiayaOperasional>>

    @DELETE("api/v1/expenses/{id}")
    suspend fun deleteExpense(
        @Path("id") id: String
    ): Response<BaseResponse<Any>>
}
