package com.uawdevstudios.watchovermeandroid.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.View
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
import kotlinx.android.synthetic.main.activity_splash_screen.*
import kotlinx.android.synthetic.main.content_fragment_home.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class HelpMeService : Service() {

    var watcherList = ArrayList<Watcher>()
    lateinit var apiService: APIService
    lateinit var watcherRunnable: Runnable
    lateinit var broadcastReceiver: BroadcastReceiver
    lateinit var fusedLocationClient: FusedLocationProviderClient
    var serviceId: String? = null
    var wearerFirstName: String? = null
    var batteryLevel: String? = null
    var position = 0
    var cycle = 0
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy")
    val timeFormatter = SimpleDateFormat("hh:mm:ss aa")
    val calendar = Calendar.getInstance()
    val handler = Handler()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val loginData =
            getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        serviceId = loginData?.getString("serviceId", "")
        wearerFirstName = loginData?.getString("wearerFirstName", "")
        apiService = ServiceBuilder.buildService(APIService::class.java)

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0).toString()
            }
        }

        registerReceiver(
            broadcastReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )


        watcherRunnable = Runnable {
            if (position < watcherList.size && requestStatus == true) {
                iterateWatchers(watcherList, position)
                position++
            } else {
                stopSelf()
            }
            handler.postDelayed(watcherRunnable, 20000)
        }

        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_02"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Channel 2",
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
        Toast.makeText(this, "Request initiated", Toast.LENGTH_SHORT).show()
        initiateService()

        Handler().postDelayed({
            watcherRunnable.run()
        }, 5000)
        requestStatus = false
        return START_NOT_STICKY
    }

    override fun onDestroy() {

        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val requestCall = apiService.deactivateHelpMeRequest(serviceId)

        requestCall.enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {

            }

            override fun onResponse(call: Call<String>, response: Response<String>) {

            }

        })
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

            val timeNow = calendar.time

            val requestCall = apiService.helpMeRequestInitiate(
                batteryLevel,
                location?.latitude.toString(),
                location?.longitude.toString(),
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
                        requestStatus = true
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

    fun iterateWatchers(wachers: ArrayList<Watcher>, position: Int) {
        Log.d("Help me request", wachers[position].toString())
    }


    companion object {
        var requestStatus: Boolean = false
    }

}
