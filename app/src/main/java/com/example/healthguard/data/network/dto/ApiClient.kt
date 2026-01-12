package com.example.healthguard.data.network.dto

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit



object ApiClient {

    private const val USE_LOCAL = false
    private const val USE_EMULATOR = false

    private const val LOCAL_EMULATOR = "http://10.0.2.2:8080/"
    private const val LOCAL_DEVICE = "http://192.168.1.146:8080/"

    private const val PROD_URL =
        "https://healthguard-backend-192038493071.europe-west1.run.app/"

    private val BASE_URL = when {
        !USE_LOCAL -> PROD_URL
        USE_EMULATOR -> LOCAL_EMULATOR
        else -> LOCAL_DEVICE
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            println("🌐 API Request: ${request.method} ${request.url}")
            chain.proceed(request)
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(http)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val vision: VisionService by lazy { retrofit.create(VisionService::class.java) }
    val scan: ScanService by lazy { retrofit.create(ScanService::class.java) }
    val med: MedService by lazy { retrofit.create(MedService::class.java) }
    val health: HealthService by lazy { retrofit.create(HealthService::class.java) }
    val chat: ChatService by lazy { retrofit.create(ChatService::class.java) }
}
