package com.fuat.enyakineczane

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.fuat.enyakineczane.adapter.PharmacyAdapter
import com.fuat.enyakineczane.retrofit.Pharmacy
import com.fuat.enyakineczane.retrofit.RetrofitInstance
import com.google.android.gms.location.*
import kotlinx.coroutines.launch

class NearestPharmaciesActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var dutyButton: Button
    private var shouldRefresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearest_pharmacies)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        progressBar = findViewById(R.id.progressBar)
        loadingText = findViewById(R.id.loadingText)
        recyclerView = findViewById(R.id.recyclerView)
        dutyButton = findViewById(R.id.dutyButton)

        shouldRefresh = intent.getBooleanExtra("refresh", false)

        swipeRefreshLayout.setOnRefreshListener {
            Log.d("NearestPharmaciesActivity", "Swipe to refresh triggered.")
            // Swipe refresh başladığında swipe refresh indicator'ını durdur ve progress barı göster
            swipeRefreshLayout.isRefreshing = false
            showLoading()
            getCurrentLocation()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            if (shouldRefresh) {
                showLoading()
                getCurrentLocation()
            }
        }

        // Konum istekleri için ayarları yapılandırıyoruz
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Konum güncellemelerini almak için geri çağırma fonksiyonu
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    fusedLocationClient.removeLocationUpdates(this)
                    searchNearbyPharmacies(location)
                }
            }
        }

        dutyButton.setOnClickListener {
            val intent = Intent(this, DutyPharmaciesActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefresh) {
            showLoading()
            getCurrentLocation()
            shouldRefresh = false
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    showLoading()
                    searchNearbyPharmacies(location)
                } else {
                    requestLocationUpdates()
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        dutyButton.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        loadingText.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        dutyButton.visibility = View.VISIBLE
    }

    private fun searchNearbyPharmacies(location: Location) {
        val apiKey = getString(R.string.google_maps_key)
        val locationString = "${location.latitude},${location.longitude}"
        val type = "pharmacy"
        val rankBy = "distance"

        lifecycleScope.launch {
            val response = RetrofitInstance.api.getNearbyPharmacies(locationString, rankBy, type, apiKey)
            if (response.isSuccessful) {
                val body = response.body()
                val pharmacies = body?.results?.map { result ->
                    var name = result.name
                    if (name.contains("Pharmacy", ignoreCase = true)) {
                        name = name.replace("(?i)Pharmacy".toRegex(), "").trim() + " Eczanesi"
                    }
                    val distance = FloatArray(1)
                    Location.distanceBetween(location.latitude, location.longitude, result.geometry.location.lat, result.geometry.location.lng, distance)
                    val distanceInKm = distance[0] / 1000.0
                    val phoneNumber = getPhoneNumber(result.place_id, apiKey)
                    val pharmacy = Pharmacy(name, distanceInKm, result.geometry.location.lat, result.geometry.location.lng, phoneNumber)
                    pharmacy
                }?.sortedBy { it.distance }
                val nearestPharmacies = pharmacies?.take(5) ?: emptyList()
                setupRecyclerView(nearestPharmacies)
            } else {
                Log.e("NearestPharmaciesActivity", "API Call Failed: ${response.message()}")
            }
            hideLoading()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private suspend fun getPhoneNumber(placeId: String, apiKey: String): String? {
        val response = RetrofitInstance.api.getPlaceDetails(placeId, apiKey)
        return if (response.isSuccessful) {
            response.body()?.result?.international_phone_number
        } else {
            null
        }
    }

    private fun setupRecyclerView(pharmacyList: List<Pharmacy>) {
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = PharmacyAdapter(pharmacyList) { pharmacy ->
            openGoogleMaps(pharmacy.latitude, pharmacy.longitude)
        }
        recyclerView.adapter = adapter
    }

    private fun openGoogleMaps(latitude: Double, longitude: Double) {
        val uri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=w")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            Log.e("NearestPharmaciesActivity", "Location permission denied.")
        }
    }
}
