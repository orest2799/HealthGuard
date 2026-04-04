package com.example.healthguard.data.network.pills

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class PillLogFirebaseDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User is not signed in")
    }

    private fun todayKey(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return "%04d-%02d-%02d".format(year, month, day)
    }

    private fun logsRef() =
        database.reference
            .child("users")
            .child(requireUid())
            .child("pillLogs")
            .child(todayKey())

    suspend fun markDoseTaken(timeKey: String) {
        Log.d("PILL_DEBUG", "Saving taken dose for date=${todayKey()}, key=$timeKey")

        logsRef()
            .child(timeKey)
            .setValue(true)
            .await()
    }

    suspend fun unmarkDoseTaken(timeKey: String) {
        logsRef()
            .child(timeKey)
            .removeValue()
            .await()
    }

    suspend fun getTakenDoseKeysToday(): Set<String> {
        val snapshot = logsRef().get().await()
        val result = snapshot.children.mapNotNull { it.key }.toSet()

        Log.d("PILL_DEBUG", "Loading taken doses for date=${todayKey()} -> $result")

        return result
    }
}