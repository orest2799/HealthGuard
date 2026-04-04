package com.example.healthguard.data.network.appointments

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AppointmentFirebaseDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User is not signed in")
    }

    private fun ref() = db.reference
        .child("users")
        .child(requireUid())
        .child("appointments")

    suspend fun saveAppointment(appointment: Appointment) {
        ref().child(appointment.id).setValue(appointment).await()
    }

    suspend fun deleteAppointment(id: String) {
        ref().child(id).removeValue().await()
    }

    suspend fun getAppointment(id: String): Appointment? {
        val snapshot = ref().child(id).get().await()
        return snapshot.getValue(Appointment::class.java)
    }

    suspend fun getAllAppointments(): List<Appointment> {
        val snapshot = ref().get().await()
        return snapshot.children.mapNotNull { it.getValue(Appointment::class.java) }
    }
}