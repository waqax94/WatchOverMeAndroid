package com.uawdevstudios.watchovermeandroid.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.activities.MainActivity
import com.uawdevstudios.watchovermeandroid.models.NotificationItem
import com.uawdevstudios.watchovermeandroid.receivers.LocationBroadcast
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

        if (message.notification == null) {
            saveDataNotification(message)
        } else {
            showNotification(message)
        }
    }


    private fun showNotification(message: RemoteMessage) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val notificationTitle = message.notification?.title
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, 0)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setAutoCancel(true)
            .setContentTitle(notificationTitle)
            .setContentText(message.notification?.body)
            .setSmallIcon(R.drawable.app_notification_icon)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setContentIntent(pendingIntent)


        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, builder.build())
    }

    private fun saveDataNotification(message: RemoteMessage) {

        val timeNow = Calendar.getInstance().time

        val notification = NotificationItem(
            message.data["body"].toString(),
            dateFormatter.format(timeNow),
            timeFormatter.format(timeNow)
        )

        FileService(this).saveNotification(notification)

        val i = Intent(applicationContext, LocationBroadcast::class.java)
        i.putExtra("message",message.data["title"].toString())
        i.flags = Intent.FLAG_RECEIVER_FOREGROUND
        sendBroadcast(i)

        sendBroadcast(Intent().setAction("NewNotification"))
    }


}