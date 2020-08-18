package com.uawdevstudios.watchovermeandroid.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.firebase.iid.FirebaseInstanceId
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.fragments.*
import com.uawdevstudios.watchovermeandroid.services.APIService
import com.uawdevstudios.watchovermeandroid.services.CustomLocationService
import com.uawdevstudios.watchovermeandroid.services.ServiceBuilder
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val EXTRA_CIRCULAR_REVEAL_X = "EXTRA_CIRCULAR_REVEAL_X"
    private val EXTRA_CIRCULAR_REVEAL_Y = "EXTRA_CIRCULAR_REVEAL_Y"

    var revealX: Int = 0
    var revealY: Int = 0

    val fragmentManager = supportFragmentManager
    var homeVisible = false
    var notificationVisible = false
    var watchersVisible = false
    var profileVisible = false
    lateinit var location: Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out)
        setContentView(R.layout.activity_main)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)


        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            intent.hasExtra(EXTRA_CIRCULAR_REVEAL_X) &&
            intent.hasExtra(EXTRA_CIRCULAR_REVEAL_Y)
        ) {
            revealX = intent.getIntExtra(EXTRA_CIRCULAR_REVEAL_X, 0)
            revealY = intent.getIntExtra(EXTRA_CIRCULAR_REVEAL_Y, 0)
            val viewTreeObserver: ViewTreeObserver = mainParentLayout.viewTreeObserver
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        revealActivity(revealX, revealY)
                        mainParentLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }
        } else {
            mainParentLayout.visibility = View.VISIBLE
        }


        loadHomeFragment()

        mainNotificationTabButton.setOnClickListener {
            loadNotificationFragment()
        }

        mainHomeTabButton.setOnClickListener {
            loadHomeFragment()
        }

        mainWatcherTabButton.setOnClickListener {
            loadWatchersFragment()
        }

        mainProfileTabButton.setOnClickListener {
            loadProfileFragment()
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    override fun onResume() {
        super.onResume()
        startLocationService()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun loadNotificationFragment(){

        if(!notificationVisible){
            val transaction = fragmentManager.beginTransaction()
            mainNotificationTabButton.setImageDrawable(resources.getDrawable(R.drawable.notification_selected))
            mainHomeTabButton.setImageDrawable(resources.getDrawable(R.drawable.home))
            mainWatcherTabButton.setImageDrawable(resources.getDrawable(R.drawable.watchers))
            mainProfileTabButton.setImageDrawable(resources.getDrawable(R.drawable.profile))
            notificationVisible = true
            homeVisible = false
            watchersVisible = false
            profileVisible = false
            mainFragmentTitle.text = "Notifications"
            transaction.setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
            transaction.replace(R.id.mainFragmentContainer,NotificationFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    fun loadHomeFragment(){

        if(!homeVisible){
            val transaction = fragmentManager.beginTransaction()
            mainNotificationTabButton.setImageDrawable(resources.getDrawable(R.drawable.notification))
            mainHomeTabButton.setImageDrawable(resources.getDrawable(R.drawable.home_selected))
            mainWatcherTabButton.setImageDrawable(resources.getDrawable(R.drawable.watchers))
            mainProfileTabButton.setImageDrawable(resources.getDrawable(R.drawable.profile))
            notificationVisible = false
            homeVisible = true
            watchersVisible = false
            profileVisible = false
            mainFragmentTitle.text = "Home"
            transaction.setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
            transaction.replace(R.id.mainFragmentContainer,HomeFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    fun loadWatchersFragment(){

        if(!watchersVisible){
            val transaction = fragmentManager.beginTransaction()
            mainNotificationTabButton.setImageDrawable(resources.getDrawable(R.drawable.notification))
            mainHomeTabButton.setImageDrawable(resources.getDrawable(R.drawable.home))
            mainWatcherTabButton.setImageDrawable(resources.getDrawable(R.drawable.watchers_selected))
            mainProfileTabButton.setImageDrawable(resources.getDrawable(R.drawable.profile))
            notificationVisible = false
            homeVisible = false
            watchersVisible = true
            profileVisible = false
            mainFragmentTitle.text = "Watchers"
            transaction.setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
            transaction.replace(R.id.mainFragmentContainer,WatchersFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

    }

    fun loadProfileFragment(){

        if(!profileVisible){
            val transaction = fragmentManager.beginTransaction()
            mainNotificationTabButton.setImageDrawable(resources.getDrawable(R.drawable.notification))
            mainHomeTabButton.setImageDrawable(resources.getDrawable(R.drawable.home))
            mainWatcherTabButton.setImageDrawable(resources.getDrawable(R.drawable.watchers))
            mainProfileTabButton.setImageDrawable(resources.getDrawable(R.drawable.profile_selected))
            notificationVisible = false
            homeVisible = false
            watchersVisible = false
            profileVisible = true
            mainFragmentTitle.text = "Profile"
            transaction.setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
            transaction.replace(R.id.mainFragmentContainer,ProfileFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }


    private fun startLocationService() {
        if (!isLocationServiceRunning()) {
            val serviceIntent = Intent(baseContext, CustomLocationService::class.java)
            //        this.startService(serviceIntent);
            serviceIntent.action = 1.toString()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this@MainActivity.startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun isLocationServiceRunning(): Boolean {
        val manager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if ("com.uawdevstudios.watchovermeandroid.services.CustomLocationService" == service.service.className) {
                return true
            }
        }
        return false
    }

    fun revealActivity(x: Int, y: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val finalRadius =
                (mainParentLayout.width.coerceAtLeast(mainParentLayout.height) * 1.1).toFloat()

            // create the animator for this view (the start radius is zero)
            val circularReveal =
                ViewAnimationUtils.createCircularReveal(mainParentLayout, x, y, 0f, finalRadius)
            circularReveal.duration = 600
            circularReveal.interpolator = AccelerateInterpolator()

            // make the view visible and start the animation
            mainParentLayout.visibility = View.VISIBLE
            circularReveal.start()
        } else {
            finish()
        }
    }

    fun unRevealActivity() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            finish()
        } else {
            val finalRadius =
                (mainParentLayout.width.coerceAtLeast(mainParentLayout.height) * 1.1).toFloat()
            val circularReveal =
                ViewAnimationUtils.createCircularReveal(
                    mainParentLayout, revealX, revealY, finalRadius, 0f
                )
            circularReveal.duration = 600
            circularReveal.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mainParentLayout.visibility = View.INVISIBLE
                    finish()
                }
            })
            circularReveal.start()
        }
    }

}