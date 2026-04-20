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

    // Fix: Return null instead of throwing an exception to prevent crashes
    private fun getUid(): String? {
        return auth.currentUser?.uid
    }

    private fun todayKey(): String {
        val calendar = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun logsRef() = getUid()?.let { uid ->
        database.reference
            .child("users")
            .child(uid)
            .child("pillLogs")
            .child(todayKey())
    }

    suspend fun markDoseTaken(timeKey: String) {
        val ref = logsRef() ?: return // Safety exit if no user logged in
        Log.d("PILL_DEBUG", "Saving taken dose for date=${todayKey()}, key=$timeKey")
        ref.child(timeKey).setValue(true).await()
    }

    suspend fun unmarkDoseTaken(timeKey: String) {
        val ref = logsRef() ?: return
        ref.child(timeKey).removeValue().await()
    }

    suspend fun getTakenDoseKeysToday(): Set<String> {
        val ref = logsRef() ?: return emptySet()
        return try {
            val snapshot = ref.get().await()
            snapshot.children.mapNotNull { it.key }.toSet()
        } catch (e: Exception) {
            Log.e("PILL_DEBUG", "Failed to load logs", e)
            emptySet()
        }
    }
}