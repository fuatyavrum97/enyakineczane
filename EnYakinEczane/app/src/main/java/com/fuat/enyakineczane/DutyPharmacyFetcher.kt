package com.fuat.enyakineczane

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.fuat.enyakineczane.retrofit.CollectApiRetrofitInstance
import com.fuat.enyakineczane.retrofit.Pharmacy
import com.fuat.enyakineczane.shared_preferences.PreferenceHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object DutyPharmacyFetcher {

    fun fetchAndNotify(context: Context, onPharmaciesFetched: (List<Pharmacy>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val apiKey = context.getString(R.string.collect_api_key)
            CollectApiRetrofitInstance.initialize(apiKey)
            val response = CollectApiRetrofitInstance.api.getDutyPharmacies("Muğla")

            if (response.isSuccessful && response.body()?.success == true) {
                val currentLocation = getCurrentLocation(context)

                val pharmacies = response.body()?.result?.map { dutyPharmacy ->
                    val locationParts = dutyPharmacy.loc.split(",")
                    val latitude = locationParts[0].toDouble()
                    val longitude = locationParts[1].toDouble()
                    val distance = if (currentLocation != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, latitude, longitude, results)
                        results[0].toDouble() // Mesafe kilometre cinsinden
                    } else {
                        0.0
                    }
                    val phoneNumber = "0${dutyPharmacy.phone.replace("-", "").replace(" ", "")}"
                    Pharmacy(dutyPharmacy.name, distance, latitude, longitude, phoneNumber)
                }?.sortedBy { it.distance }

                withContext(Dispatchers.Main) {
                    val nearestPharmacies = pharmacies?.take(5) ?: emptyList()
                    onPharmaciesFetched(nearestPharmacies)
                }

                PreferenceHelper.saveDutyPharmacies(context, pharmacies ?: emptyList())
                PreferenceHelper.saveLastRequestTime(context, System.currentTimeMillis())
            } else {
                Log.e("DutyPharmacyFetcher", "API Call Failed: ${response.message()}")
            }
        }
    }

    private suspend fun getCurrentLocation(context: Context): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("DutyPharmacyFetcher", "Location permission is not granted.")
            return null
        }

        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
