package com.example.healthguard.data.network.steps

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StepPrefsManager(context: Context, userId: String) {

    private val prefs = context.getSharedPreferences("step_data_$userId", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveDailyGoal(goal: Int) {
        prefs.edit { putInt("daily_goal", goal) }
    }

    fun getDailyGoal(): Int {
        return prefs.getInt("daily_goal", 0)
    }

    fun setBaseline(date: String, baseline: Int) {
        prefs.edit { putInt("baseline_$date", baseline) }
    }

    fun getBaseline(date: String): Int = prefs.getInt("baseline_$date", -1)

    fun getLastSyncedSteps(date: String): Int =
        prefs.getInt("last_synced_steps_$date", -1)

    fun setLastSyncedSteps(date: String, steps: Int) {
        prefs.edit { putInt("last_synced_steps_$date", steps) }
    }

    fun getLastSyncTime(date: String): Long =
        prefs.getLong("last_sync_time_$date", 0L)

    fun setLastSyncTime(date: String, time: Long) {
        prefs.edit { putLong("last_sync_time_$date", time) }
    }

    private fun trimHistory(history: MutableMap<String, Int>, keepDays: Int = 90) {
        val keysSorted = history.keys.sorted()
        val extra = keysSorted.size - keepDays
        if (extra > 0) {
            for (k in keysSorted.take(extra)) history.remove(k)
        }
    }

    fun saveDailySteps(date: String, count: Int) {
        val history = getFullHistory().toMutableMap()
        history[date] = count
        trimHistory(history, keepDays = 90)
        prefs.edit { putString("history_map", gson.toJson(history)) }
    }

    fun getFullHistory(): Map<String, Int> {
        val json = prefs.getString("history_map", "{}")
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return try {
            gson.fromJson<Map<String, Int>>(json, type) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }
}