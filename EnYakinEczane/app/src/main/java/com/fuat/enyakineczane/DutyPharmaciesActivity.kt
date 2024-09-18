package com.fuat.enyakineczane

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuat.enyakineczane.adapter.PharmacyAdapter
import com.fuat.enyakineczane.retrofit.CollectApiRetrofitInstance
import com.fuat.enyakineczane.retrofit.Pharmacy
import com.fuat.enyakineczane.receiver.NotificationReceiver
import com.fuat.enyakineczane.shared_preferences.PreferenceHelper
import com.fuat.utils.LocationUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DutyPharmaciesActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var dateText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_duty_pharmacies)

        progressBar = findViewById(R.id.progressBar)
        loadingText = findViewById(R.id.loadingText)
        recyclerView = findViewById(R.id.recyclerView)
        dateText = findViewById(R.id.dateText)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val apiKey = getString(R.string.collect_api_key)
        CollectApiRetrofitInstance.initialize(apiKey)

        showLoading()
        getCurrentLocation()
        setDate()

        setDailyPharmacyUpdateAlarm()
    }

    private fun setDate() {
        val sdf = SimpleDateFormat("dd MMMM yyyy EEEE", Locale("tr"))
        val currentDate = sdf.format(Date())
        dateText.text = "$currentDate Tarihli Nöbetçi Eczaneler"
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        loadingText.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun getCurrentLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                currentLocation = location
                checkAndFetchDutyPharmacies()
            }
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun checkAndFetchDutyPharmacies() {
        val lastRequestTime = PreferenceHelper.getLastRequestTime(this)
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursInMillis = TimeUnit.HOURS.toMillis(24)

        if (currentTime - lastRequestTime >= twentyFourHoursInMillis) {
            getDutyPharmacies()
        } else {
            val cachedPharmacies = PreferenceHelper.getDutyPharmacies(this)
            if (cachedPharmacies != null) {
                setupRecyclerView(cachedPharmacies)
                hideLoading()
            } else {
                getDutyPharmacies()
            }
        }
    }

    private fun getDutyPharmacies() {
        lifecycleScope.launch {
            val cityName = currentLocation?.let {
                LocationUtils.getCityNameFromLocation(this@DutyPharmaciesActivity, it)
            } ?: "İstanbul" // Şehir bulunamazsa varsayılan olarak "İstanbul" kullanılacak

            val response = CollectApiRetrofitInstance.api.getDutyPharmacies(cityName)
            if (response.isSuccessful && response.body()?.success == true) {
                val pharmacies = response.body()?.result?.map { dutyPharmacy ->
                    val locationParts = dutyPharmacy.loc.split(",")
                    val latitude = locationParts[0].toDouble()
                    val longitude = locationParts[1].toDouble()
                    val distance = if (currentLocation != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(currentLocation!!.latitude, currentLocation!!.longitude, latitude, longitude, results)
                        results[0].toDouble()
                    } else {
                        0.0
                    }
                    val phoneNumber = "0${dutyPharmacy.phone.replace("-", "").replace(" ", "")}"
                    Pharmacy(dutyPharmacy.name, distance, latitude, longitude, phoneNumber)
                }?.sortedBy { it.distance }
                val nearestPharmacies = pharmacies?.take(5) ?: emptyList()
                setupRecyclerView(nearestPharmacies)
                PreferenceHelper.saveDutyPharmacies(this@DutyPharmaciesActivity, nearestPharmacies)
                PreferenceHelper.saveLastRequestTime(this@DutyPharmaciesActivity, System.currentTimeMillis())
            } else {
                Log.e("DutyPharmaciesActivity", "API Call Failed: ${response.message()}")
            }
            hideLoading()
        }
    }


    private fun setupRecyclerView(pharmacyList: List<Pharmacy>) {
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = PharmacyAdapter(pharmacyList) { pharmacy ->
        }
        recyclerView.adapter = adapter
    }

    private fun setDailyPharmacyUpdateAlarm() {
        val alarmIntent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        // Günlük 18:30'da tetiklenecek alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
