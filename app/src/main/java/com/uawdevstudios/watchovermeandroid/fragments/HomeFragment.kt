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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    lateinit var broadcastReceiver: BroadcastReceiver
    lateinit var UIBroadcastReceiver: BroadcastReceiver
    lateinit var rootView: View
    lateinit var fusedLocationClient: FusedLocationProviderClient
    val dateTimeFormatter = SimpleDateFormat("dd MM yyyy hh:mm:ss aa")
    var serviceId: String? = null
    var wearFirstName: String? = null


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
        wearFirstName = loginData?.getString("wearerFirstName", "")

        rootView.homeServiceId.text = serviceId
        rootView.homeWearerName.text = loginData?.getString(
            "wearerFirstName",
            ""
        ) + " " + loginData?.getString("wearerLastName", "")

        checkService()
        updateUI(rootView)

        val permissionDialog1 = AlertDialog.Builder(activity)
        permissionDialog1.setTitle("Help Me service is running")
        permissionDialog1.setMessage("Stop contacting watchers")

        permissionDialog1.setPositiveButton("Stop") { dialog, which ->
            //stoptHelpMeService()
            HelpMeService.contactWatcherStatus = "Stopped"
            updateUI(rootView)

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

        UIBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateUI(rootView)
            }
        }

        requireActivity().registerReceiver(this.UIBroadcastReceiver, IntentFilter("HelpMeStatus"))

        rootView.homeGreetingText.text = getGreetingText()
        getCurrentLocation()

        rootView.homeHelpMeButton.setOnClickListener {



            if (isHelpMeServiceRunning()) {

                if (HelpMeService.contactWatcherStatus == "Running") {
                    permissionDialog1.show()
                } else {
                    val permissionDialog2 = AlertDialog.Builder(activity)
                    permissionDialog2.setTitle("Help Me service is currently unavailable")
                    permissionDialog2.setMessage("It will be available after ${calculateRemainingTime()} minutes")

                    permissionDialog2.setPositiveButton("Dismiss") { dialog, which ->

                    }

                    permissionDialog2.show()
                }

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
            requireActivity().unregisterReceiver(UIBroadcastReceiver)
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
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    111
                )
            }
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            try {
                reverseGeoCoder(location)
            }
            catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    fun reverseGeoCoder(location: Location?) {
        val gc = Geocoder(activity, Locale.getDefault())
        try {
            val addresses = gc.getFromLocation(location!!.latitude, location.longitude, 2)
            val address = addresses[0]
            rootView.homeLocation.text = (address.locality + ", " + address.adminArea)
        } catch (e: java.lang.Exception) {
            getCurrentLocation()
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

    private fun checkService() {
        if(isHelpMeServiceRunning()){
            if(calculateRemainingTime() <= 0 || calculateRemainingTime() > 20){
                val intent = Intent(rootView.context,HelpMeService::class.java)
                requireActivity().stopService(intent)
            }
        }
    }

    fun updateUI(rootView: View) {
        if (isHelpMeServiceRunning()) {
            if (HelpMeService.contactWatcherStatus == "Running") {
                rootView.homeHelpMeButton.text = "STOP"
                rootView.homeHelpMeButton.background = resources.getDrawable(R.drawable.button_bg2)
            } else {
                rootView.homeHelpMeButton.text = "Help ME!"
                rootView.homeHelpMeButton.background = resources.getDrawable(R.drawable.button_bg7)
            }
        } else {
            rootView.homeHelpMeButton.text = "Help ME!"
            rootView.homeHelpMeButton.background = resources.getDrawable(R.drawable.button_bg2)
        }
    }

    fun calculateRemainingTime() : Int{
        val ONE_MINUTE_IN_MILLIS = 60000
        val timeNow = Calendar.getInstance().time
        val initiatedTime = dateTimeFormatter.parse(HelpMeService.timeInitiated)
        val t = initiatedTime.time
        val availableTime = Date(t + (15 * ONE_MINUTE_IN_MILLIS))
        val timeRemaining = availableTime.time - timeNow.time
        return TimeUnit.MILLISECONDS.toMinutes(timeRemaining).toInt()
    }

}