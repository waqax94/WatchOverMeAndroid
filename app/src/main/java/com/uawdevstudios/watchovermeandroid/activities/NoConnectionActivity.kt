package com.uawdevstudios.watchovermeandroid.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.models.ServerResponse
import com.uawdevstudios.watchovermeandroid.models.Wearer
import com.uawdevstudios.watchovermeandroid.services.APIService
import com.uawdevstudios.watchovermeandroid.services.ServiceBuilder
import kotlinx.android.synthetic.main.activity_no_connection.*
import kotlinx.android.synthetic.main.activity_splash_screen.*
import kotlinx.android.synthetic.main.progress_view.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NoConnectionActivity : AppCompatActivity() {

    lateinit var progressDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_connection)

        val progressDialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.progress_view, null)
        dialogView.loadingText.text = "Reconnecting..."
        Glide.with(applicationContext)
            .load(R.drawable.loading)
            .placeholder(R.drawable.loading)
            .centerCrop()
            .into(dialogView.loadingIcon)
        progressDialogBuilder.setView(dialogView)
        progressDialogBuilder.setCancelable(false)
        progressDialog = progressDialogBuilder.create()


        noConnectionButton.setOnClickListener {
            loginMethod()
        }
    }


    override fun onBackPressed() {
        finishAffinity()
    }

    fun loginMethod() {
        val loginData =
            getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        val wearerPhone = loginData.getString("wearerPhone", "")
        val wearerPassword = loginData.getString("wearerPassword", "")

        progressDialog.show()

        val apiService: APIService = ServiceBuilder.buildService(APIService::class.java)
        val requestCall = apiService.loginProcessing(wearerPhone, wearerPassword);

        requestCall.enqueue(object : Callback<ServerResponse> {

            override fun onResponse(
                call: Call<ServerResponse>,
                response: Response<ServerResponse>
            ) {
                progressDialog.hide()
                val serverResponse = response.body()
                if (serverResponse != null) {
                    if (serverResponse.connection == true && serverResponse.queryStatus == true) {
                        val wearerType = object : TypeToken<Wearer>() {}.type

                        val wearer: Wearer =
                            Gson().fromJson(serverResponse.data.toString(), wearerType)

                        val userData =
                            getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
                        val editor = userData.edit()
                        editor.putString("wearerId", wearer.wearerId)
                        editor.putString("serviceId", wearer.serviceId)
                        editor.putString("wearerFirstName", wearer.wearerFirstName)
                        editor.putString("wearerLastName", wearer.wearerLastName)
                        editor.putString("wearerEmail", wearer.wearerEmail)
                        editor.putString("wearerPhone", wearer.wearerPhone)
                        editor.putString("wearerPassword", wearerPassword)
                        editor.apply()
                        val intent = Intent(baseContext, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        val intent = Intent(baseContext, LoginActivity::class.java)
                        startActivity(intent)
                    }
                } else {
                    val intent = Intent(baseContext, LoginActivity::class.java)
                    startActivity(intent)
                }
            }

            override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
                progressDialog.hide()
            }

        })
    }

}