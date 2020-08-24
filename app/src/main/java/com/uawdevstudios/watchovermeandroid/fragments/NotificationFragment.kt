package com.uawdevstudios.watchovermeandroid.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

        activity?.registerReceiver(this.broadcastReceiver, IntentFilter("NewNotification"))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_notification, container, false)

        loadNotifications(rootView.context)
        rootView.notificationRecyclerView.adapter = notificationAdapter


        Log.d(
            "Read File Content",
            "JSON: " + notificationList.toString()
        )

        rootView.notificationRecyclerView.layoutManager = LinearLayoutManager(this.context)
        rootView.notificationRecyclerView.setHasFixedSize(true)

        return rootView
    }

    val broadcastReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            loadNotifications(context!!)
        }

    }

    fun loadNotifications(context: Context) {

        notificationList.clear()
        val newNotificationItems = FileService(context).loadFromFile()
        for (i in 0..newNotificationItems.size-1){
            notificationList.add(newNotificationItems.get(i))
        }
        sortList()
    }

    private fun sortList() {

        notificationList.sortByDescending { formatter.parse(it.notificationDate + " " + it.notificationTime) }

        notificationAdapter.notifyDataSetChanged()

    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.unregisterReceiver(this.broadcastReceiver)
    }


}
