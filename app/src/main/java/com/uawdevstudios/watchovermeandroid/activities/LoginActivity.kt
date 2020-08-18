package com.uawdevstudios.watchovermeandroid.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.uawdevstudios.watchovermeandroid.R
import com.uawdevstudios.watchovermeandroid.models.NotificationItem
import com.uawdevstudios.watchovermeandroid.models.ServerResponse
import com.uawdevstudios.watchovermeandroid.models.Wearer
import com.uawdevstudios.watchovermeandroid.services.APIService
import com.uawdevstudios.watchovermeandroid.services.ServiceBuilder
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.progress_view.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Exception


class LoginActivity : AppCompatActivity() {

    private val EXTRA_CIRCULAR_REVEAL_X = "EXTRA_CIRCULAR_REVEAL_X"
    private val EXTRA_CIRCULAR_REVEAL_Y = "EXTRA_CIRCULAR_REVEAL_Y"
    lateinit var progressDialog: AlertDialog

    var revealX: Int = 0
    var revealY: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            intent.hasExtra(EXTRA_CIRCULAR_REVEAL_X) &&
            intent.hasExtra(EXTRA_CIRCULAR_REVEAL_Y)
        ) {
            revealX = intent.getIntExtra(EXTRA_CIRCULAR_REVEAL_X, 0)
            revealY = intent.getIntExtra(EXTRA_CIRCULAR_REVEAL_Y, 0)
            val viewTreeObserver: ViewTreeObserver = loginParentLayout.viewTreeObserver
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        revealActivity(revealX, revealY)
                        loginParentLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }
        } else {
            loginParentLayout.visibility = View.VISIBLE
            LoginActivity().finish()
        }

        val progressDialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.progress_view, null)
        dialogView.loadingText.text = "Authenticating"
        Glide.with(applicationContext)
            .load(R.drawable.loading)
            .placeholder(R.drawable.loading)
            .centerCrop()
            .crossFade()
            .into(dialogView.loadingIcon)
        progressDialogBuilder.setView(dialogView)
        progressDialogBuilder.setCancelable(false)
        progressDialog = progressDialogBuilder.create()


        loginCCP.registerCarrierNumberEditText(loginPhone)
        loginCCP.setNumberAutoFormattingEnabled(true)

        loginPhone.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                loginPhone.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.textfield_bg1, null)
                loginErrorLayout.visibility = View.INVISIBLE
            }
        })

        loginPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                loginPassword.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.textfield_bg1, null)
                loginErrorLayout.visibility = View.INVISIBLE
            }

        })

        loginParentLayout.setOnClickListener {
            hideKeybord()
        }

        loginMainLayout.setOnClickListener {
            hideKeybord()
        }


        loginButton.setOnClickListener {

            if (loginPhone.text.toString() != "") {

                if (loginPassword.text.toString() != "") {

                    progressDialog.show()
                    val apiService: APIService = ServiceBuilder.buildService(APIService::class.java)
                    val requestCall: Call<ServerResponse> = apiService.loginProcessing(
                        loginCCP.fullNumberWithPlus,
                        loginPassword.text.toString()
                    )

                    requestCall.enqueue(object : Callback<ServerResponse> {

                        override fun onResponse(
                            call: Call<ServerResponse>,
                            response: Response<ServerResponse>
                        ) {
                            progressDialog.hide()
                            if (response.body() != null) {
                                val serverResponse: ServerResponse? = response.body()

                                if (serverResponse!!.connection == true && serverResponse.queryStatus == true) {

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
                                    editor.putString(
                                        "wearerPassword",
                                        loginPassword.text.toString()
                                    )
                                    editor.apply()
                                    updateTokenOnServer()
                                    val intent = Intent(baseContext, MainActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    loginErrorMessage.text = serverResponse.message
                                    loginErrorLayout.visibility = View.VISIBLE
                                }

                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "Server error",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }

                        override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
                            progressDialog.hide()
                            Toast.makeText(applicationContext, "No Connection", Toast.LENGTH_SHORT)
                                .show()
                        }

                    })

                } else {
                    loginPassword.background =
                        ResourcesCompat.getDrawable(resources, R.drawable.textfield_bg2, null)
                }
            } else {
                loginPhone.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.textfield_bg2, null)
            }

        }

    }

    override fun onBackPressed() {
        finishAffinity()
    }

    fun revealActivity(x: Int, y: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val finalRadius =
                (loginParentLayout.width.coerceAtLeast(loginParentLayout.height) * 1.1).toFloat()

            // create the animator for this view (the start radius is zero)
            val circularReveal =
                ViewAnimationUtils.createCircularReveal(loginParentLayout, x, y, 0f, finalRadius)
            circularReveal.duration = 600
            circularReveal.interpolator = AccelerateInterpolator()

            // make the view visible and start the animation
            loginParentLayout.visibility = View.VISIBLE
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
                (loginParentLayout.width.coerceAtLeast(loginParentLayout.height) * 1.1).toFloat()
            val circularReveal =
                ViewAnimationUtils.createCircularReveal(
                    loginParentLayout, revealX, revealY, finalRadius, 0f
                )
            circularReveal.duration = 600
            circularReveal.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    loginParentLayout.visibility = View.INVISIBLE
                    finish()
                }
            })
            circularReveal.start()
        }
    }

    private fun hideKeybord() {
        var imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        try {
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun updateTokenOnServer(){

        val tokenInstance = FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
            val token = it.token

            val loginData =
                applicationContext.getSharedPreferences("wearerInfo", Context.MODE_PRIVATE)
            val serviceId = loginData.getString("serviceId", "")
            val apiService = ServiceBuilder.buildService(APIService::class.java)
            val requestCall = apiService.updateDeviceToken(serviceId,token)

            requestCall.enqueue(object: Callback<String> {

                override fun onResponse(call: Call<String>, response: Response<String>) {

                }

                override fun onFailure(call: Call<String>, t: Throwable) {

                }

            })

        }
    }

}
