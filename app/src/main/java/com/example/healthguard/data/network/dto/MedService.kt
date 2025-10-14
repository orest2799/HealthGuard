package com.example.healthguard.data.network.dto

import retrofit2.http.GET
import retrofit2.http.Query

interface MedService {
    @GET("meds/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("lang") lang: String? = null,
        @Query("source") source: String? = null
    ): MedSearchResponse
}
