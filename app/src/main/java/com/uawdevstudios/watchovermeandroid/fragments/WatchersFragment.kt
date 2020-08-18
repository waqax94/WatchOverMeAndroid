package com.uawdevstudios.watchovermeandroid.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.adapters.WatcherAdapter
import com.uawdevstudios.watchovermeandroid.models.NotificationItem
import com.uawdevstudios.watchovermeandroid.models.ServerResponse
import com.uawdevstudios.watchovermeandroid.models.Watcher
import com.uawdevstudios.watchovermeandroid.models.Wearer
import com.uawdevstudios.watchovermeandroid.services.APIService
import com.uawdevstudios.watchovermeandroid.services.ServiceBuilder
import kotlinx.android.synthetic.main.fragment_profile.view.*
import kotlinx.android.synthetic.main.fragment_watchers.view.*
import kotlinx.android.synthetic.main.progress_view.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt

class WatchersFragment : Fragment() {

    var watcherList = ArrayList<Watcher>()
    lateinit var watcherAdapter: WatcherAdapter
    lateinit var progressDialog: AlertDialog
    var serviceId: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_watchers, container, false)

        val loginData =
            activity?.getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        serviceId = loginData?.getString("serviceId", "")
        watcherAdapter = WatcherAdapter(watcherList, rootView.context)
        val progressDialogBuilder = AlertDialog.Builder(rootView.context)
        val dialogView = layoutInflater.inflate(R.layout.progress_view, null)
        dialogView.loadingText.text = "Please wait..."
        Glide.with(rootView.context)
            .load(R.drawable.loading)
            .placeholder(R.drawable.loading)
            .centerCrop()
            .crossFade()
            .into(dialogView.loadingIcon)
        progressDialogBuilder.setView(dialogView)
        progressDialogBuilder.setCancelable(false)
        progressDialog = progressDialogBuilder.create()

        loadWatchers(rootView)

        rootView.watcherConnectionLayout.setOnClickListener {
            loadWatchers(rootView)
        }

        return rootView
    }

    fun loadWatchers(rootView: View) {
        val apiService: APIService = ServiceBuilder.buildService(APIService::class.java)
        val requestCall = apiService.getWatchers(serviceId)

        progressDialog.show()

        requestCall.enqueue(object : Callback<ServerResponse> {

            override fun onResponse(
                call: Call<ServerResponse>,
                response: Response<ServerResponse>
            ) {
                val serverResponse = response.body()
                progressDialog.dismiss()

                if(serverResponse != null){
                    if(serverResponse.connection == true && serverResponse.queryStatus == true){

                        val watcherListType = object : TypeToken<ArrayList<Watcher>>() {}.type

                        val dataList =
                            Gson().fromJson(serverResponse.data.toString(), watcherListType) as ArrayList<Watcher>

                        for(data in dataList){
                            data.watcherPriorityNum = data.watcherPriorityNum!!.toFloat().roundToInt().toString()
                            watcherList.add(data)
                        }

                        rootView.watchersRecyclerView.adapter = watcherAdapter
                        rootView.watchersRecyclerView.layoutManager =
                            LinearLayoutManager(rootView.context)
                        rootView.watchersRecyclerView.setHasFixedSize(true)

                        rootView.watcherConnectionLayout.visibility = View.INVISIBLE
                        rootView.watchersRecyclerView.visibility = View.VISIBLE

                    }
                    else{
                        rootView.watchersRecyclerView.visibility = View.INVISIBLE
                        rootView.watcherConnectionHeader.text = "Response Error!"
                        rootView.watcherConnectionLayout.visibility = View.VISIBLE
                    }
                }
                else{
                    rootView.watchersRecyclerView.visibility = View.INVISIBLE
                    rootView.watcherConnectionHeader.text = "Response Error!"
                    rootView.watcherConnectionLayout.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
                progressDialog.dismiss()
                rootView.watchersRecyclerView.visibility = View.INVISIBLE
                rootView.watcherConnectionLayout.visibility = View.VISIBLE
            }
        })
    }
}






