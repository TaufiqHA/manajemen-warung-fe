package com.example.data.api

import com.example.ui.screens.TransaksiHarian
import com.example.data.BaseResponse
import com.example.data.TransactionRequest
import retrofit2.Response
import retrofit2.http.*
import okhttp3.ResponseBody

interface TransactionApiService {
    @GET("api/v1/transactions")
    suspend fun getTransactions(
        @Query("filter") filter: String? = null
    ): Response<BaseResponse<List<TransaksiHarian>>>

    @POST("api/v1/transactions")
    suspend fun createTransaction(
        @Body request: TransactionRequest
    ): Response<BaseResponse<Any>>

    @PATCH("api/v1/transactions/{id}/cancel")
    suspend fun cancelTransaction(
        @Path("id") transactionId: String,
        @Body request: com.example.data.CancelTransactionRequest
    ): Response<ResponseBody>

    /**
     * Endpoint untuk menghapus transaksi permanen.
     * Hanya pengguna dengan role ADMIN_TOKO yang diizinkan (ditangani server).
     */
    @DELETE("api/v1/transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") transactionId: String
    ): Response<BaseResponse<Any>>
}
