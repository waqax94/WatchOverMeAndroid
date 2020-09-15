package com.uawdevstudios.watchovermeandroid.receivers

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.uawdevstudios.watchovermeandroid.services.APIService
import com.uawdevstudios.watchovermeandroid.services.ServiceBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class LocationBroadcast : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

         val notificationTitle = intent.getStringExtra("message")!!

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(context)
        val apiService = ServiceBuilder.buildService(APIService::class.java)
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val loginData =
            context.getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        val serviceId = loginData?.getString("serviceId", "")

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            if (notificationTitle.substring(0, 8) != "Location" || serviceId == "") {
                return@addOnSuccessListener
            }

            val requestCall = apiService.sendLocation(
                serviceId,
                notificationTitle.substring(8),
                location?.latitude.toString(),
                location?.longitude.toString(),
                reverseGeoCoder(location,context)
            )

            requestCall.enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.body() != "done") {
                        Log.e("Location Push", "Error in location sending")
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.e("Location Push", "Error in location sending")
                }

            })

        }

    }

    fun reverseGeoCoder(location: Location?, context: Context): String {
        val gc = Geocoder(context, Locale.getDefault())
        try {
            val addresses = gc.getFromLocation(location!!.latitude, location.longitude, 2)
            val address = addresses[0]
            return address.locality + ", " + address.adminArea + ", " + address.countryName
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return ""
    }

}
