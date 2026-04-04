package com.example.healthguard.data.network.dto


import android.util.Log
import com.example.healthguard.data.GeminiOCRParser
import com.example.healthguard.data.network.auth.FirebaseAuthHeaderInterceptor
import com.example.healthguard.data.network.auth.FirebaseTokenAuthenticator
import com.example.healthguard.data.network.service.ChatService
import com.example.healthguard.data.network.service.HealthService
import com.example.healthguard.data.network.service.MedService
import com.example.healthguard.data.network.service.ScanService
import com.example.healthguard.data.network.service.StepsService
import com.example.healthguard.data.network.service.VisionService
import com.example.healthguard.data.repo.GeminiOCRRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val LOCAL_EMULATOR = "http://10.0.2.2:8080/"
    private const val LOCAL_DEVICE = "http://192.168.1.146:8080/"
    private const val PROD_URL = "https://healthguard-final-clean-192038493071.europe-west1.run.app/"

    private const val USE_EMULATOR = false

    private const val FORCE_PROD = true

    private val BASE_URL = if (FORCE_PROD) {
        PROD_URL
    } else {
        if (USE_EMULATOR) LOCAL_EMULATOR else LOCAL_DEVICE
    }


    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun okHttpBuilder(): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)


    private fun loggingInterceptor() = Interceptor { chain ->
        val req = chain.request()

        val hasAuth = req.header("Authorization") != null
        Log.d("ApiClient", "🌐 ${req.method} ${req.url} auth=$hasAuth")

        val res = chain.proceed(req)

        Log.d("ApiClient", "✅ ${res.code}")
        return@Interceptor res
    }


    private val httpPublic: OkHttpClient = okHttpBuilder()
        .apply {  addInterceptor(loggingInterceptor()) }
        .build()

    private val httpAuthed: OkHttpClient = okHttpBuilder()
        .addInterceptor(FirebaseAuthHeaderInterceptor())
        .authenticator(FirebaseTokenAuthenticator())
        .apply { addInterceptor(loggingInterceptor()) }
        .build()

    private val retrofitPublic: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpPublic)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val retrofitAuthed: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpAuthed)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()


    val health: HealthService by lazy { retrofitPublic.create(HealthService::class.java) }


    val vision: VisionService by lazy { retrofitAuthed.create(VisionService::class.java) }
    val scan: ScanService by lazy { retrofitAuthed.create(ScanService::class.java) }
    val med: MedService by lazy { retrofitAuthed.create(MedService::class.java) }
    val chat: ChatService by lazy { retrofitAuthed.create(ChatService::class.java) }
    val steps: StepsService by lazy { retrofitAuthed.create(StepsService::class.java) }

    val ocrRepository: GeminiOCRRepository by lazy {
        GeminiOCRRepository(vision, GeminiOCRParser())
    }
}
