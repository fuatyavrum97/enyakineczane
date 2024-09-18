package com.fuat.enyakineczane.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.fuat.enyakineczane.retrofit.CollectApiRetrofitInstance
import com.fuat.enyakineczane.retrofit.Pharmacy
import com.fuat.enyakineczane.shared_preferences.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            if (shouldFetchDutyPharmacies(it)) {
                fetchDutyPharmacies(it)
            }
        }
    }

    private fun shouldFetchDutyPharmacies(context: Context): Boolean {
        val lastRequestTime = PreferenceHelper.getLastRequestTime(context)
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursInMillis = TimeUnit.HOURS.toMillis(24)
        return currentTime - lastRequestTime >= twentyFourHoursInMillis
    }

    private fun fetchDutyPharmacies(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = CollectApiRetrofitInstance.api.getDutyPharmacies("Muğla")
            if (response.isSuccessful && response.body()?.success == true) {
                val pharmacies = response.body()?.result?.map { dutyPharmacy ->
                    val locationParts = dutyPharmacy.loc.split(",")
                    val latitude = locationParts[0].toDouble()
                    val longitude = locationParts[1].toDouble()
                    val distance = 0.0
                    val phoneNumber = "0${dutyPharmacy.phone.replace("-", "").replace(" ", "")}"
                    Pharmacy(dutyPharmacy.name, distance, latitude, longitude, phoneNumber)
                }?.sortedBy { it.distance }

                PreferenceHelper.saveDutyPharmacies(context, pharmacies ?: emptyList())
                PreferenceHelper.saveLastRequestTime(context, System.currentTimeMillis())

                sendNotification(context)
            } else {
                Log.e("NotificationReceiver", "API Call Failed: ${response.message()}")
            }
        }
    }

    private fun sendNotification(context: Context) {
    }
}
