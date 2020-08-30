package com.uawdevstudios.watchovermeandroid.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Watcher(
    @SerializedName("person_id")
    @Expose
    var watcherId: String? = null,
    @SerializedName("f_name")
    @Expose
    var watcherFirstName: String? = null,
    @SerializedName("l_name")
    @Expose
    var watcherLastName: String? = null,
    @SerializedName("email")
    @Expose
    var watcherEmail: String? = null,
    @SerializedName("phone")
    @Expose
    var watcherPhone: String? = null,
    @SerializedName("priority_num")
    @Expose
    var watcherPriorityNum: String? = null
)