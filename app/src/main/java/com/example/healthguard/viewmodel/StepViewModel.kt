package com.example.healthguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.repo.StepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class StepViewModel(
    private val repository: StepRepository
) : ViewModel() {

    private var persistJob: Job? = null
    private val _stepState = MutableStateFlow<StepUiState>(StepUiState.Loading)
    val stepState: StateFlow<StepUiState> = _stepState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val start = LocalDate.now().minusDays(29).toString()

            // Step 1 — show local data immediately (fast path)
            val localHistory = withContext(Dispatchers.IO) { repository.getHistory() }
            val localToday = localHistory[today] ?: 0
            val localMonthly = localHistory.values.sum()

            _stepState.value = StepUiState.Success(
                todaySteps = localToday,
                dailyTarget = repository.getDailyTargetOrDefault(),
                weeklyAvg = 0.0,
                monthlyTotal = localMonthly,
                fullHistory = localHistory,
                currentStreak = 0,
                goalReachedDays = 0,
                goalCompletionPercentage = 0.0,
                bestDayDate = null,
                bestDaySteps = null,
                averageActiveOnly = 0.0,
                daysWithData = 0
            )

            val (goal, summary) = withContext(Dispatchers.IO) {
                val goalDeferred = async { repository.fetchGoal(defaultGoal = repository.getDailyTargetOrDefault()) }
                val historyDeferred = async { repository.fetchHistory(start, today) }
                goalDeferred.await() to historyDeferred.await()
            }

            val cur = _stepState.value
            if (cur is StepUiState.Success) {
                _stepState.value = cur.copy(dailyTarget = goal)
            }

            val historyMap = summary?.data?.associate { it.date to it.steps }
                ?: return@launch

            val backendToday = historyMap[today] ?: 0
            val mergedToday = maxOf(localToday, backendToday)

            val mergedHistory = localHistory.toMutableMap()
            for ((date, steps) in historyMap) {
                mergedHistory[date] = maxOf(mergedHistory[date] ?: 0, steps)
            }
            mergedHistory[today] = mergedToday

            val last7Dates = (0..6).map { LocalDate.now().minusDays(it.toLong()).toString() }
            val last7Steps = last7Dates.map { d -> mergedHistory[d] ?: 0 }
            val weeklyAvg = last7Steps.average()

            val dailyTarget = summary.dailyTarget.takeIf { it > 0 } ?: goal

            _stepState.value = StepUiState.Success(
                todaySteps = mergedToday,
                dailyTarget = dailyTarget,
                weeklyAvg = weeklyAvg,
                monthlyTotal = mergedHistory.values.sum(),
                fullHistory = mergedHistory,
                currentStreak = summary.currentStreak,
                goalReachedDays = summary.goalReachedDays,
                goalCompletionPercentage = summary.goalCompletionPercentage,
                // Store raw values — formatted in the UI layer with stringResource()
                bestDayDate = summary.bestDay?.date,
                bestDaySteps = summary.bestDay?.steps,
                averageActiveOnly = summary.averageActiveOnly,
                daysWithData = summary.daysWithData
            )
        }
    }

    fun onStepDetected(totalSinceBoot: Int) {
        val today = LocalDate.now().toString()
        val baseline = repository.getOrCreateBaseline(today, totalSinceBoot)
        val todaySteps = (totalSinceBoot - baseline).coerceAtLeast(0)

        val current = _stepState.value
        if (current is StepUiState.Success) {
            _stepState.value = current.copy(todaySteps = todaySteps)
        }

        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(750)
            withContext(Dispatchers.IO) {
                repository.updateSteps(todaySteps)
            }
        }
    }

    fun updateGoal(newGoal: Int) {
        val safe = newGoal.coerceIn(1, 100_000)

        val cur = _stepState.value
        if (cur is StepUiState.Success) {
            _stepState.value = cur.copy(dailyTarget = safe)
        }

        viewModelScope.launch {
            repository.updateGoal(safe)
        }
    }
}

sealed class StepUiState {
    data object Loading : StepUiState()

    data class Success(
        val todaySteps: Int,
        val dailyTarget: Int,
        val weeklyAvg: Double,
        val monthlyTotal: Int,
        val fullHistory: Map<String, Int>,
        val currentStreak: Int,
        val goalReachedDays: Int,
        val goalCompletionPercentage: Double,
        // Raw values — formatted in StatsScreen using stringResource()
        val bestDayDate: String?,
        val bestDaySteps: Int?,
        val averageActiveOnly: Double,
        val daysWithData: Int
    ) : StepUiState()
}