package com.uawdevstudios.watchovermeandroid.receivers

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import com.uawdevstudios.watchovermeandroid.services.CustomLocationService
import java.util.*

class RebootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        HelpMeTrigger.schecduleExactAlarm(
            context,
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager,
            5
        )

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.e("Reboot", "Called")

            val loginData =
                context.getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
            val serviceId = loginData?.getString("serviceId", "")

            if(serviceId != "" && serviceId != null){
                startLocationService(context)
            }
        }


    }

    private fun startLocationService(context: Context ) {
        if (!isLocationServiceRunning(context)) {
            val serviceIntent = Intent(context, CustomLocationService::class.java)
            //        this.startService(serviceIntent);
            serviceIntent.action = 1.toString()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    private fun isLocationServiceRunning(context: Context): Boolean {
        val manager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if ("com.uawdevstudios.watchovermeandroid.services.CustomLocationService" == service.service.className) {
                return true
            }
        }
        return false
    }

    companion object {
        const val REBOOT_RECEIVER = "Reboot Receiver"
        fun schecduleExactAlarm(context: Context, alarmManager: AlarmManager, interval: Int){
            val c = Calendar.getInstance()
            c.add(Calendar.SECOND, interval)
            val timeAfter = c.timeInMillis
            val intent = Intent(context, RebootReceiver::class.java)
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            val pendingIntent = PendingIntent.getBroadcast(context,0,intent,0)
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(timeAfter,pendingIntent),pendingIntent)
        }
    }

}
