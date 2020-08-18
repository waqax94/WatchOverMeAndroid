package com.uawdevstudios.watchovermeandroid.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import com.uawdevstudios.watchovermeandroid.R

class HelpMeRequestActivity : AppCompatActivity() {

    val string = "black"
    lateinit var timer: CountDownTimer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out)
        setContentView(R.layout.activity_help_me_request)
    }


}