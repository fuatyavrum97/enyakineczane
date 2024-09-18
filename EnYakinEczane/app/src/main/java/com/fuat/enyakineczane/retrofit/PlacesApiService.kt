package com.fuat.enyakineczane.retrofit

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApiService {
    @GET("place/nearbysearch/json")
    suspend fun getNearbyPharmacies(
        @Query("location") location: String,
        @Query("rankby") rankBy: String,
        @Query("type") type: String,
        @Query("key") apiKey: String
    ): Response<PlacesResponse>

    @GET("place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("key") apiKey: String
    ): Response<PlaceDetailsResponse>
}
