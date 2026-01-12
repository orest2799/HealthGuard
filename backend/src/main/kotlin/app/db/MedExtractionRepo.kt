package app.db

import app.meds.MedRecord
import com.google.cloud.Timestamp

data class MedExtractionRecord(
    val userId: String,
    val ocrId: String,
    val meds: List<MedRecord>,
    val createdAt: Timestamp = Timestamp.now()
)

object MedExtractionRepo {
    fun save(userId: String, ocrId: String, meds: List<MedRecord>): String {
        val doc = DB.firestore.collection("med_extractions").document()
        val data = mapOf(
            "userId" to userId,
            "ocrId" to ocrId,
            "meds" to meds,
            "createdAt" to Timestamp.now()
        )
        doc.set(data).get()
        return doc.id
    }
}