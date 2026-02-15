package com.example.studymate

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    //private const val BASE_URL = "http://10.172.242.65:3000/"
    private const val BASE_URL = "http://192.168.100.45:3000/"
    // 10.0.2.2 = localhost for Android emulator

    val api: ChatApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApi::class.java)
    }
}