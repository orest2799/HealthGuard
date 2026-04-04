package com.example.healthguard.data.network.steps

import android.content.Context
import com.example.healthguard.data.repo.StepRepository

object Injection {

    @Volatile
    private var stepRepository: StepRepository? = null

    @Volatile
    private var currentUserId: String? = null

    fun provideStepRepository(context: Context, userId: String): StepRepository {

        if (userId != currentUserId) {
            synchronized(this) {
                if (userId != currentUserId) {
                    stepRepository = null
                    currentUserId = userId
                }
            }
        }

        return stepRepository ?: synchronized(this) {
            stepRepository ?: createStepRepository(context.applicationContext, userId)
                .also { stepRepository = it }
        }
    }


    fun reset() {
        synchronized(this) {
            stepRepository = null
            currentUserId = null
        }
    }

    private fun createStepRepository(context: Context, userId: String): StepRepository {
        val prefsManager = StepPrefsManager(context, userId)
        return StepRepository(prefsManager)
    }
}