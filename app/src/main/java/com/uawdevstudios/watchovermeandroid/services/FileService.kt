package com.uawdevstudios.watchovermeandroid.services

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.uawdevstudios.watchovermeandroid.models.NotificationItem
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class FileService(context: Context) {

    private val formatter = SimpleDateFormat("dd MMMM yyyy hh:mm:ss aa")
    private val gsonPretty = GsonBuilder().setPrettyPrinting().create()
    private val filePath = context.getDir("notifications", Context.MODE_PRIVATE)
    private var file = File(filePath, FILE_NAME)


    fun writeToFile(notifications: List<NotificationItem>){

        val jsonString = gsonPretty.toJson(notifications)
        val fileOutputStream = FileOutputStream(file)

        try {
            fileOutputStream.write(jsonString.toByteArray())
            fileOutputStream.flush()
            fileOutputStream.close()
        }
        catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun loadFromFile(): ArrayList<NotificationItem>{

        try {
            val bufferedReader = file.bufferedReader()
            val notificationListType = object : TypeToken<ArrayList<NotificationItem>>() {}.type
            val jsonString = bufferedReader.use { it.readText() }


            val notificationItemList: ArrayList<NotificationItem> = Gson().fromJson(jsonString,notificationListType)

            return notificationItemList
        }
        catch (e: Exception){

        }
        return ArrayList<NotificationItem>()
    }

    fun saveNotification(newNotification: NotificationItem){
        var notifications = ArrayList<NotificationItem>()

        notifications = loadFromFile()
        notifications.add(newNotification)
        writeToFile(notifications)

    }

    fun removeNotifications(){
        val notificationList = loadFromFile()
        val TIME_IN_MILLIS = 11 * 60 * 60000

        val calendar = Calendar.getInstance()
        val timeNow = calendar.time
        for (i in notificationList.size - 1 downTo 0) {
            val notificationTimeStamp = formatter.parse(notificationList[i].notificationDate + " " + notificationList[i].notificationTime)
            if (timeNow.time - notificationTimeStamp!!.time >= TIME_IN_MILLIS) {
                notificationList.removeAt(i)
            }
        }
        writeToFile(notificationList)


    }

    companion object {
        val FILE_NAME = "notifications.json"
    }

}