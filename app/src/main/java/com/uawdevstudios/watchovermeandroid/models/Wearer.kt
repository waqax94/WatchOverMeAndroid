package com.uawdevstudios.watchovermeandroid.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Wearer (
    @SerializedName("wearerId")
    @Expose
    var wearerId: String? = null,
    @SerializedName("serviceId")
    @Expose
    var serviceId: String? = null,
    @SerializedName("wearerFirstName")
    @Expose
    var wearerFirstName: String? = null,
    @SerializedName("wearerLastName")
    @Expose
    var wearerLastName: String? = null,
    @SerializedName("wearerEmail")
    @Expose
    var wearerEmail: String? = null,
    @SerializedName("wearerPhone")
    @Expose
    var wearerPhone: String? = null
)