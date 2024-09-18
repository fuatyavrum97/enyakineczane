package com.fuat.enyakineczane.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: PlacesApiService by lazy {
        retrofit.create(PlacesApiService::class.java)
    }
}
