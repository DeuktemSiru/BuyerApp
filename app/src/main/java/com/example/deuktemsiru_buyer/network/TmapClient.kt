package com.example.deuktemsiru_buyer.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TmapClient {
    val api: TmapApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://apis.openapi.sk.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmapApiService::class.java)
    }
}