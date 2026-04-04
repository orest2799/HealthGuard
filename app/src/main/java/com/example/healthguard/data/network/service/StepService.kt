package com.example.healthguard.data.network.service

import com.example.healthguard.data.network.steps.ApiStatusResponse
import com.example.healthguard.data.network.steps.StepGoalRequest
import com.example.healthguard.data.network.steps.StepGoalResponse
import com.example.healthguard.data.network.steps.StepHistorySummaryResponse
import com.example.healthguard.data.network.steps.StepSyncRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface StepsService {

    @POST("api/steps/sync")
    suspend fun syncSteps(@Body request: StepSyncRequest): Response<ApiStatusResponse>

    @GET("api/steps/history")
    suspend fun getHistory(
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<StepHistorySummaryResponse>

    @GET("api/steps/goal")
    suspend fun getGoal(): Response<StepGoalResponse>

    @POST("api/steps/goal")
    suspend fun setGoal(@Body req: StepGoalRequest): Response<StepGoalResponse>
}
