package app.db

import com.google.cloud.Timestamp

data class OcrRecord(
    val userId: String,
    val text: String,
    val createdAt: Timestamp = Timestamp.now()
)

object OcrRepo {
    fun save(userId: String, text: String): String {
        val doc = DB.firestore.collection("ocr_results").document()
        val data = mapOf(
            "userId" to userId,
            "text" to text,
            "createdAt" to Timestamp.now()
        )
        doc.set(data).get()
        return doc.id
    }
}