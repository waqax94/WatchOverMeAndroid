package com.uawdevstudios.watchovermeandroid.services

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.uawdevstudios.watchovermeandroid.activities.MainActivity
import com.uawdevstudios.watchovermeandroid.activities.SplashScreen
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class CustomLocationService : Service() {
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    var batteryLevel: String? = null
    var serviceId: String? = null
    lateinit var broadcastReceiver: BroadcastReceiver
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy")
    val timeFormatter = SimpleDateFormat("hh:mm:ss aa")

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val loginData =
            getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        serviceId = loginData?.getString("serviceId", "")
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val intent = Intent(this, SplashScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, 0)

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0).toString()
            }
        }

        registerReceiver(
            broadcastReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "watch_over_me"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Watch Over Me",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Watch Over Me Service")
                .setContentText("Location service is running")
                .setContentIntent(pendingIntent)
                .build()
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: called.")
        location
        return START_STICKY
    }

    private val location:


            Unit
        get() {
            val mLocationRequestHighAccuracy = LocationRequest()
            mLocationRequestHighAccuracy.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            mLocationRequestHighAccuracy.interval = UPDATE_INTERVAL
            mLocationRequestHighAccuracy.fastestInterval = FASTEST_INTERVAL


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
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
            }
            else {
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
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {

                    val timeNow = Calendar.getInstance().time

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

                        FileService(this@CustomLocationService).removeNotifications()

                        val apiService = ServiceBuilder.buildService(APIService::class.java)
                        val requestCall = apiService.regularLog(
                            batteryLevel,
                            location.latitude.toString(),
                            location.longitude.toString(),
                            reverseGeoCoder(location),
                            "Regularly timed log",
                            dateFormatter.format(timeNow),
                            timeFormatter.format(timeNow),
                            "Hourly Log",
                            serviceId
                        )

                        requestCall.enqueue(object : Callback<String> {
                            override fun onResponse(
                                call: Call<String>,
                                response: Response<String>
                            ) {
                                Log.d("Regular Log Response", response.body().toString())
                            }

                            override fun onFailure(call: Call<String>, t: Throwable) {
                                Log.d("Regular Log Response", "Error")
                            }

                        })

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
        try {
            mFusedLocationClient?.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("Service Destroy", "Service Destroyed")
        super.onDestroy()
    }

    fun reverseGeoCoder(location: Location?): String {
        var locaity = ""
        val gc = Geocoder(this, Locale.getDefault())
        try {
            val addresses = gc.getFromLocation(location!!.latitude, location.longitude, 2)
            val address = addresses[0]
            locaity = address.locality + ", " + address.adminArea + ", " + address.countryName
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return locaity
    }

    companion object {
        private const val TAG = "LocationService"
        private const val UPDATE_INTERVAL = 30 * 60 * 1000.toLong()
        private const val FASTEST_INTERVAL: Long = 29 * 60 * 1000 /* 2 sec */
    }
}