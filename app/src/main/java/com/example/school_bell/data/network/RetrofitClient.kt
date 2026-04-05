package com.example.school_bell.data.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val DEFAULT_BASE_URL = "http://localhost:8080/"
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L

    @Volatile
    private var retrofit: Retrofit? = null

    @Volatile
    private var currentBaseUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    fun getApiService(baseUrl: String = DEFAULT_BASE_URL): ApiService {
        if (retrofit == null || currentBaseUrl != baseUrl) {
            synchronized(this) {
                if (retrofit == null || currentBaseUrl != baseUrl) {
                    currentBaseUrl = baseUrl
                    retrofit = buildRetrofit(baseUrl)
                }
            }
        }
        return retrofit!!.create(ApiService::class.java)
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(buildOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")

            authToken?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }

            chain.proceed(requestBuilder.build())
        }

        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /** Force rebuild with new base URL (call when server URL changes in settings) */
    fun reset() {
        synchronized(this) {
            retrofit = null
        }
    }
}
