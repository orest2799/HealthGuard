package app.services

import app.dto.ChatResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.api.Blob
import com.google.cloud.vertexai.api.Content
import com.google.cloud.vertexai.api.GenerationConfig
import com.google.cloud.vertexai.api.Part
import com.google.cloud.vertexai.generativeai.GenerativeModel
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object GeminiService {
    private val mapper = jacksonObjectMapper()
    private const val PROJECT_ID = "healthguard-b443f"
    private const val LOCATION = "europe-west1"
    private const val MODEL_NAME = "gemini-2.0-flash-001"

    // Bilingual system instruction
    private val SYSTEM_INSTRUCTION = """
        You are HealthGuard AI, an experienced pharmacist assistant.
        RULES:
        1. Always start with a short medical disclaimer.
        2. Answer with bullet points and bold text.
        3. For side effects or dosage questions, give a complete list.
        4. IMPORTANT: The user will specify the language in the prompt (Greek or English). 
           You MUST respond in whichever language they request.
        5. Never diagnose. If there are signs of emergency, tell the user to call emergency services immediately.
    """.trimIndent()

    private val vertexAi: VertexAI by lazy { VertexAI(PROJECT_ID, LOCATION) }

    private val model: GenerativeModel by lazy {
        val generationConfig = GenerationConfig.newBuilder()
            .setTemperature(0.7f)
            .setTopP(0.95f)
            .setMaxOutputTokens(900)
            .build()

        GenerativeModel.Builder()
            .setModelName(MODEL_NAME)
            .setVertexAi(vertexAi)
            .setSystemInstruction(
                Content.newBuilder().addParts(Part.newBuilder().setText(SYSTEM_INSTRUCTION)).build()
            )
            .setGenerationConfig(generationConfig)
            .build()
    }

    suspend fun chat(
        message: String,
        historyId: String?,
        context: Map<String, String>?,
        language: String = "el"  // "el" = Greek, "en" = English
    ): ChatResponse = withContext(Dispatchers.IO) {

        val sessionId = historyId ?: UUID.randomUUID().toString()

        val brand = context?.get("brand") ?: "the medicine"
        val substance = context?.get("activeSubstance") ?: ""
        val strength = context?.get("strength") ?: ""
        val form = context?.get("form") ?: ""

        val history = ChatMemory.get(sessionId)
        val historyText = if (history.isEmpty()) {
            "(no previous messages)"
        } else {
            history.joinToString("\n") { turn ->
                val who = if (turn.role == "user") "USER" else "ASSISTANT"
                "$who: ${turn.text}"
            }
        }

        // Tell Gemini which language to respond in
        val languageInstruction = if (language == "el")
            "IMPORTANT: Respond in Greek (Ελληνικά)."
        else
            "IMPORTANT: Respond in English."

        val userPrompt = """
            $languageInstruction
            
            YOU ARE A PHARMACIST. DO NOT DIAGNOSE.
            If there are signs of emergency, tell the user to call local emergency services immediately.

            TARGET_MEDICINE: $brand ($substance)
            PACKAGING: strength=$strength, form=$form

            CHAT_HISTORY:
            $historyText

            USER_QUESTION:
            $message
        """.trimIndent()

        ChatMemory.appendUser(sessionId, message)

        try {
            val response = model.generateContent(userPrompt)
            val replyText = response.candidatesList.firstOrNull()
                ?.content?.partsList?.firstOrNull()?.text
                ?: if (language == "el") "Λυπάμαι, δεν μπόρεσα να επεξεργαστώ το αίτημα."
                else "Sorry, I could not process the request."

            ChatMemory.appendAssistant(sessionId, replyText)

            val galinosLink = "https://www.galinos.gr/el/medicines/search?q=${brand.replace(" ", "+")}"

            ChatResponse(
                sessionId = sessionId,
                reply = replyText,
                metadata = mapOf(
                    "status" to "success",
                    "sources" to listOf(mapOf("title" to "Galinos.gr", "url" to galinosLink)),
                    "quickActions" to quickActions(language),
                    "context" to mapOf(
                        "brand" to brand,
                        "activeSubstance" to substance,
                        "strength" to strength,
                        "form" to form
                    )
                )
            )
        } catch (e: Exception) {
            println("CHAT ERROR: ${e.message}")
            e.printStackTrace()
            ChatResponse(
                sessionId = sessionId,
                reply = if (language == "el") "Σφάλμα AI: ${e.message}" else "AI Error: ${e.message}",
                metadata = mapOf("status" to "error")
            )
        }
    }


    fun quickActions(language: String = "el"): List<Map<String, String>> {
        return if (language == "el") {
            listOf(
                mapOf("id" to "side_effects",     "title" to "Παρενέργειες",          "message" to "Ποιες είναι οι παρενέργειες για αυτό το φάρμακο; Δώσε πλήρη λίστα και πότε να ζητήσω βοήθεια."),
                mapOf("id" to "dosage",            "title" to "Δοσολογία",              "message" to "Ποια είναι η δοσολογία; Δώσε τυπικές οδηγίες και σημαντικές προειδοποιήσεις."),
                mapOf("id" to "contraindications", "title" to "Αντενδείξεις",           "message" to "Ποιες είναι οι αντενδείξεις και ποιοι πρέπει να το αποφεύγουν;"),
                mapOf("id" to "missed_dose",       "title" to "Ξέχασα τα χάπια μου",   "message" to "Ξέχασα μια δόση. Τι να κάνω; Δώσε πρακτικές οδηγίες και τι να αποφύγω.")
            )
        } else {
            listOf(
                mapOf("id" to "side_effects",     "title" to "Side Effects",      "message" to "What are the side effects of this medicine? Give a full list and when to seek help."),
                mapOf("id" to "dosage",            "title" to "Dosage",             "message" to "What is the dosage? Give typical instructions and important warnings."),
                mapOf("id" to "contraindications", "title" to "Contraindications",  "message" to "What are the contraindications and who should avoid it?"),
                mapOf("id" to "missed_dose",       "title" to "Missed my pills",    "message" to "I missed a dose. What should I do? Give practical instructions and what to avoid.")
            )
        }
    }

    private fun detectMimeType(bytes: ByteArray): String {
        if (bytes.size < 12) return "application/octet-stream"
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) return "image/jpeg"
        val pngSig = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        if (bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(pngSig)) return "image/png"
        if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) return "image/webp"
        return "application/octet-stream"
    }

    suspend fun processImage(imageBytes: ByteArray): MedicineOcrResult? =
        withContext(Dispatchers.IO) {
            try {
                if (imageBytes.isEmpty()) throw IllegalArgumentException("Empty image bytes")
                val mimeType = detectMimeType(imageBytes)
                if (!mimeType.startsWith("image/")) throw IllegalArgumentException("Unsupported image format")

                val promptText = "Analyse the medicine image. Return ONLY a JSON object with keys: brand, activeSubstance, strength, form."
                val content = Content.newBuilder()
                    .setRole("user")
                    .addParts(Part.newBuilder().setText(promptText).build())
                    .addParts(
                        Part.newBuilder().setInlineData(
                            Blob.newBuilder().setMimeType(mimeType)
                                .setData(ByteString.copyFrom(imageBytes)).build()
                        ).build()
                    ).build()

                val response = model.generateContent(content)
                val rawText = response.candidatesList.firstOrNull()?.content?.partsList?.firstOrNull()?.text ?: ""
                val match = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL).find(rawText)
                val cleanJson = match?.value ?: return@withContext null
                mapper.readValue(cleanJson, MedicineOcrResult::class.java)
            } catch (e: Exception) {
                println("OCR ERROR: ${e.message}")
                e.printStackTrace()
                null
            }
        }
}