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
    private const val MODEL_NAME =
        "gemini-2.0-flash-001" // Αλλαγή σε 1.5 για μεγαλύτερη σταθερότητα

    private val SYSTEM_INSTRUCTION = """
        Είσαι ο HealthGuard AI, ένας έμπειρος φαρμακοποιός. 
        ΚΑΝΟΝΕΣ:
        1. Ξεκινάς ΠΑΝΤΑ με μια σύντομη ιατρική αποποίηση ευθύνης.
        2. Απαντάς ΑΝΑΛΥΤΙΚΑ με bullet points και έντονα γράμματα (bold).
        3. Αν ο χρήστης ρωτάει για παρενέργειες ή δοσολογία, δίνεις πλήρη λίστα.
        4. Απαντάς στα Ελληνικά.
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
            .build() // Χωρίς .setTools
    }

    suspend fun chat(
        message: String,
        historyId: String?,
        context: Map<String, String>?
    ): ChatResponse = withContext(Dispatchers.IO) {

        val sessionId = historyId ?: UUID.randomUUID().toString()

        val brand = context?.get("brand") ?: "το φάρμακο"
        val substance = context?.get("activeSubstance") ?: ""
        val strength = context?.get("strength") ?: ""
        val form = context?.get("form") ?: ""

        // Pull recent history
        val history = ChatMemory.get(sessionId)

        val historyText = if (history.isEmpty()) {
            "(no previous messages)"
        } else {
            history.joinToString("\n") { turn ->
                val who = if (turn.role == "user") "USER" else "ASSISTANT"
                "$who: ${turn.text}"
            }
        }

        val userPrompt = """
        ΕΙΣΑΙ ΦΑΡΜΑΚΟΠΟΙΟΣ. ΜΗΝ ΚΑΝΕΙΣ ΔΙΑΓΝΩΣΗ. 
        Αν υπάρχουν σημάδια επείγοντος, πες να καλέσει άμεσα τις τοπικές υπηρεσίες έκτακτης ανάγκης.

        TARGET_MEDICINE: $brand ($substance)
        PACKAGING: strength=$strength, form=$form

        CHAT_HISTORY:
        $historyText

        USER_QUESTION:
        $message
    """.trimIndent()

        // Store user turn
        ChatMemory.appendUser(sessionId, message)

        try {
            val response = model.generateContent(userPrompt)
            val replyText =
                response.candidatesList.firstOrNull()?.content?.partsList?.firstOrNull()?.text
                    ?: "Λυπάμαι, δεν μπόρεσα να επεξεργαστώ το αίτημα."

            // Store assistant turn
            ChatMemory.appendAssistant(sessionId, replyText)

            val galinosLink =
                "https://www.galinos.gr/el/medicines/search?q=${brand.replace(" ", "+")}"

            ChatResponse(
                sessionId = sessionId,
                reply = replyText,
                metadata = mapOf(
                    "status" to "success",
                    "sources" to listOf(mapOf("title" to "Galinos.gr", "url" to galinosLink)),
                    "quickActions" to quickActions(),
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
                reply = "Σφάλμα AI: ${e.message}",
                metadata = mapOf("status" to "error")
            )
        }
    }


    private fun detectMimeType(bytes: ByteArray): String {
        if (bytes.size < 12) return "application/octet-stream"

        // JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return "image/jpeg"
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        val pngSig = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        if (bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(pngSig)) {
            return "image/png"
        }

        // WEBP: "RIFF"...."WEBP"
        if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return "image/webp"
        }

        return "application/octet-stream"
    }
    fun quickActions(): List<Map<String, String>> = listOf(
        mapOf(
            "id" to "side_effects",
            "title" to "Παρενέργειες",
            "message" to "Ποιες είναι οι παρενέργειες για αυτό το φάρμακο; Δώσε πλήρη λίστα και πότε να ζητήσω βοήθεια."
        ),
        mapOf(
            "id" to "dosage",
            "title" to "Δοσολογία",
            "message" to "Ποια είναι η δοσολογία; Δώσε τυπικές οδηγίες και σημαντικές προειδοποιήσεις."
        ),
        mapOf(
            "id" to "contraindications",
            "title" to "Αντενδείξεις",
            "message" to "Ποιες είναι οι αντενδείξεις και ποιοι πρέπει να το αποφεύγουν;"
        ),
        mapOf(
            "id" to "missed_dose",
            "title" to "Ξέχασα τα χάπια μου",
            "message" to "Ξέχασα μια δόση. Τι να κάνω; Δώσε πρακτικές οδηγίες και τι να αποφύγω."
        )
    )

    suspend fun processImage(imageBytes: ByteArray): MedicineOcrResult? =
        withContext(Dispatchers.IO) {
            try {
                if (imageBytes.isEmpty()) throw IllegalArgumentException("Empty image bytes")

                val mimeType = detectMimeType(imageBytes)
                if (!mimeType.startsWith("image/")) {
                    throw IllegalArgumentException("Unsupported/invalid image format (mime=$mimeType)")
                }

                val promptText =
                    "Ανάλυσε την εικόνα του φαρμάκου. Επίστρεψε ΜΟΝΟ ένα JSON object με τα κλειδιά: brand, activeSubstance, strength, form."

                val content = Content.newBuilder()
                    .setRole("user")
                    .addParts(Part.newBuilder().setText(promptText).build())
                    .addParts(
                        Part.newBuilder().setInlineData(
                            Blob.newBuilder()
                                .setMimeType(mimeType)
                                .setData(ByteString.copyFrom(imageBytes))
                                .build()
                        ).build()
                    )
                    .build()

                val response = model.generateContent(content)
                val rawText =
                    response.candidatesList.firstOrNull()?.content?.partsList?.firstOrNull()?.text ?: ""

                val match = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL).find(rawText)
                val cleanJson = match?.value ?: return@withContext null

                return@withContext mapper.readValue(cleanJson, MedicineOcrResult::class.java)

            } catch (e: Exception) {
                println("OCR ERROR: ${e.message}")
                e.printStackTrace()
                null
            }
        }

}
