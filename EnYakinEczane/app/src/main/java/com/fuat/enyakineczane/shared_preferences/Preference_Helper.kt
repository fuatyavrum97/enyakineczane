package com.fuat.enyakineczane.shared_preferences

import android.content.Context
import android.content.SharedPreferences
import com.fuat.enyakineczane.retrofit.Pharmacy
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PreferenceHelper {
    private const val PREF_NAME = "DutyPharmacyPreferences"
    private const val KEY_LAST_REQUEST_TIME = "last_request_time"
    private const val KEY_DUTY_PHARMACIES = "duty_pharmacies"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveLastRequestTime(context: Context, time: Long) {
        getPreferences(context).edit().putLong(KEY_LAST_REQUEST_TIME, time).apply()
    }

    fun getLastRequestTime(context: Context): Long {
        return getPreferences(context).getLong(KEY_LAST_REQUEST_TIME, 0)
    }

    fun saveDutyPharmacies(context: Context, pharmacies: List<Pharmacy>) {
        val gson = Gson()
        val json = gson.toJson(pharmacies)
        getPreferences(context).edit().putString(KEY_DUTY_PHARMACIES, json).apply()
    }

    fun getDutyPharmacies(context: Context): List<Pharmacy>? {
        val gson = Gson()
        val json = getPreferences(context).getString(KEY_DUTY_PHARMACIES, null)
        return if (json != null) {
            val type = object : TypeToken<List<Pharmacy>>() {}.type
            gson.fromJson(json, type)
        } else {
            null
        }
    }
}
