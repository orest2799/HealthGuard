package com.example.healthguard.data.repo

import com.example.healthguard.data.network.dto.MedRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MedicineRepository {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseDatabase.getInstance().getReference("medicine_cabinet")


    fun saveToCabinet(record: MedRecord, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onResult(false)
            return
        }
        val entryId = db.child(userId).push().key ?: run {
            onResult(false)
            return
        }

        db.child(userId).child(entryId).setValue(record)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun searchMedicines(query: String, country: String?, type: String): List<MedRecord> {

        return listOf(
            MedRecord(
                brand = query,
                summary = "Information found via OCR scan.",
                source = "Live Detection"
            )
        )
    }
}