package app.services

import com.google.cloud.firestore.FieldPath
import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.firebase.cloud.FirestoreClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseService {
    private val db: Firestore by lazy { FirestoreClient.getFirestore() }

    private companion object {
        const val DEFAULT_DAILY_TARGET = 5000
    }

    suspend fun syncStepsToCloud(userId: String, date: String, steps: Int) {
        requireIsoDate(date)
        println("DEBUG: Sync - User: $userId, Date: $date, Steps: $steps")

        val docRef = db.collection("users").document(userId)
            .collection("daily_stats").document(date)

        withContext(Dispatchers.IO) {
            try {
                val snap = docRef.get().get()

                val existing = if (snap.exists()) (snap.getLong("steps") ?: 0L).toInt() else 0
                val finalSteps = maxOf(existing, steps)

                val data = mapOf(
                    "steps" to finalSteps,
                    "lastUpdated" to FieldValue.serverTimestamp()
                )

                docRef.set(data, SetOptions.merge()).get()

                println("✅ Firestore updated for $userId on $date. Stored: $finalSteps (incoming=$steps, existing=$existing)")
            } catch (e: Exception) {
                println("❌ Firestore Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    suspend fun setDailyGoal(userId: String, dailyTarget: Int) {
        val docRef = db.collection("users").document(userId)
            .collection("settings").document("steps_goal")

        val data = mapOf(
            "dailyTarget" to dailyTarget,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        withContext(Dispatchers.IO) {
            docRef.set(data, SetOptions.merge()).get()
        }
    }

    suspend fun getDailyGoal(userId: String): Int {
        val docRef = db.collection("users").document(userId)
            .collection("settings").document("steps_goal")

        return withContext(Dispatchers.IO) {
            val snap = docRef.get().get()
            if (snap.exists())
                (snap.getLong("dailyTarget") ?: DEFAULT_DAILY_TARGET.toLong()).toInt()
            else
                DEFAULT_DAILY_TARGET
        }
    }

    suspend fun getHistory(userId: String, startDate: String, endDate: String): List<Map<String, Any>> {
        requireIsoDate(startDate)
        requireIsoDate(endDate)

        val col = db.collection("users").document(userId).collection("daily_stats")

        return withContext(Dispatchers.IO) {
            val snap = col
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), startDate)
                .whereLessThanOrEqualTo(FieldPath.documentId(), endDate)
                .get().get()

            snap.documents.map { d ->
                mapOf(
                    "date" to d.id,
                    "steps" to ((d.getLong("steps") ?: 0L).toInt())
                )
            }.sortedBy { it["date"].toString() }
        }
    }

    private fun requireIsoDate(date: String) {
        require(Regex("""\d{4}-\d{2}-\d{2}""").matches(date)) { "date must be YYYY-MM-DD" }
    }
}