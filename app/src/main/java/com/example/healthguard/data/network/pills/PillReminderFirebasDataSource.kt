package com.example.healthguard.data.network.pills

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class PillReminderFirebaseDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User is not signed in")
    }

    private fun remindersRef() =
        database.reference
            .child("users")
            .child(requireUid())
            .child("pillReminders")

    suspend fun saveReminder(reminder: PillReminderDto) {
        remindersRef()
            .child(reminder.id)
            .setValue(reminder)
            .await()
    }

    suspend fun deleteReminder(reminderId: String) {
        remindersRef()
            .child(reminderId)
            .removeValue()
            .await()
    }
    suspend fun getReminder(reminderId: String): PillReminderDto? {
        val snapshot = remindersRef().child(reminderId).get().await()
        return snapshot.getValue(PillReminderDto::class.java)
    }

    suspend fun getAllReminders(): List<PillReminderDto> {
        val snapshot = remindersRef().get().await()
        return snapshot.children.mapNotNull { it.getValue(PillReminderDto::class.java) }
    }

    suspend fun updateReminder(reminder: PillReminderDto) {
        remindersRef().child(reminder.id).setValue(reminder).await()
    }



}