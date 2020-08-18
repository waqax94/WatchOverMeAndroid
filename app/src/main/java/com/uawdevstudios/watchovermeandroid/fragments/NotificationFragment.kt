package com.uawdevstudios.watchovermeandroid.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.JsonReader
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.adapters.NotificationAdapter
import com.uawdevstudios.watchovermeandroid.models.NotificationItem
import com.uawdevstudios.watchovermeandroid.services.FileService
import kotlinx.android.synthetic.main.fragment_notification.view.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern
import java.util.*
import kotlin.collections.ArrayList

class NotificationFragment : Fragment() {

    var notificationList = ArrayList<NotificationItem>()
    val notificationAdapter = NotificationAdapter(notificationList)
    val formatter = SimpleDateFormat("dd MMMM yyyy hh:mm:ss aa")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_notification, container, false)



//        FileService(rootView.context).writeToFile(notificationList)

        loadNotifications(rootView.context)
//        popNotificationItems()
//        notificationList = FileService(rootView.context).loadFromFile()
//        sortList()
        rootView.notificationRecyclerView.adapter = notificationAdapter

//        notificationList = FileService(rootView.context).loadFromFile()

        Log.d(
            "Read File Content",
            "JSON: " + notificationList.toString()
        )


        //popNotificationItems()
        rootView.notificationRecyclerView.layoutManager = LinearLayoutManager(this.context)
        rootView.notificationRecyclerView.setHasFixedSize(true)

        return rootView
    }

    fun loadNotifications(context: Context) {

        val newNotificationItems = FileService(context).loadFromFile()
        for (i in 0..newNotificationItems.size-1){
            notificationList.add(newNotificationItems.get(i))
        }
        sortList()
    }

    fun popNotificationItems() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -12)
        val timeNow = calendar.time
        for (i in notificationList.size - 1 downTo 0) {
            val notificationTimeStamp =
                formatter.parse(notificationList[i].notificationDate + " " + notificationList[i].notificationTime)
            if (notificationTimeStamp!!.before(timeNow)) {
                notificationList.removeAt(i)
            }
        }
        sortList()
    }

    private fun sortList() {

        notificationList.sortByDescending { formatter.parse(it.notificationDate + " " + it.notificationTime) }

        notificationAdapter.notifyDataSetChanged()

    }


}


//notificationList.add(
//NotificationItem(
//"Waqas Waheed,The HelpMe Watch team is now contacting Iffat Khan for you my SMS, Email and phone. They are number 1 of 4 possible responding watchers for you. We will contact all of your Responding Watchers in sequence and keep you informed of the progress.",
//"11 January 2020", "10:20:55 am"
//)
//)
//addNotificationItem(NotificationItem(
//"Waqas Waheed,The HelpMe Watch team is now contacting Iffat Khan for you my SMS, Email and phone. They are number 1 of 4 possible responding watchers for you. We will contact all of your Responding Watchers in sequence and keep you informed of the progress.",
//"15 January 2020", "09:20:36 am"
//))
//addNotificationItem(NotificationItem(
//"Waqas Waheed,The HelpMe Watch team is now contacting Iffat Khan for you my SMS, Email and phone. They are number 1 of 4 possible responding watchers for you. We will contact all of your Responding Watchers in sequence and keep you informed of the progress.",
//"02 January 2020", "10:25:36 am"
//))
//addNotificationItem(NotificationItem(
//"Waqas Waheed,The HelpMe Watch team is now contacting Iffat Khan for you my SMS, Email and phone. They are number 1 of 4 possible responding watchers for you. We will contact all of your Responding Watchers in sequence and keep you informed of the progress.",
//"15 January 2020", "11:28:36 am"
//))
//addNotificationItem(NotificationItem(
//"Waqas Waheed,The HelpMe Watch team is now contacting Iffat Khan for you my SMS, Email and phone. They are number 1 of 4 possible responding watchers for you. We will contact all of your Responding Watchers in sequence and keep you informed of the progress.",
//"09 August 2020", "11:20:36 am"
//))