package com.fuat.enyakineczane.retrofit

data class PlaceDetailsResponse(
    val result: PlaceDetailsResult
)

data class PlaceDetailsResult(
    val international_phone_number: String?
)
