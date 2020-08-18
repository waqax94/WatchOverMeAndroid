package com.uawdevstudios.watchovermeandroid.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.activities.MainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FirebaseNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("TOKEN",token)
        updateTokenOnServer(token)
    }

    private fun updateTokenOnServer(token: String){
        val loginData =
            applicationContext.getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        val serviceId = loginData.getString("serviceId", "")
        val apiService = ServiceBuilder.buildService(APIService::class.java)
        val requestCall = apiService.updateDeviceToken(serviceId,token)

        requestCall.enqueue(object: Callback<String> {

            override fun onResponse(call: Call<String>, response: Response<String>) {

            }

            override fun onFailure(call: Call<String>, t: Throwable) {

            }

        })
    }

    override fun onMessageReceived(message: RemoteMessage) {
        showNotification(message)
    }

    private fun showNotification(message: RemoteMessage) {
        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        val pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this)
            .setAutoCancel(true)
            .setContentTitle(message.notification?.title)
            .setContentText(message.data["message"])
            .setSmallIcon(R.drawable.app_notification_icon)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0,builder.build())
    }

}