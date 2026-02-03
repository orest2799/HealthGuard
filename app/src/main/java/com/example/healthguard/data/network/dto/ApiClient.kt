package com.example.healthguard.data.network.dto

import VisionService
import com.example.healthguard.data.GeminiOCRParser
import com.example.healthguard.data.repo.GeminiOCRRepository
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
    private const val PROD_URL = "https://healthguard-final-clean-192038493071.europe-west1.run.app/"
    //private const val PROD_URL = "https://medicine-backend-192038493071.us-central1.run.app/"
    val ocrRepository: GeminiOCRRepository by lazy {
        GeminiOCRRepository(vision, GeminiOCRParser())
    }
    private val BASE_URL = when {
        !USE_LOCAL -> PROD_URL
        USE_EMULATOR -> LOCAL_EMULATOR
        else -> LOCAL_DEVICE
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Αυξήσαμε σε 30
        .readTimeout(60, TimeUnit.SECONDS)    // Αυξήσαμε σε 60 γιατί το Gemini αργεί
        .writeTimeout(60, TimeUnit.SECONDS)   // Αυξήσαμε σε 60 για το upload της εικόνας
        .addInterceptor { chain ->
            val request = chain.request()
            // Χρήσιμο Log για να βλέπουμε τι γίνεται στο Logcat
            println("🌐 API Request Sent: ${request.method} ${request.url}")
            val response = chain.proceed(request)
            println("✅ API Response Received: ${response.code}")
            response
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
