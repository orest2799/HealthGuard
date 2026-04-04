package com.example.healthguard.data.repo

import com.example.healthguard.data.models.MedicineOcrResult
import com.example.healthguard.data.network.dto.ApiClient
import com.example.healthguard.data.network.dto.ChatRequest
import com.example.healthguard.data.network.dto.ChatResponse
import com.example.healthguard.data.network.dto.ChatSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChatRepository {

    suspend fun processMedicineImage(imageFile: File): ChatSessionResult =
        withContext(Dispatchers.IO) {
            try {
                val body = imageFile.asRequestBody("application/octet-stream".toMediaType())
                val response = ApiClient.chat.chatFromImage(body)

                if (!response.isSuccessful) {
                    return@withContext ChatSessionResult.Error("Σφάλμα διακομιστή: ${response.code()}")
                }

                val chat = response.body()
                    ?: return@withContext ChatSessionResult.Error("Κενή απάντηση από τον διακομιστή.")

                val ocr = extractOcr(chat.metadata)
                    ?: return@withContext ChatSessionResult.Error("Η αναγνώριση απέτυχε. Δοκίμασε ξανά.")
                val quickActions = extractQuickActions(chat.metadata)

                ChatSessionResult.Success(
                    sessionId = chat.sessionId,
                    reply = chat.reply,
                    extractedText = ocr.brand ?: "",
                    medicines = emptyList(),
                    sources = extractSources(chat.metadata),
                    ocrResult = ocr,
                    quickActions = quickActions
                )

            } catch (e: Exception) {
                ChatSessionResult.Error("Σφάλμα σύνδεσης: ${e.localizedMessage ?: "Άγνωστο σφάλμα"}")
            }
        }

    suspend fun sendMessage(
        sessionId: String?,
        message: String,
        medicineContext: String? = null,
        activeSubstance: String? = null,
        strength: String? = null
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.chat.chat(
                ChatRequest(
                    sessionId = sessionId, // null => backend creates new session
                    text = message,
                    context = mapOf(
                        "brand" to (medicineContext ?: ""),
                        "activeSubstance" to (activeSubstance ?: ""),
                        "strength" to (strength ?: "")
                    )
                )
            )

            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractSources(metadata: Map<String, Any?>?): List<ChatSource> {
        val rawSources = metadata?.get("sources") as? List<*> ?: return emptyList()
        return rawSources.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val title = m["title"]?.toString()
            val url = m["url"]?.toString()
            if (!title.isNullOrBlank() && !url.isNullOrBlank()) ChatSource(title, url) else null
        }
    }

    private fun extractOcr(metadata: Map<String, Any?>?): MedicineOcrResult? {
        val ocrMap = metadata?.get("ocr") as? Map<*, *> ?: return null
        return MedicineOcrResult(
            brand = ocrMap["brand"]?.toString(),
            activeSubstance = ocrMap["activeSubstance"]?.toString(),
            strength = ocrMap["strength"]?.toString(),
            form = ocrMap["form"]?.toString()
        )
    }
    private fun extractQuickActions(metadata: Map<String, Any?>?): List<Map<String, String>> {
        val list = metadata?.get("quickActions") as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val id = m["id"]?.toString() ?: return@mapNotNull null
            val title = m["title"]?.toString() ?: return@mapNotNull null
            val message = m["message"]?.toString() ?: return@mapNotNull null
            mapOf("id" to id, "title" to title, "message" to message)
        }
    }

}

sealed class ChatSessionResult {
    data class Success(
        val sessionId: String,
        val reply: String,
        val extractedText: String,
        val medicines: List<Any>,
        val sources: List<ChatSource>,
        val ocrResult: MedicineOcrResult,
        val quickActions: List<Map<String, String>> = emptyList()
    ) : ChatSessionResult()

    data class Error(val message: String) : ChatSessionResult()
}
