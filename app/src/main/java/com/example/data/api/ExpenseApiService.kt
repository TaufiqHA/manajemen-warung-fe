package com.example.data.api

import com.example.ui.screens.BiayaOperasional
import com.example.data.BaseResponse
import retrofit2.Response
import retrofit2.http.*

data class ExpenseRequest(
    val kategori: String,
    val keterangan: String,
    val jumlah: Double,
    val tanggal: String,
    val pembuat: String? = null
)

interface ExpenseApiService {
    @GET("api/v1/expenses")
    suspend fun getExpenses(
        @Query("filter") filter: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<BaseResponse<List<BiayaOperasional>>>

    @POST("api/v1/expenses")
    suspend fun addExpense(
        @Body expense: ExpenseRequest
    ): Response<BaseResponse<BiayaOperasional>>

    @PUT("api/v1/expenses/{id}")
    suspend fun updateExpense(
        @Path("id") id: String,
        @Body expense: ExpenseRequest
    ): Response<BaseResponse<BiayaOperasional>>

    @DELETE("api/v1/expenses/{id}")
    suspend fun deleteExpense(
        @Path("id") id: String
    ): Response<BaseResponse<Any>>
}
