package app.models

data class StepSyncRequest(
    val date: String, // YYYY-MM-DD
    val steps: Int
)

data class StepGoalRequest(
    val dailyTarget: Int
)

data class StepGoalResponse(
    val userId: String,
    val dailyTarget: Int
)

data class StepHistoryItem(
    val date: String,
    val steps: Int
)

data class StepHistorySummaryResponse(
    val userId: String,
    val startDate: String,
    val endDate: String,
    val dailyTarget: Int,
    val data: List<StepHistoryItem>,
    val total: Int,
    val average: Double,           // avg over ALL days in range (incl. zero-step days)
    val averageActiveOnly: Double, // avg over days where steps > 0
    val daysWithData: Int,
    val goalCompletionPercentage: Double,
    val goalReachedDays: Int,
    val bestDay: BestDay?,
    val currentStreak: Int
)

data class BestDay(
    val date: String,
    val steps: Int
)