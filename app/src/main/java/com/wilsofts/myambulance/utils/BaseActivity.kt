package com.wilsofts.myambulance.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

open class BaseActivity : AppCompatActivity() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var locationLauncher: ActivityResultLauncher<IntentSenderRequest>

    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e., how often you should receive updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient has a new Location
    private lateinit var locationCallback: LocationCallback

    private val gpsSwitchStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent.action) {
                // Make an action or refresh an already managed state.
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                if (isGpsEnabled || isNetworkEnabled) {
                    Log.e("Location", "Location is enabled : isGpsEnabled = $isGpsEnabled isNetworkEnabled = $isNetworkEnabled")
                } else {
                    Log.e("Location", "Location disabled ")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register the permissions callback, which handles the user's response to the system permissions dialog.
        this.requestPermissionLauncher =
            this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                var falseCount = 0
                permissions.entries.forEach {
                    if (it.value == false) {
                        falseCount++
                    }
                }
                if (falseCount > 0) {
                    Toast.makeText(this, "Location permissions denied, application is exiting", Toast.LENGTH_LONG).show()
                    this.finish()
                } else {
                    LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("location-initialised"))
                }
            }

        /*The launcher to handle GPS, once on, ask for location permissions, else exit application*/
        this.locationLauncher = this.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                this.initPermissions()
            } else {
                Utils.showToast(this, "GPS Not enabled, application is exiting")
                this.finish()
            }
        }
        this.initLocation()
    }

    /*step 1, initialise location requests*/
    private fun initLocation() {
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        this.locationRequest = LocationRequest.create().apply {
            // Sets the desired interval for active location updates. This interval is inexact.
            this.interval = TimeUnit.SECONDS.toMillis(20)
            // Sets the fastest rate for active location updates.
            // This interval is exact, and your application will never receive updates more frequently than this value
            this.fastestInterval = TimeUnit.SECONDS.toMillis(5)
            // Sets the maximum time when batched location updates are delivered. Updates may be delivered sooner than this interval
            this.maxWaitTime = TimeUnit.SECONDS.toMillis(30)
            this.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

            this.smallestDisplacement = 50F
        }

        this.locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation.let {
                    LocalBroadcastManager.getInstance(this@BaseActivity).sendBroadcast(
                        Intent("location-updates").putExtra("location", it)
                    )
                }
            }
        }
    }

    /*step 1, turn on gps*/
    private fun turnOnGPS() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(this.locationRequest)

        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
        result.addOnCompleteListener {
            try {
                result.getResult(Exception::class.java)
                // All location settings are satisfied. The client can initialize location requests here.
                this.initPermissions()
            } catch (exception: Exception) {
                Utils.logE("GPS Exception", exception.localizedMessage ?: "GPS Error", exception)
                if (exception is ResolvableApiException) {
                    try {
                        this.locationLauncher.launch(IntentSenderRequest.Builder(exception.resolution).build())
                    } catch (throwable: Throwable) {
                        Utils.logE("GPS Throwable", throwable.localizedMessage ?: "GPS Error", throwable)
                        Utils.showToast(this, "Your device does not support location, application is exiting")
                        this.finish()
                    }
                } else {
                    Utils.showToast(this, "Your device does not support location, application is exiting")
                    this.finish()
                }
            }
        }
    }

    /*step 3, request for location permissions*/
    private fun isLocationPermissionGranted(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun initPermissions() {
        fun requestPermissions() {
            this.requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }

        if (!this.isLocationPermissionGranted()) {
            if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            ) {
                AlertDialog.Builder(this)
                    .setMessage("In order to get actual locations, please grant the application location permissions")
                    .setTitle("Permission Request")
                    .setPositiveButton("Grant") { dialog, _ ->
                        dialog.dismiss()
                        requestPermissions()
                    }
                    .setNegativeButton("Deny") { dialog, _ ->
                        dialog.dismiss()
                        this.finish()
                    }
                    .create()
                    .show()
            } else {
                requestPermissions()
            }
        } else {
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("location-initialised"))
        }
    }

    /*step 4, get last location*/
    @SuppressLint("MissingPermission")
    fun getLastLocation(lastLocation: LastLocation) {
        if (isLocationPermissionGranted()) {
            LocationServices.getFusedLocationProviderClient(this).lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        lastLocation.locationReceived(it)
                    }
                }
                .addOnFailureListener { e ->
                    Utils.logE("Location error", e.localizedMessage ?: "Location error", e)
                    Utils.showToast(this, e.localizedMessage ?: "Location error")
                }
        }
    }

    /*location updates*/
    @SuppressLint("MissingPermission")
    private fun startUpdates() {
        if (isLocationPermissionGranted()) {
            this.fusedLocationProviderClient.requestLocationUpdates(this.locationRequest, this.locationCallback, Looper.myLooper()!!)
        }
    }

    private fun stopUpdates() {
        val removeTask = this.fusedLocationProviderClient.removeLocationUpdates(this.locationCallback)
        removeTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.e("Updates", "Location Callback removed.")
            } else {
                Log.e("Updates", "Failed to remove Location Callback.")
            }
        }
    }

    /*overridden methods*/
    override fun onResume() {
        super.onResume()
        AppPrefs.updateFCMToken()
        LocalBroadcastManager.getInstance(this).registerReceiver(this.gpsSwitchStateReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        )
        this.turnOnGPS()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.gpsSwitchStateReceiver)
    }

    companion object {
        fun getLocationDetails(latitude: Double, longitude: Double, context: Context): Utils.GeocodedAddress {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 1)

                if (addresses.isNotEmpty()) {
                    if (addresses[0].maxAddressLineIndex >= 0) {
                        return Utils.GeocodedAddress(latitude = latitude, longitude = longitude, placeName = addresses[0].getAddressLine(0))
                    }
                }
            } catch (ioException: IOException) {
                Log.e("Location", ioException.localizedMessage, ioException)
            }
            return Utils.GeocodedAddress(latitude = latitude, longitude = longitude, placeName = "")
        }

        fun getUrl(start: LatLng, end: LatLng, direction_mode: String = "driving"): String {
            val parameters = "origin=${start.latitude},${start.longitude}&destination=${end.latitude},${end.longitude}&mode=$direction_mode"
            val output = "json"
            //todo this api is being paid for
            val directionApi = "AIzaSyBeD6jdx3b4ZVGzjTciOmI1_GGdgPWlr3M"
            return "https://maps.googleapis.com/maps/api/directions/$output?$parameters&key=$directionApi&alternatives=true"
        }
    }

    interface LastLocation {
        fun locationReceived(location: Location)
    }
}