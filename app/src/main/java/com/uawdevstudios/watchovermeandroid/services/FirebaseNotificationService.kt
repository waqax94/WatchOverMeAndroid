package com.uawdevstudios.watchovermeandroid.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.activities.MainActivity
import com.uawdevstudios.watchovermeandroid.models.NotificationItem
import com.uawdevstudios.watchovermeandroid.services.HelpMeService.Companion.HELP_ME_TRIGGER
import kotlinx.android.synthetic.main.content_fragment_home.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class FirebaseNotificationService : FirebaseMessagingService() {

    val dateFormatter = SimpleDateFormat("dd MMMM yyyy")
    val timeFormatter = SimpleDateFormat("hh:mm:ss aa")
    val CHANNEL_ID = "Urgent"


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("TOKEN", token)
        updateTokenOnServer(token)
    }

    private fun updateTokenOnServer(token: String) {
        val loginData =
            applicationContext.getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        val serviceId = loginData.getString("serviceId", "")
        val apiService = ServiceBuilder.buildService(APIService::class.java)
        val requestCall = apiService.updateDeviceToken(serviceId, token)

        requestCall.enqueue(object : Callback<String> {

            override fun onResponse(call: Call<String>, response: Response<String>) {

            }

            override fun onFailure(call: Call<String>, t: Throwable) {

            }

        })
    }

    override fun onMessageReceived(message: RemoteMessage) {
        createNotificationChannel()

        if (message.notification == null) {
            saveDataNotification(
                message,
                message.data["title"].toString(),
                message.data["body"].toString()
            )
        } else {
            showNotification(message)
        }
    }


    private fun showNotification(message: RemoteMessage) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        var notificationTitle = message.notification?.title
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, 0)

        if (notificationTitle.toString().substring(0, 8) == "Location") {
            notificationTitle = "Location Request"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setAutoCancel(true)
            .setContentTitle(notificationTitle)
            .setContentText(message.notification?.body)
            .setSmallIcon(R.drawable.app_notification_icon)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setContentIntent(pendingIntent)

        val i = Intent(applicationContext,LocationBroadcast::class.java)
        i.putExtra("message",message.notification?.title.toString())
        i.flags = Intent.FLAG_RECEIVER_FOREGROUND
        sendBroadcast(i)


        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, builder.build())
    }

    private fun saveDataNotification(message: RemoteMessage, title: String, data: String) {

        val timeNow = Calendar.getInstance().time

        val notification = NotificationItem(
            message.data["body"].toString(),
            dateFormatter.format(timeNow),
            timeFormatter.format(timeNow)
        )

        FileService(this).saveNotification(notification)

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(applicationContext)
        val apiService = ServiceBuilder.buildService(APIService::class.java)
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val loginData =
            applicationContext.getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        val serviceId = loginData?.getString("serviceId", "")

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            if (message.data["title"].toString().substring(0, 8) != "Location") {
                return@addOnSuccessListener
            }

            val requestCall = apiService.sendLocation(
                serviceId,
                message.data["title"].toString().substring(8),
                location?.latitude.toString(),
                location?.longitude.toString(),
                reverseGeoCoder(location)
            )

            requestCall.enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.body() != "done") {
                        Log.e("Location Push", "Error in location sending")
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.e("Location Push", "Error in location sending")
                }

            })

        }

        sendBroadcast(Intent().setAction("NewNotification"))
    }

    fun reverseGeoCoder(location: Location?): String {
        val gc = Geocoder(applicationContext, Locale.getDefault())
        try {
            val addresses = gc.getFromLocation(location!!.latitude, location.longitude, 2)
            val address = addresses[0]
            return address.locality + ", " + address.adminArea + ", " + address.countryName
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return ""
    }

    @SuppressLint("WrongConstant")
    private fun createNotificationChannel() {

        val powerManager =
            applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "notification :LOCATION_TRIGGER"
        )

        wakeLock.acquire(60 * 1000L)
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "WOM"
            val descriptionText = "WOM Notification"
            val importance = NotificationManager.IMPORTANCE_MAX
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}