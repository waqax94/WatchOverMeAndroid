package com.uawdevstudios.watchovermeandroid.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_splash_screen.*
import kotlinx.android.synthetic.main.progress_view.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler().postDelayed({
            splashScreenLoading.visibility = View.VISIBLE
            loginMethod()
        }, 200)

    }


    fun presentActivity(view: View, activityClass: Class<out Activity>) {
        var options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, view, "transition")

        val revealX = (view.x + view.width / 2).toInt()
        val revealY = (view.y + view.height / 2).toInt()

        val intent = Intent(baseContext, activityClass)

        intent.putExtra("EXTRA_CIRCULAR_REVEAL_X", revealX)
        intent.putExtra("EXTRA_CIRCULAR_REVEAL_Y", revealY)

        ActivityCompat.startActivity(this, intent, options.toBundle())
    }

    override fun onBackPressed() {

    }

    fun loginMethod() {

        val loginData =
            getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
        val wearerPhone = loginData.getString("wearerPhone", "")
        val wearerPassword = loginData.getString("wearerPassword", "")


        val apiService: APIService = ServiceBuilder.buildService(APIService::class.java)
        val requestCall = apiService.loginProcessing(wearerPhone, wearerPassword);

        requestCall.enqueue(object : Callback<ServerResponse> {

            override fun onResponse(
                call: Call<ServerResponse>,
                response: Response<ServerResponse>
            ) {
                val serverResponse = response.body()
                if (serverResponse != null) {
                    if (serverResponse.connection == true && serverResponse.queryStatus == true) {

                        val wearerType = object : TypeToken<Wearer>() {}.type

                        val wearer: Wearer =
                            Gson().fromJson(serverResponse.data.toString(), wearerType)

                        val requestCall2 = apiService.setLoginStatus(wearer.serviceId,"true")

                        requestCall2.enqueue(object : Callback<String>{
                            override fun onResponse(
                                call: Call<String>,
                                response: Response<String>
                            ) {
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
                                presentActivity(splashScreenIcon, MainActivity::class.java)
                            }

                            override fun onFailure(call: Call<String>, t: Throwable) {
                                val intent = Intent(baseContext, NoConnectionActivity::class.java)
                                startActivity(intent)
                            }

                        })

                    } else {
                        presentActivity(splashScreenIcon, LoginActivity::class.java)
                    }
                } else {
                    presentActivity(splashScreenIcon, LoginActivity::class.java)
                }
            }
            override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
                val intent = Intent(baseContext, NoConnectionActivity::class.java)
                startActivity(intent)
            }

        })
    }
}