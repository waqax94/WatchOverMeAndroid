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
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.uawdevstudios.watchovermeandroid.models.ServerResponse
import com.uawdevstudios.watchovermeandroid.models.Watcher
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class HelpMeService : Service() {

    lateinit var broadcastReceiver: BroadcastReceiver
    lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val loginData =
            getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        context = applicationContext
        serviceId = loginData?.getString("serviceId", "")
        wearerFirstName = loginData?.getString("wearerFirstName", "")
        wearerLastName = loginData?.getString("wearerLastName", "")
        wearerId = loginData?.getString("wearerId", "")
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
            val CHANNEL_ID = "watch_over_me_help_me"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Watch Over Me (Help Me)",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Watch Over Me")
                .setContentText("Help me request is currently in progress.").build()
            notification.flags = Notification.FLAG_NO_CLEAR
            startForeground(2, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        timeInitiated = ""
        contactWatcherStatus = "Running"
        initiateService()
        return START_NOT_STICKY
    }

    override fun onDestroy() {

        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Log.e("Help Me Service", "Destroyed")

        watcherList.clear()
        timeInitiated = ""
        contactWatcherStatus = "Running"
        context = null
        watcherList = ArrayList<Watcher>()
        alertLogId = ""
        serviceId= null
        wearerFirstName = null
        wearerLastName = null
        wearerId = null
        batteryLevel = null
        position = 0
        cycle = 0
        stopMe = false

        sendBroadcast(Intent().setAction("HelpMeStatus"))
    }

    fun initiateService() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Please make sure you location is on", Toast.LENGTH_SHORT).show()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            val timeNow = Calendar.getInstance().time

            timeInitiated = dateTimeFormatter.format(timeNow)

            val apiService = ServiceBuilder.buildService(APIService::class.java)
            val requestCall = apiService.helpMeRequestInitiate(
                batteryLevel,
                location?.latitude.toString(),
                location?.longitude.toString(),
                reverseGeoCoder(location),
                "Help Me request is initiated for you " + wearerFirstName,
                dateFormatter.format(timeNow),
                timeFormatter.format(timeNow),
                "Alert Log",
                serviceId
            )

            requestCall.enqueue(object : Callback<ServerResponse> {

                override fun onResponse(
                    call: Call<ServerResponse>,
                    response: Response<ServerResponse>
                ) {
                    val serverResponse = response.body()
                    if (serverResponse != null) {
                        alertLogId = serverResponse.message.toString()
                        val watcherListType = object : TypeToken<ArrayList<Watcher>>() {}.type

                        val dataList =
                            Gson().fromJson(
                                serverResponse.data.toString(),
                                watcherListType
                            ) as ArrayList<Watcher>

                        for (data in dataList) {
                            data.watcherPriorityNum =
                                data.watcherPriorityNum!!.toFloat().roundToInt().toString()
                            watcherList.add(data)
                        }

                        sendBroadcast(Intent().setAction("HelpMeStatus"))
                        HelpMeTrigger.schecduleExactAlarm(applicationContext,getSystemService(ALARM_SERVICE) as AlarmManager, 1)

                    } else {
                        stopSelf()
                    }
                }

                override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
                    stopSelf()
                }

            })

        }
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
        var timeInitiated = ""
        var contactWatcherStatus = "Running"
        var context: Context? = null
        var watcherList = ArrayList<Watcher>()
        var alertLogId = ""
        var serviceId: String? = null
        var wearerFirstName: String? = null
        var wearerLastName: String? = null
        var wearerId: String? = null
        var batteryLevel: String? = null
        var position = 0
        var cycle = 0
        var stopMe = false
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy")
        val timeFormatter = SimpleDateFormat("hh:mm:ss aa")
        val dateTimeFormatter = SimpleDateFormat("dd MM yyyy hh:mm:ss aa")
        val dateFormatter1 = SimpleDateFormat("yyyyddMM")
        val timeFormatter1 = SimpleDateFormat("HHmmss")


    }

}
