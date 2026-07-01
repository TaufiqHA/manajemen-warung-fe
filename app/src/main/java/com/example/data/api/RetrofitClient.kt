package com.example.data.api

import android.content.Context
import com.example.utils.TokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.153.15.115:8000" // Connection to local server by IP address

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun getOkHttpClient(context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val tokenManager = TokenManager(context)

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val token = tokenManager.getToken()

                val requestBuilder = originalRequest.newBuilder()
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                requestBuilder.addHeader("Accept", "application/json")

                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .build()
    }

    fun getRetrofit(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getOkHttpClient(context))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun getAuthApiService(context: Context): AuthApiService {
        return getRetrofit(context).create(AuthApiService::class.java)
    }

    fun getUserApiService(context: Context): UserApiService {
        return getRetrofit(context).create(UserApiService::class.java)
    }

    fun getProductApiService(context: Context): ProductApiService {
        return getRetrofit(context).create(ProductApiService::class.java)
    }

    fun getExpenseApiService(context: Context): ExpenseApiService {
        return getRetrofit(context).create(ExpenseApiService::class.java)
    }

    fun getTransactionApiService(context: Context): TransactionApiService {
        return getRetrofit(context).create(TransactionApiService::class.java)
    }

    fun getWarungSettingApiService(context: Context): WarungSettingApiService {
        return getRetrofit(context).create(WarungSettingApiService::class.java)
    }
}
