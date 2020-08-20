package com.uawdevstudios.watchovermeandroid.fragments

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.activities.LoginActivity
import com.uawdevstudios.watchovermeandroid.activities.MainActivity
import com.uawdevstudios.watchovermeandroid.models.NotificationItem
import com.uawdevstudios.watchovermeandroid.services.APIService
import com.uawdevstudios.watchovermeandroid.services.CustomLocationService
import com.uawdevstudios.watchovermeandroid.services.FileService
import com.uawdevstudios.watchovermeandroid.services.ServiceBuilder
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.view.*
import kotlinx.android.synthetic.main.progress_view.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ProfileFragment : Fragment(), OnMapReadyCallback {

    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var rootView: View
    lateinit var progressDialog: AlertDialog

    private val mHandler: Handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_profile, container, false)

        val loginData =
            activity?.getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)

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

        rootView.profileServiceId.text = loginData?.getString("serviceId", "")
        rootView.profileFirstName.text = loginData?.getString("wearerFirstName", "")
        rootView.profileLastName.text = loginData?.getString("wearerLastName","")
        rootView.profileEmail.text = loginData?.getString("wearerEmail","")
        rootView.profilePhone.text = loginData?.getString("wearerPhone","")

        initMap(savedInstanceState)

        val permissionDialog = AlertDialog.Builder(activity)
        permissionDialog.setTitle("Sign Out?")
        permissionDialog.setMessage("Are you sure?")

        permissionDialog.setPositiveButton("Sign Out") { dialog, which ->

            progressDialog.show()
            val apiService = ServiceBuilder.buildService(APIService::class.java)
            val requestCall = apiService.setLoginStatus(loginData?.getString("serviceId", ""),"false")

            requestCall.enqueue(object : Callback<String>{
                override fun onResponse(call: Call<String>, response: Response<String>) {

                    if(response.body().toString() == "done"){
                        val userData =
                            activity?.getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
                        val editor = userData?.edit()
                        if (editor != null) {
                            editor.putString("wearerId", "")
                            editor.putString("serviceId", "")
                            editor.putString("wearerFirstName", "")
                            editor.putString("wearerLastName", "")
                            editor.putString("wearerEmail", "")
                            editor.putString("wearerPhone", "")
                            editor.putString("wearerPassword", "")
                            editor.apply()
                        }

                        val intent = Intent(rootView.context, LoginActivity::class.java)
                        startActivity(intent)
                        FileService(rootView.context).writeToFile(ArrayList<NotificationItem>())
                        stopLocationService()
                        MainActivity().onBackPressed()
                        progressDialog.dismiss()
                    }
                    else{
                        progressDialog.dismiss()
                        Toast.makeText(rootView.context,"Unable to logout",Toast.LENGTH_SHORT).show()
                    }

                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(rootView.context,"Connection Error",Toast.LENGTH_SHORT).show()
                }

            })
        }

        permissionDialog.setNegativeButton("Cancel") { dialog, which ->
        }


        rootView.profileSignOutButton.setOnClickListener {
            permissionDialog.show()
        }

        return rootView
    }


    private fun stopLocationService() {
        val serviceIntent = Intent(activity?.baseContext, CustomLocationService::class.java)
        serviceIntent.action = 1.toString()
        activity?.stopService(serviceIntent)
    }

    fun initMap(savedInstanceState: Bundle?) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        var mapViewBundle: Bundle? = null

        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle("AIzaSyDwXBxaCCsrD98pkkixfhwek9goF3IV9MI")
        }

        rootView.mapView.onCreate(mapViewBundle)

        rootView.mapView.getMapAsync(this)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        var mapViewBundle = outState.getBundle("AIzaSyDwXBxaCCsrD98pkkixfhwek9goF3IV9MI")
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle("AIzaSyDwXBxaCCsrD98pkkixfhwek9goF3IV9MI", mapViewBundle)
        }
        rootView.mapView.onSaveInstanceState(mapViewBundle)
    }


    override fun onStart() {
        super.onStart()
        rootView.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        rootView.mapView.onStop()
    }

    override fun onResume() {
        super.onResume()
        rootView.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        rootView.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        rootView.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        rootView.mapView.onLowMemory()
    }


    override fun onMapReady(googleMap: GoogleMap?) {
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
            if (location != null) {

                val latLng = LatLng(location.latitude, location.longitude)

                val options = MarkerOptions().position(latLng).title("Current Location")

                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18.0F))

                googleMap?.addMarker(options)

            }
        }
    }
}