package com.example.data.api

import com.example.ui.screens.MenuItem
import com.example.data.BaseResponse
import retrofit2.Response
import retrofit2.http.*

data class CategoryRequest(
    @com.squareup.moshi.Json(name = "name") val name: String
)

interface ProductApiService {
    @GET("api/v1/products")
    suspend fun getProducts(): Response<BaseResponse<List<MenuItem>>>

    @POST("api/v1/products")
    suspend fun addProduct(
        @Body product: MenuItem
    ): Response<BaseResponse<MenuItem>>

    @PUT("api/v1/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: String,
        @Body product: MenuItem
    ): Response<BaseResponse<MenuItem>>

    @DELETE("api/v1/products/{id}")
    suspend fun deleteProduct(
        @Path("id") id: String
    ): Response<BaseResponse<Any>>

    @POST("api/v1/categories")
    suspend fun addCategory(
        @Body category: CategoryRequest
    ): Response<BaseResponse<Any>>
}
