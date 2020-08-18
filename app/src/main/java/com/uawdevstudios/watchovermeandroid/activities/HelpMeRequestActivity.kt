package com.uawdevstudios.watchovermeandroid.activities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.services.HelpMeService
import kotlinx.android.synthetic.main.activity_help_me_request.*

class HelpMeRequestActivity : AppCompatActivity() {

    lateinit var timer: CountDownTimer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out)
        setContentView(R.layout.activity_help_me_request)

        timer = object : CountDownTimer(10*1000,1){
            override fun onFinish() {
                startHelpMeService()
                val intent = Intent(baseContext, MainActivity::class.java)
                intent.putExtra("Fragment","Notification")
                startActivity(intent)
                finish()
            }

            override fun onTick(p0: Long) {
                helpMeTimer.text = (p0/1000).toString()
                helpMeProgress.progress = (p0/100).toInt()
            }

        }.start()

        helpMeCancelButton.setOnClickListener {
            timer.cancel()
            val intent = Intent(baseContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    override fun onBackPressed() {

        timer.cancel()
        val intent = Intent(baseContext, MainActivity::class.java)
        startActivity(intent)
        finish()

    }

    private fun startHelpMeService() {
        if (!isHelpMeServiceRunning()) {
            val serviceIntent = Intent(baseContext, HelpMeService::class.java)

            serviceIntent.action = 2.toString()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun isHelpMeServiceRunning(): Boolean {
        val manager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if ("com.uawdevstudios.watchovermeandroid.services.HelpMeService" == service.service.className) {
                return true
            }
        }
        return false
    }


}