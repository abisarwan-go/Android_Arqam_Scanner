package com.example.myapplication.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitClient {
    private const val BASE_URL = "http://192.168.43.229:3000"
    private const val ARQAM_BASE_URL = "http://192.168.0.29:3000"
    private var retrofit: Retrofit? = null

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()

    fun getClient(): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(ARQAM_BASE_URL)  // Set the base URL
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())  // For JSON parsing
                .build()
        }
        return retrofit!!
    }

}

