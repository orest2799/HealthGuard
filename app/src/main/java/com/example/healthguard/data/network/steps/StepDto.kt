package com.example.healthguard.data.network.steps

data class ApiStatusResponse(
    val status: String? = null,
    val message: String? = null
)

data class StepSyncRequest(val date: String, val steps: Int)

data class StepGoalRequest(val dailyTarget: Int)

data class StepGoalResponse(val userId: String, val dailyTarget: Int)

data class StepHistoryItem(val date: String, val steps: Int)

data class BestDay(val date: String, val steps: Int)

data class StepHistorySummaryResponse(
    val userId: String,
    val startDate: String,
    val endDate: String,
    val dailyTarget: Int = 0,
    val data: List<StepHistoryItem> = emptyList(),
    val total: Int = 0,
    val average: Double = 0.0,
    val averageActiveOnly: Double = 0.0,
    val daysWithData: Int = 0,
    val goalCompletionPercentage: Double = 0.0,
    val goalReachedDays: Int = 0,
    val bestDay: BestDay? = null,
    val currentStreak: Int = 0
)