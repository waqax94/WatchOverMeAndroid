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
import android.location.Geocoder
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.telephony.SmsManager
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
    lateinit var watcherRunnable: Runnable
    lateinit var broadcastReceiver: BroadcastReceiver
    lateinit var fusedLocationClient: FusedLocationProviderClient
    var alertLogId = ""
    var serviceId: String? = null
    var wearerFirstName: String? = null
    var wearerLastName: String? = null
    var wearerId: String? = null
    var batteryLevel: String? = null
    var position = 0
    var cycle = 0
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy")
    val timeFormatter = SimpleDateFormat("hh:mm:ss aa")
    val dateFormatter1 = SimpleDateFormat("yyyyddMM")
    val timeFormatter1 = SimpleDateFormat("HHmmss")
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
        wearerLastName = loginData?.getString("wearerLastName","")
        wearerId = loginData?.getString("wearerId","")
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
            if (position < watcherList.size && cycle < 2 && contactWatcherStatus == "Running") {
                iterateWatchers(watcherList)
                position++

                if (position >= watcherList.size) {
                    position = 0
                    cycle++
                }

            } else {

                if (cycle >= 2) {
                    contactWatcherStatus = "Complete"
                }

                val notificationHeader = "Help Me Response"
                var notificationText = ""

                if (contactWatcherStatus == "Complete") {
                    notificationText =
                        wearerFirstName + ", Watch Over Me has contacted all your watchers and none of them responded " +
                                "yet. We recommend you to seek other ways to get help."
                } else if (contactWatcherStatus == "Responded") {
                    notificationText = "$wearerFirstName, help is coming"
                } else if (contactWatcherStatus == "Stopped") {
                    notificationText = "Help Me service has been stopped."
                } else {
                    notificationText = "Help Me service was interrupted."
                }

                val apiService = ServiceBuilder.buildService(APIService::class.java)
                val requestCall =
                    apiService.wearerNotification(serviceId, notificationHeader, notificationText)

                requestCall.enqueue(object : Callback<String> {
                    override fun onResponse(call: Call<String>, response: Response<String>) {

                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {

                    }

                })
                sendBroadcast(Intent().setAction("HelpMeStatus"))
                Handler().postDelayed({
                    stopSelf()
                }, 1200000)
                return@Runnable

            }
            handler.postDelayed(watcherRunnable, 20000)
        }


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
        try {
            handler.removeCallbacks(watcherRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        timeInitiated = ""
        contactWatcherStatus = "Running"

        val notificationHeader = "Help Me Response"
        val notificationText = "Help Me service is now available"

        val apiService = ServiceBuilder.buildService(APIService::class.java)

        val requestCall =
            apiService.deactivateHelpMeRequest(serviceId, notificationHeader, notificationText)

        requestCall.enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {

            }

            override fun onResponse(call: Call<String>, response: Response<String>) {

            }
        })

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

            timeInitiated = timeFormatter.format(timeNow)

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
                        watcherRunnable.run()


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

    fun iterateWatchers(watchers: ArrayList<Watcher>) {


        val notificationHeader = "Help Me Response"
        var notificationText = ""
        val timeNow = Calendar.getInstance().time
        val contactDate = dateFormatter1.format(timeNow)
        val contactTime = timeFormatter1.format(timeNow)
        val alertNum = alertLogId.substring(3)
        val watcherIdNum = watchers[position].watcherId?.substring(6)
        val responseBaseLink = "http://192.168.0.105/hmr/"
        val responseLink = responseBaseLink + alertNum + contactDate + "/" + watcherIdNum + contactTime

        if (cycle == 0) {
            if (position == 0) {
                notificationText =
                    wearerFirstName + ", Watch Over Me team is now contacting " + watchers[position].watcherFirstName +
                            " " + watchers[position].watcherLastName + " through email and SMS. They are number " + (position + 1) + " of " + watchers.size +
                            " possible responding watchers for you. We will contact all of your responding watchers in sequence and keep " +
                            "you informed of the progress."
            } else {
                notificationText =
                    wearerFirstName + ", Watch Over Me team is now contacting " + watchers[position].watcherFirstName +
                            " " + watchers[position].watcherLastName + " through email and SMS. They are number " + (position + 1) + " of " + watchers.size +
                            " possible responding watchers for you."
            }



        }
        else if (cycle == 1) {
            if (position == 0) {
                notificationText =
                    wearerFirstName + ", this is the second and final cycle of this request. Watch Over Me team is now contacting " + watchers[position].watcherFirstName +
                            " " + watchers[position].watcherLastName + " through phone call. They are number " + (position + 1) + " of " + watchers.size +
                            " possible responding watchers for you."
            } else {
                notificationText =
                    wearerFirstName + ", Watch Over Me team is now contacting " + watchers[position].watcherFirstName +
                            " " + watchers[position].watcherLastName + " through phone call. They are number " + (position + 1) + " of " + watchers.size +
                            " possible responding watchers for you."
            }

        }


        val apiService = ServiceBuilder.buildService(APIService::class.java)
        val requestCall = apiService.contactWatcher(serviceId,notificationHeader,notificationText,
        watchers[position].watcherId,cycle.toString(),alertLogId,wearerId,dateFormatter.format(timeNow),timeFormatter.format(timeNow),
        responseLink,watchers[position].watcherPhone,wearerFirstName)

        requestCall.enqueue(object : Callback<ServerResponse> {
            override fun onResponse(
                call: Call<ServerResponse>,
                response: Response<ServerResponse>
            ) {
                val serverResponse = response.body()
                if(serverResponse != null){
                    if(serverResponse.connection!! && serverResponse.queryStatus!! ){
                        contactWatcherStatus = "Responded"
                    }
                    else if(serverResponse.connection!! && !serverResponse.queryStatus!!){
                        iterateWatchers(watchers)
                        position++
                    }
                    else {

                        if(cycle == 0){
//                            val smsText = "Hi, "+ watchers[position].watcherFirstName + " follow the link to help me: \n $responseLink"
//                            Toast.makeText(this@HelpMeService,"Sending Sms",Toast.LENGTH_SHORT).show()
//                            SmsManager.getDefault().sendTextMessage(watchers[position].watcherPhone,null,smsText,null,null)
                        }
                    }
//                    val smsText = "Hi, "+ watchers[position].watcherFirstName + " follow the link to help me: \n $responseLink"
//                    Toast.makeText(this@HelpMeService,"Sending Sms",Toast.LENGTH_SHORT).show()
//                    SmsManager.getDefault().sendTextMessage(watchers[position].watcherPhone,null,smsText,null,null)
                }
                else{

                    contactWatcherStatus = ""
                }
            }

            override fun onFailure(call: Call<ServerResponse>, t: Throwable) {

                contactWatcherStatus = ""
            }

        })
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
    }

}
