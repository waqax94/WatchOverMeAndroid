package com.uawdevstudios.watchovermeandroid.adapters

import android.icu.util.LocaleData
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.models.NotificationItem
import com.uawdevstudios.watchovermeandroid.services.CustomLocationService
import kotlinx.android.synthetic.main.notification_item.view.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class NotificationAdapter(private val notificationList: List<NotificationItem>): RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val notificationMessage: TextView = itemView.notificationItemText
        val notificationTime: TextView = itemView.notificationItemTime
        val notificationDate: TextView = itemView.notificationItemDate
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView:View = LayoutInflater.from(parent.context).inflate(R.layout.notification_item,parent,false)

        return NotificationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val currentItem = notificationList[position]

        holder.notificationMessage.text = currentItem.notificationText
        holder.notificationTime.text = currentItem.notificationTime.toString()
        holder.notificationDate.text = currentItem.notificationDate.toString()
    }

    override fun getItemCount() = notificationList.size

}