package com.uawdevstudios.watchovermeandroid.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*


class CustomLocationService : Service() {
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Watch Over Me Service")
                .setContentText("Location service is running").build()
            notification.flags = Notification.FLAG_NO_CLEAR
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: called.")
        location
        return START_NOT_STICKY
    }

    private val location:


            Unit
        get() {
            val mLocationRequestHighAccuracy = LocationRequest()
            mLocationRequestHighAccuracy.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            mLocationRequestHighAccuracy.interval = UPDATE_INTERVAL
            mLocationRequestHighAccuracy.fastestInterval = FASTEST_INTERVAL


            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(
                    TAG,
                    "getLocation: stopping the location service."
                )
                stopSelf()
                return
            }
            Log.d(
                TAG,
                "getLocation: getting location information."
            )

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    Log.d(
                        TAG,
                        "onLocationResult: got location result."
                    )
                    val location = locationResult.lastLocation
                    if (location != null) {
                        Log.d(
                            TAG,
                            "Latitude: " + location.latitude
                        )
                    }
                }
            }

            mFusedLocationClient!!.requestLocationUpdates(
                mLocationRequestHighAccuracy,
                locationCallback,
                Looper.myLooper()
            )
        }

    override fun onDestroy() {
        try{
            mFusedLocationClient?.removeLocationUpdates(locationCallback)
        }
        catch(e: Exception){
            e.printStackTrace()
        }
        Log.d("Service Destroy", "Service Destroyed")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LocationService"
        private const val UPDATE_INTERVAL = 4 * 1000 /* 4 secs */.toLong()
        private const val FASTEST_INTERVAL: Long = 2000 /* 2 sec */
    }
}