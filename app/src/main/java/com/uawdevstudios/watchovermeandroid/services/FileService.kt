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

class FileService(context: Context) {

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

        Log.d(
            "File Content",
            "JSON: " + jsonString
        )
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


    companion object {
        val FILE_NAME = "notifications.json"
    }

}