package com.uawdevstudios.watchovermeandroid.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

data class ServerResponse(
    @SerializedName("connection")
    @Expose
    var connection: Boolean? = false,
    @SerializedName("queryStatus")
    @Expose
    var queryStatus: Boolean? = false,
    @SerializedName("message")
    @Expose
    var message: String? = null,
    @SerializedName("data")
    @Expose
    var data: Any? = null
)