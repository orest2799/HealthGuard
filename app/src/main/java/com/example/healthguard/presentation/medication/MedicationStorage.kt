package com.example.healthguard.presentation.medication

import android.content.Context

import com.example.healthguard.domain.model.Medication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.datastore.preferences.preferencesDataStore



val Context.dataStore by preferencesDataStore("medications")

object MedicationFirebaseStorage {

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseDatabase.getInstance().getReference("medications")

    fun saveMedication(med: Medication, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val medId = med.id.ifEmpty { db.child(userId).push().key ?: "" }
        val updatedMed = med.copy(id = medId)
        db.child(userId).child(medId).setValue(updatedMed)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun deleteMedication(medId: String, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.child(userId).child(medId).removeValue()
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun fetchMedications(onData: (List<Medication>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Medication::class.java) }
                onData(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onData(emptyList())
            }
        })
    }
}