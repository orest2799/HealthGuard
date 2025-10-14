package com.example.healthguard.data.network.dto

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VisionService {
    @Multipart
    @POST("vision/annotate")
    suspend fun annotate(@Part file: MultipartBody.Part): VisionDto
}
