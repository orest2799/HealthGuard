package com.example.healthguard.data.network.service

import com.example.healthguard.data.network.dto.GalinosResponse
import com.example.healthguard.data.network.dto.MedSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MedService {
    /**
     * Search medicines using EMA/OpenFDA databases
     */
    @GET("meds/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("lang") lang: String? = null,
        @Query("source") source: String? = null
    ): MedSearchResponse

    /**
     * Search Galinos (Greek medicine database) for detailed information
     */
    @GET("meds/galinos")
    suspend fun searchGalinos(
        @Query("q") q: String
    ): GalinosResponse
}