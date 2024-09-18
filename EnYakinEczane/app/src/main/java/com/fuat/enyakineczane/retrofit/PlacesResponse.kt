package com.fuat.enyakineczane.retrofit

data class PlacesResponse(
    val results: List<PlaceResult>
)

data class PlaceResult(
    val place_id: String,
    val name: String,
    val geometry: Geometry
)

data class Geometry(
    val location: Location
)

data class Location(
    val lat: Double,
    val lng: Double
)
