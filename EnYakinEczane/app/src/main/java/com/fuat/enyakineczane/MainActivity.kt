package com.fuat.enyakineczane

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fuat.enyakineczane.receiver.NotificationReceiver
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val LOCATION_SETTINGS_REQUEST_CODE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.search_button)
        button.setOnClickListener {
            checkLocationPermissionAndServices()
        }

        setDailyPharmacyUpdateAlarm()
    }

    private fun setDailyPharmacyUpdateAlarm() {
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)

        if (pendingIntent == null) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)

                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            val newPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                newPendingIntent
            )
        }
    }

    private fun checkLocationPermissionAndServices() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            checkLocationServices()
        }
    }

    private fun checkLocationServices() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(this, "Konum servisleri kapalı. Lütfen ayarlardan izin verin.", Toast.LENGTH_LONG).show()
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE)
            }, 3000)
        } else {
            startNearestPharmaciesActivity(refresh = true)
        }
    }

    private fun startNearestPharmaciesActivity(refresh: Boolean) {
        val intent = Intent(this, NearestPharmaciesActivity::class.java)
        intent.putExtra("refresh", refresh)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationServices()
            } else {
                Toast.makeText(this, "Konum izni verilmedi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isGpsEnabled || isNetworkEnabled) {
                Toast.makeText(this, "En yakın eczaneler yükleniyor...", Toast.LENGTH_SHORT).show()
                startNearestPharmaciesActivity(refresh = true)
            } else {
                Toast.makeText(this, "Konum servisleri hala kapalı.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
