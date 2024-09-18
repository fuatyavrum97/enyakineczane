package com.fuat.enyakineczane.retrofit

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface CollectApiService {
    @GET("health/dutyPharmacy")
    suspend fun getDutyPharmacies(
        @Query("il") city: String
    ): retrofit2.Response<DutyPharmacyResponse>
}

class AuthInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("content-type", "application/json")
            .addHeader("authorization", "apikey $apiKey")
            .build()
        return chain.proceed(request)
    }
}

object CollectApiRetrofitInstance {
    private var apiKey: String? = null

    fun initialize(apiKey: String) {
        this.apiKey = apiKey
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(apiKey!!))
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.collectapi.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: CollectApiService by lazy {
        retrofit.create(CollectApiService::class.java)
    }
}
