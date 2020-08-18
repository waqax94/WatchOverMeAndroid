package com.uawdevstudios.watchovermeandroid.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hbb20.CountryCodePicker
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.R.*
import com.uawdevstudios.watchovermeandroid.models.Watcher
import kotlinx.android.synthetic.main.watcher_item.view.*

class WatcherAdapter(private val watcherList: List<Watcher>,private val context: Context): RecyclerView.Adapter<WatcherAdapter.WatcherViewHolder>() {


    class WatcherViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val watcherRanking: TextView = itemView.watcherPriority
        val watcherName: TextView = itemView.watcherName
        val watcherNumber: TextView = itemView.watcherContact
        val watcherCall: ImageButton = itemView.watcherCallButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatcherViewHolder {
        val itemView:View = LayoutInflater.from(parent.context).inflate(R.layout.watcher_item,parent,false)


        return WatcherViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: WatcherViewHolder, position: Int) {

        val currentWatcher = watcherList[position]

        holder.watcherRanking.text = currentWatcher.watcherPriorityNum
        holder.watcherName.text = currentWatcher.watcherFirstName + " " + currentWatcher.watcherLastName
        holder.watcherNumber.text = currentWatcher.watcherPhone

        holder.watcherCall.setOnClickListener {
            val phoneIntent: Intent = Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",currentWatcher.watcherPhone,null))
            startNextIntent(phoneIntent)
        }

    }

    override fun getItemCount() = watcherList.size

    fun startNextIntent(intent: Intent){

        context.startActivity(intent)
    }

}