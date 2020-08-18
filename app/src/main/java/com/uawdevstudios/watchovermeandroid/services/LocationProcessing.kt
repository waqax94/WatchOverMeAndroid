package com.uawdevstudios.watchovermeandroid.services

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.content_fragment_home.view.*
import java.security.Permission
import java.util.*

class LocationProcessing() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var updatedLocation: Location? = null
    val UpdateInverval = 15 * 1000
    val FastestInterval = 2 * 1000


    fun getLocation(activity: Activity): Location? {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)


        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        locationRequest.interval = UpdateInverval.toLong()
        locationRequest.fastestInterval = FastestInterval.toLong()
        if (ActivityCompat.checkSelfPermission(
                activity.applicationContext,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                activity.applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                111
            )
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                updatedLocation = locationResult!!.lastLocation
            }
        }, Looper.myLooper())

        return updatedLocation
    }

}