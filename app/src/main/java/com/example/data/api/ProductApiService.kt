package com.example.data.api

import com.example.ui.screens.MenuItem
import com.example.data.BaseResponse
import retrofit2.Response
import retrofit2.http.*
import com.squareup.moshi.JsonClass

data class CategoryRequest(
    @com.squareup.moshi.Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class ExportResponse(
    val success: Boolean,
    val message: String? = null,
    @com.squareup.moshi.Json(name = "download_url") val downloadUrl: String? = null
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

    @GET("api/v1/products/export")
    suspend fun exportProducts(
        @Query("search") search: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_order") sortOrder: String? = null
    ): Response<ExportResponse>
}

