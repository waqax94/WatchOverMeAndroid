package com.uawdevstudios.watchovermeandroid.fragments

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.replace
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.activities.HelpMeRequestActivity
import com.uawdevstudios.watchovermeandroid.activities.LoginActivity
import com.uawdevstudios.watchovermeandroid.activities.MainActivity
import com.uawdevstudios.watchovermeandroid.models.NotificationItem
import com.uawdevstudios.watchovermeandroid.services.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_fragment_home.view.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class HomeFragment : Fragment() {

    lateinit var broadcastReceiver: BroadcastReceiver
    lateinit var rootView: View
    lateinit var fusedLocationClient: FusedLocationProviderClient
    var serviceId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView = inflater.inflate(R.layout.fragment_home, container, false)

        val loginData =
            activity?.getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        serviceId = loginData?.getString("serviceId", "")

        rootView.homeServiceId.text = serviceId
        rootView.homeWearerName.text = loginData?.getString(
            "wearerFirstName",
            ""
        ) + " " + loginData?.getString("wearerLastName", "")


        if (isHelpMeServiceRunning()) {
            rootView.homeHelpMeButton.text = "STOP"
        }


        val permissionDialog = AlertDialog.Builder(activity)
        permissionDialog.setTitle("Help me request running")
        permissionDialog.setMessage("Do you want to stop it?")

        permissionDialog.setPositiveButton("Stop") { dialog, which ->
            stoptHelpMeService()
            rootView.homeHelpMeButton.text = "Help ME!"
        }
        permissionDialog.setNegativeButton("Cancel") { dialog, which ->

        }


        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val bLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                rootView.homeBattery.text = "$bLevel% remaining"
            }
        }

        requireActivity().registerReceiver(
            broadcastReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        rootView.homeGreetingText.text = getGreetingText()
        getCurrentLocation()

        rootView.homeHelpMeButton.setOnClickListener {

            if (isHelpMeServiceRunning()) {
                permissionDialog.show()
            } else {
                val intent = Intent(rootView.context, HelpMeRequestActivity::class.java)
                startActivity(intent)
                activity?.finish()
            }
        }

        return rootView
    }

    override fun onStop() {
        super.onStop()
        try {
            requireActivity().unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getGreetingText(): String {

        val hour = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Integer.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH")))
        } else {
            Integer.valueOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        }

        if (hour in 5..12) {
            return "Good Morning,"
        } else if (hour in 13..17) {
            return "Good Afternoon,"
        } else if (hour in 18..20) {
            return "Good Evening,"
        } else if ((hour in 21..24) || (hour in 0..4)) {
            return "Hi there,"
        }
        return "Hello"
    }

    fun getCurrentLocation() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        if (ActivityCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    111
                )
            }
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            reverseGeoCoder(location)
        }
    }

    fun reverseGeoCoder(location: Location?) {
        val gc = Geocoder(activity, Locale.getDefault())
        try {
            val addresses = gc.getFromLocation(location!!.latitude, location.longitude, 2)
            val address = addresses[0]
            rootView.homeLocation.text = address.locality
        } catch (e: java.lang.Exception) {
            getCurrentLocation()
        }
    }

    private fun stoptHelpMeService() {
        if (isHelpMeServiceRunning()) {
            val serviceIntent = Intent(rootView.context, HelpMeService::class.java)
            serviceIntent.action = 2.toString()
            activity?.stopService(serviceIntent)
        }
    }

    private fun isHelpMeServiceRunning(): Boolean {
        val manager =
            activity?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if ("com.uawdevstudios.watchovermeandroid.services.HelpMeService" == service.service.className) {
                return true
            }
        }
        return false
    }

}