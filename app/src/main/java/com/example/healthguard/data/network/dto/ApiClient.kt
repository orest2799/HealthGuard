package com.example.healthguard.data.network.dto

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // TODO: set your PC/LAN IP that the phone can reach
    private const val BASE_URL = "http://192.168.1.146:8080/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(http)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // Expose service singletons so UI/Repos never touch Retrofit directly
    val vision: VisionService by lazy { retrofit.create(VisionService::class.java) }
    val scan:   ScanService   by lazy { retrofit.create(ScanService::class.java) }
    val med:    MedService    by lazy { retrofit.create(MedService::class.java) }
}
