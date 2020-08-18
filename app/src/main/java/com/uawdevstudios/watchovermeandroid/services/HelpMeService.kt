package com.uawdevstudios.watchovermeandroid.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.uawdevstudios.watchovermeandroid.models.ServerResponse
import com.uawdevstudios.watchovermeandroid.models.Watcher
import kotlinx.android.synthetic.main.activity_splash_screen.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt

class HelpMeService : Service() {

    var watcherList = ArrayList<Watcher>()
    lateinit var apiService: APIService
    lateinit var watcherRunnable: Runnable
    var serviceId: String? = null
    var position = 0
    val handler = Handler()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val loginData =
            getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        serviceId = loginData?.getString("serviceId", "")
        apiService = ServiceBuilder.buildService(APIService::class.java)

        watcherRunnable = Runnable {
            if(position < watcherList.size && requestStatus == true){
                iterateWatchers(watcherList,position)
                position++
            }
            else{
                stopSelf()
            }
            handler.postDelayed(watcherRunnable,20000)
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
        loadWatchers()

        Handler().postDelayed({
            watcherRunnable.run()
        }, 5000)
        requestStatus = false
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        val requestCall = apiService.deactivateHelpMeRequest(serviceId)

        requestCall.enqueue(object : Callback<String>{
            override fun onFailure(call: Call<String>, t: Throwable) {

            }

            override fun onResponse(call: Call<String>, response: Response<String>) {

            }

        })
    }

    fun loadWatchers() {
        val requestCall = apiService.getWatchers(serviceId)

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

    fun iterateWatchers(wachers: ArrayList<Watcher>, position: Int) {
        Log.d("Help me request", wachers[position].toString())
    }


    companion object {
        var requestStatus: Boolean = false
    }

}
