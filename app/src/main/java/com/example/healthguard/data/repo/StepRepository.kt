package com.example.healthguard.data.repo

import com.example.healthguard.data.network.dto.ApiClient
import com.example.healthguard.data.network.steps.StepGoalRequest
import com.example.healthguard.data.network.steps.StepHistorySummaryResponse
import com.example.healthguard.data.network.steps.StepPrefsManager
import com.example.healthguard.data.network.steps.StepSyncRequest
import java.time.LocalDate

class StepRepository(private val prefsManager: StepPrefsManager) {

    companion object {

        const val DEFAULT_DAILY_TARGET = 5000
    }

    private fun todayIso(): String = LocalDate.now().toString()

    suspend fun updateSteps(currentSteps: Int): Boolean {
        val today = todayIso()
        val safeSteps = maxOf(0, currentSteps)

        prefsManager.saveDailySteps(today, safeSteps)

        val now = System.currentTimeMillis()
        val lastSyncTime = prefsManager.getLastSyncTime(today)
        val lastSyncedSteps = prefsManager.getLastSyncedSteps(today)

        val timeOk = (now - lastSyncTime) >= 30_000
        val stepsOk = (lastSyncedSteps < 0) || (safeSteps >= lastSyncedSteps + 100)

        if (!timeOk && !stepsOk) return true

        return try {
            val response = ApiClient.steps.syncSteps(
                StepSyncRequest(date = today, steps = safeSteps)
            )
            if (response.isSuccessful) {
                prefsManager.setLastSyncTime(today, now)
                prefsManager.setLastSyncedSteps(today, safeSteps)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun getOrCreateBaseline(date: String, sensorTotal: Int): Int {
        val existing = prefsManager.getBaseline(date)
        if (existing >= 0) {
            // Reboot detection: sensor reset means sensorTotal < stored baseline
            if (sensorTotal < existing) {
                prefsManager.setBaseline(date, sensorTotal)
                return sensorTotal
            }
            return existing
        }
        prefsManager.setBaseline(date, sensorTotal)
        return sensorTotal
    }

    // Fixed: default is now 5000 everywhere, matching the backend
    fun getDailyTargetOrDefault(defaultTarget: Int = DEFAULT_DAILY_TARGET): Int {
        val local = prefsManager.getDailyGoal()
        return if (local > 0) local else defaultTarget
    }

    suspend fun updateGoal(newGoal: Int): Boolean {
        val safe = newGoal.coerceIn(1, 100_000)
        prefsManager.saveDailyGoal(safe)
        return try {
            val resp = ApiClient.steps.setGoal(StepGoalRequest(dailyTarget = safe))
            resp.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchHistory(start: String, end: String): StepHistorySummaryResponse? {
        return try {
            LocalDate.parse(start)
            LocalDate.parse(end)
            val response = ApiClient.steps.getHistory(start, end)
            if (response.isSuccessful) response.body() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchGoal(defaultGoal: Int = DEFAULT_DAILY_TARGET): Int {
        val local = prefsManager.getDailyGoal().takeIf { it > 0 } ?: defaultGoal
        return try {
            val response = ApiClient.steps.getGoal()
            android.util.Log.d("GOAL", "GET /steps/goal code=${response.code()}")
            if (response.isSuccessful) {
                val goal = response.body()?.dailyTarget ?: local
                prefsManager.saveDailyGoal(goal)
                goal
            } else {
                local
            }
        } catch (_: Exception) {
            local
        }
    }

    fun getHistory(): Map<String, Int> = prefsManager.getFullHistory()
}