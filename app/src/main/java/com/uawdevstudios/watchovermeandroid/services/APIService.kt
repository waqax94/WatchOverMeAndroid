package com.uawdevstudios.watchovermeandroid.services

import com.uawdevstudios.watchovermeandroid.models.ServerResponse
import com.uawdevstudios.watchovermeandroid.models.Watcher
import retrofit2.Call
import retrofit2.http.*

interface APIService {
    // Path parameter in get request
    // @GET("destination/{id}")
    // fun getDestination(@Path("id")id: Int): Call<Destination>

    // Query parameter in get request
    // @GET("destination/{id}")
    // fun getDestination(@Query("id")id: Int): Call<Destination>

    // Multiple Query parameters in get request
    // @GET("destination")
    // fun getDestination(@Query("param1") param1: String?, @Query("param2") param2: String?): Call<Destination>

    // Query Map
    // @GET("destination")
    // fun getDestination(@QueryMap filter: HashMap<String, String>): Call<Destination>
    // Defining hash map
    //val filter = HashMap<String, String>()
    //filter["param1"] = "value1"
    //filter["param2"] = "value2"


    //fun addDestination(@Body newDestination: Destination): Call<Destination>

    @GET("testConnection")
    fun testConnection(): Call<String>

    @FormUrlEncoded
    @POST("wearerLoginProcessing")
    fun loginProcessing(
        @Field("wearerPhone") wearerPhone: String?,
        @Field("wearerPassword") wearerPassword: String?
    ): Call<ServerResponse>

    @FormUrlEncoded
    @POST("setLoginStatus")
    fun setLoginStatus(
        @Field("serviceId") serviceId: String?,
        @Field("status") status: String?
    ): Call<String>

    @FormUrlEncoded
    @POST("checkLoginStatus")
    fun checkLoginStatus(
        @Field("serviceId") serviceId: String?
    ): Call<String>

    @FormUrlEncoded
    @POST("getWatchers")
    fun getWatchers(@Field("serviceId") serviceId: String?): Call<ServerResponse>

    @FormUrlEncoded
    @POST("updateDeviceToken")
    fun updateDeviceToken(
        @Field("serviceId") serviceId: String?,
        @Field("deviceToken") deviceToken: String?
    ): Call<String>


    @FormUrlEncoded
    @POST("deactivateHelpMeRequest")
    fun deactivateHelpMeRequest(
        @Field("serviceId") serviceId: String?
    ): Call<String>

    @FormUrlEncoded
    @POST("helpMeRequestInitiate")
    fun helpMeRequestInitiate(
        @Field("batteryLevel") batteryLevel: String?,
        @Field("locationLatitude") locationLatitude: String?,
        @Field("locationLongitude") locationLongitude: String?,
        @Field("locality") locality: String?,
        @Field("logText") logText: String?,
        @Field("logDate") logDate: String?,
        @Field("logTime") logTime: String?,
        @Field("logType") logType: String?,
        @Field("serviceId") serviceId: String?
    ): Call<ServerResponse>

    @FormUrlEncoded
    @POST("regularLog")
    fun regularLog(
        @Field("batteryLevel") batteryLevel: String?,
        @Field("locationLatitude") locationLatitude: String?,
        @Field("locationLongitude") locationLongitude: String?,
        @Field("locality") locality: String?,
        @Field("logText") logText: String?,
        @Field("logDate") logDate: String?,
        @Field("logTime") logTime: String?,
        @Field("logType") logType: String?,
        @Field("serviceId") serviceId: String?
    ): Call<String>

    @FormUrlEncoded
    @POST("contactWatcher")
    fun contactWatcher(
        @Field("serviceId") serviceId: String?,
        @Field("responseTitle") responseTitle: String?,
        @Field("responseText") responseText: String?,
        @Field("watcherId") watcherId: String?,
        @Field("cycle") cycle: String?,
        @Field("alertLogId") alertLogId: String?,
        @Field("wearerId") wearerId: String?,
        @Field("sendDate") sendDate: String?,
        @Field("sendTime") sendTime: String?,
        @Field("responseLink") responseLink: String?,
        @Field("watcherPhone") watcherPhone: String?,
        @Field("wearerFullName") wearerFullName: String?
        ): Call<ServerResponse>

    @FormUrlEncoded
    @POST("wearerNotification")
    fun wearerNotification(
        @Field("serviceId") serviceId: String?,
        @Field("notificationTitle") notificationTitle: String?,
        @Field("notificationText") notificationText: String?
    ): Call<String>

    @FormUrlEncoded
    @POST("sendLocation")
    fun sendLocation(
        @Field("serviceId") serviceId: String?,
        @Field("receiverId") receiverId: String?,
        @Field("latitude") latitude: String?,
        @Field("longitude") longitude: String?,
        @Field("address") address: String?
    ): Call<String>
}