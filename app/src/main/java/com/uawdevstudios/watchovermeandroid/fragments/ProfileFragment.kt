package com.uawdevstudios.watchovermeandroid.fragments

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.activities.LoginActivity
import com.uawdevstudios.watchovermeandroid.activities.MainActivity
import com.uawdevstudios.watchovermeandroid.services.CustomLocationService
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.view.*


class ProfileFragment : Fragment(), OnMapReadyCallback {

    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var rootView: View

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
            stopLocationService()
            MainActivity().onBackPressed()
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