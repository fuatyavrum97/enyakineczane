package com.fuat.utils

import android.content.Context
import android.location.Geocoder
import android.location.Location
import java.util.Locale

object LocationUtils {

    fun getCityNameFromLocation(context: Context, location: Location): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].locality //şehir
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
