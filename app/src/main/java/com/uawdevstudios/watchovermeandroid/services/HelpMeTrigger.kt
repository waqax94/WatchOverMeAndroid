package com.uawdevstudios.watchovermeandroid.services

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.uawdevstudios.watchovermeandroid.models.ServerResponse
import com.uawdevstudios.watchovermeandroid.models.Watcher
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HelpMeTrigger: BroadcastReceiver() {


    @SuppressLint("InvalidWakeLockTag")
    override fun onReceive(context: Context?, p1: Intent?) {


        val powerManager = context!!.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,HELP_ME_TRIGGER)
        wakeLock.acquire()

        Log.e("Alarm Fired at: ", Calendar.getInstance().time.toString())
        val handler = Handler()
        val watcherRunnable = Runnable {

            if(HelpMeService.stopMe){
                val apiService = ServiceBuilder.buildService(APIService::class.java)

                val requestCall =
                    apiService.deactivateHelpMeRequest(HelpMeService.serviceId)

                requestCall.enqueue(object : Callback<String> {
                    override fun onFailure(call: Call<String>, t: Throwable) {

                    }

                    override fun onResponse(call: Call<String>, response: Response<String>) {

                    }
                })
                stopHelpMeService(context)
            }

            else if (HelpMeService.position < HelpMeService.watcherList.size && HelpMeService.cycle < 2 && HelpMeService.contactWatcherStatus == "Running") {
                iterateWatchers(HelpMeService.watcherList)
                HelpMeService.position++

                if (HelpMeService.position >= HelpMeService.watcherList.size) {
                    HelpMeService.position = 0
                    HelpMeService.cycle++
                }
                schecduleExactAlarm(context,context.getSystemService(Context.ALARM_SERVICE) as AlarmManager, 20)

            } else {

                HelpMeService.stopMe = true

                if (HelpMeService.cycle >= 2) {
                    HelpMeService.contactWatcherStatus = "Complete"
                }

                val notificationHeader = "Help Me Response"
                var notificationText = ""

                if (HelpMeService.contactWatcherStatus == "Complete") {
                    notificationText =
                        HelpMeService.wearerFirstName + ", Watch Over Me has contacted all your watchers and none of them responded " +
                                "yet. We recommend you to seek other ways to get help."
                } else if (HelpMeService.contactWatcherStatus == "Responded") {
                    notificationText = "${HelpMeService.wearerFirstName}, help is coming"
                } else if (HelpMeService.contactWatcherStatus == "Stopped") {
                    notificationText = "Help Me service has been stopped."
                } else {
                    notificationText = "Help Me service was interrupted."
                }

                val apiService = ServiceBuilder.buildService(APIService::class.java)
                val requestCall =
                    apiService.wearerNotification(
                        HelpMeService.serviceId,
                        notificationHeader,
                        notificationText
                    )

                requestCall.enqueue(object : Callback<String> {
                    override fun onResponse(call: Call<String>, response: Response<String>) {

                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {

                    }

                })
                context.sendBroadcast(Intent().setAction("HelpMeStatus"))
                val timeNow = Calendar.getInstance().time
                HelpMeService.timeInitiated = HelpMeService.dateTimeFormatter.format(timeNow)
                schecduleExactAlarm(context,context.getSystemService(Context.ALARM_SERVICE) as AlarmManager, 15 * 60)
            }

        }
        handler.post(watcherRunnable)
        wakeLock.release()
    }


    fun iterateWatchers(watchers: ArrayList<Watcher>) {


        val notificationHeader = "Help Me Response"
        var notificationText = ""
        val timeNow = Calendar.getInstance().time
        val contactDate = HelpMeService.dateFormatter1.format(timeNow)
        val contactTime = HelpMeService.timeFormatter1.format(timeNow)
        val alertNum = HelpMeService.alertLogId.substring(3)
        val watcherIdNum = watchers[HelpMeService.position].watcherId?.substring(6)
        val responseBaseLink = "http://192.168.0.105/hmr/"
        val responseLink = responseBaseLink + alertNum + contactDate + "/" + watcherIdNum + contactTime

        if (HelpMeService.cycle == 0) {
            if (HelpMeService.position == 0) {
                notificationText =
                    HelpMeService.wearerFirstName + ", Watch Over Me team is now contacting " + watchers[HelpMeService.position].watcherFirstName +
                            " " + watchers[HelpMeService.position].watcherLastName + " through email and SMS. They are number " + (HelpMeService.position + 1) + " of " + watchers.size +
                            " possible responding watchers for you. We will contact all of your responding watchers in sequence and keep " +
                            "you informed of the progress."
            } else {
                notificationText =
                    HelpMeService.wearerFirstName + ", Watch Over Me team is now contacting " + watchers[HelpMeService.position].watcherFirstName +
                            " " + watchers[HelpMeService.position].watcherLastName + " through email and SMS. They are number " + (HelpMeService.position + 1) + " of " + watchers.size +
                            " possible responding watchers for you."
            }



        }
        else if (HelpMeService.cycle == 1) {
            if (HelpMeService.position == 0) {
                notificationText =
                    HelpMeService.wearerFirstName + ", this is the second and final cycle of this request. Watch Over Me team is now contacting " + watchers[HelpMeService.position].watcherFirstName +
                            " " + watchers[HelpMeService.position].watcherLastName + " through phone call. They are number " + (HelpMeService.position + 1) + " of " + watchers.size +
                            " possible responding watchers for you."
            } else {
                notificationText =
                    HelpMeService.wearerFirstName + ", Watch Over Me team is now contacting " + watchers[HelpMeService.position].watcherFirstName +
                            " " + watchers[HelpMeService.position].watcherLastName + " through phone call. They are number " + (HelpMeService.position + 1) + " of " + watchers.size +
                            " possible responding watchers for you."
            }

        }


        val apiService = ServiceBuilder.buildService(APIService::class.java)
        val requestCall = apiService.contactWatcher(
            HelpMeService.serviceId,
            notificationHeader,
            notificationText,
            watchers[HelpMeService.position].watcherId,
            HelpMeService.cycle.toString(),
            HelpMeService.alertLogId,
            HelpMeService.wearerId,
            HelpMeService.dateFormatter.format(
                timeNow
            ),
            HelpMeService.timeFormatter.format(timeNow),
            responseLink,
            watchers[HelpMeService.position].watcherPhone,
            HelpMeService.wearerFirstName
        )

        requestCall.enqueue(object : Callback<ServerResponse> {
            override fun onResponse(
                call: Call<ServerResponse>,
                response: Response<ServerResponse>
            ) {
                val serverResponse = response.body()
                if (serverResponse != null) {
                    if (serverResponse.connection!! && serverResponse.queryStatus!!) {
                        HelpMeService.contactWatcherStatus = "Responded"
                    } else if (serverResponse.connection!! && !serverResponse.queryStatus!!) {
                        iterateWatchers(watchers)
                        HelpMeService.position++
                    } else {

                        if (HelpMeService.cycle == 0) {
//                            val smsText = "Hi, "+ watchers[position].watcherFirstName + " follow the link to help me: \n $responseLink"
//                            Toast.makeText(this@HelpMeService,"Sending Sms",Toast.LENGTH_SHORT).show()
//                            SmsManager.getDefault().sendTextMessage(watchers[position].watcherPhone,null,smsText,null,null)
                        }
                    }
//                    val smsText = "Hi, "+ watchers[position].watcherFirstName + " follow the link to help me: \n $responseLink"
//                    Toast.makeText(this@HelpMeService,"Sending Sms",Toast.LENGTH_SHORT).show()
//                    SmsManager.getDefault().sendTextMessage(watchers[position].watcherPhone,null,smsText,null,null)
                } else {

                    HelpMeService.contactWatcherStatus = ""
                }
            }

            override fun onFailure(call: Call<ServerResponse>, t: Throwable) {

                HelpMeService.contactWatcherStatus = ""
            }

        })
    }

    private fun stopHelpMeService(context: Context) {
        val serviceIntent = Intent(context, HelpMeService::class.java)
        serviceIntent.action = 1.toString()
        context.stopService(serviceIntent)
    }

    companion object {
        const val HELP_ME_TRIGGER = "HelpMeTrigger"
        fun schecduleExactAlarm(context: Context, alarmManager: AlarmManager, interval: Int){
            val refreshInterval = interval
            val intent = Intent(context, HelpMeTrigger::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context,0,intent,0)
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+(refreshInterval*1000),pendingIntent)
        }
    }
}