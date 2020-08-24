package com.uawdevstudios.watchovermeandroid.services

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceBuilder {
    //"http://192.168.0.106/FoodPalm/"

    private const val URL = "http://192.168.0.105/api/"

    // Create Logger
    private val logger = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

    // Create okhttp client
    private val okHttp: OkHttpClient.Builder = OkHttpClient.Builder().addInterceptor(logger)

    // Create Retrofit builder
    private val builder: Retrofit.Builder = Retrofit.Builder().baseUrl(URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttp.build())

    // Create retrofit instance
    private val retrofit: Retrofit = builder.build()

    fun <T> buildService(serviceType: Class<T>): T {
        return retrofit.create(serviceType)
    }

}