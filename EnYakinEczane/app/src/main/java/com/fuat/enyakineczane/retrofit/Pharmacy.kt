package com.fuat.enyakineczane.retrofit

import java.io.Serializable

data class Pharmacy(
    val name: String,
    val distance: Double,
    val latitude: Double,
    val longitude: Double,
    val phoneNumber: String?
) : Serializable {
    val formattedDistance: String
        get() = if (distance < 1) {
            "${(distance * 1000).toInt()} metre"
        } else {
            "%.2f km".format(distance)
        }
}
