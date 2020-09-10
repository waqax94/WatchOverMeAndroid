package com.uawdevstudios.watchovermeandroid.receivers

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.PowerManager
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import com.uawdevstudios.watchovermeandroid.models.ServerResponse
import com.uawdevstudios.watchovermeandroid.models.Watcher
import com.uawdevstudios.watchovermeandroid.services.APIService
import com.uawdevstudios.watchovermeandroid.services.HelpMeService
import com.uawdevstudios.watchovermeandroid.services.ServiceBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class HelpMeTrigger: BroadcastReceiver() {


    @SuppressLint("InvalidWakeLockTag")
    override fun onReceive(context: Context?, p1: Intent?) {

        Log.e("Alarm", "Alarm fired")

        val powerManager = context!!.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, HELP_ME_TRIGGER)
        wakeLock.acquire()


        val handler = Handler()
        val watcherRunnable = Runnable {

            if(HelpMeService.stopMe){
                val apiService = ServiceBuilder.buildService(APIService::class.java)

                val requestCall =
                    apiService.wearerNotification(
                        HelpMeService.serviceId,
                        "Help me service",
                        "Help me service is now available")

                requestCall.enqueue(object : Callback<String> {
                    override fun onFailure(call: Call<String>, t: Throwable) {

                    }

                    override fun onResponse(call: Call<String>, response: Response<String>) {

                    }
                })
                stopHelpMeService(context)
            }

            else if (HelpMeService.position < HelpMeService.watcherList.size && HelpMeService.cycle < 2 && HelpMeService.contactWatcherStatus == "Running") {
                iterateWatchers(HelpMeService.watcherList, HelpMeService.position, context)
                HelpMeService.position++

                if (HelpMeService.position >= HelpMeService.watcherList.size) {
                    HelpMeService.position = 0
                    HelpMeService.cycle++
                }


            } else {

                HelpMeService.stopMe = true

                if (HelpMeService.cycle >= 2
                    && HelpMeService.contactWatcherStatus != "Responded"
                    && HelpMeService.contactWatcherStatus != "Stopped") {
                    HelpMeService.contactWatcherStatus = "Complete"
                }

                Log.e("Helpme Status", HelpMeService.contactWatcherStatus)

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


    fun iterateWatchers(watchers: ArrayList<Watcher>, pos: Int, context: Context) {


        val notificationHeader = "Help Me Response"
        var notificationText = ""
        var smsStatus = true
        val timeNow = Calendar.getInstance().time
        val contactDate = HelpMeService.dateFormatter1.format(timeNow)
        val contactTime = HelpMeService.timeFormatter1.format(timeNow)
        val alertNum = HelpMeService.alertLogId.substring(3)
        val watcherIdNum = watchers[HelpMeService.position].watcherId?.substring(6)
        val responseBaseLink = "http://watchoverme.uawdevstudios.com/hmr/"
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
            smsStatus = false
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
            watchers[pos].watcherId,
            HelpMeService.cycle.toString(),
            HelpMeService.alertLogId,
            HelpMeService.wearerId,
            HelpMeService.dateFormatter.format(timeNow),
            HelpMeService.timeFormatter.format(timeNow),
            responseLink,
            watchers[pos].watcherPhone,
            HelpMeService.wearerFirstName + " " + HelpMeService.wearerLastName,
            watchers[pos].watcherEmail,
            watchers[pos].watcherFirstName,
            watchers[pos].watcherLastName
        )

        requestCall.enqueue(object : Callback<ServerResponse> {
            override fun onResponse(
                call: Call<ServerResponse>,
                response: Response<ServerResponse>
            ) {
                val serverResponse = response.body()
                if (serverResponse != null) {
                    Log.e("Response Test", serverResponse.message.toString())
                    if (serverResponse.connection!! && serverResponse.queryStatus!!) {
                        HelpMeService.contactWatcherStatus = "Responded"
                    } else if (serverResponse.connection!! && !serverResponse.queryStatus!!) {
                        iterateWatchers(watchers,pos+1, context)
                        HelpMeService.position++
                    } else {
                        if (smsStatus) {
                            val smsText = "Hi " + watchers[pos].watcherFirstName + ", ${HelpMeService.wearerFirstName} need your help. Please follow the link to respond: \n" + responseLink
                            try {
                                val smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(watchers[pos].watcherPhone, null, smsText, null, null);
                            } catch (e: Exception) {
                                Toast.makeText(
                                    HelpMeService.context, e.toString(),
                                    Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    schecduleExactAlarm(context,context.getSystemService(Context.ALARM_SERVICE) as AlarmManager, 20)

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
            val c = Calendar.getInstance()
            c.add(Calendar.SECOND, interval)
            val timeAfter = c.timeInMillis
            val intent = Intent(context, HelpMeTrigger::class.java)
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            val pendingIntent = PendingIntent.getBroadcast(context,0,intent,0)
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(timeAfter,pendingIntent),pendingIntent)
        }
    }
}