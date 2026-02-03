package app.services

import app.dto.ChatResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.api.Blob
import com.google.cloud.vertexai.api.Content
import com.google.cloud.vertexai.api.GenerationConfig
import com.google.cloud.vertexai.api.HarmCategory
import com.google.cloud.vertexai.api.Part
import com.google.cloud.vertexai.api.SafetySetting
import com.google.cloud.vertexai.generativeai.GenerativeModel
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {
    private val mapper = jacksonObjectMapper()
    private const val PROJECT_ID = "healthguard-b443f"
    private const val LOCATION = "us-central1"
    private const val MODEL_NAME = "gemini-2.0-flash"

    private const val SYSTEM_INSTRUCTION =
        "Είσαι ο HealthGuard AI, ένας έγκριτος ψηφιακός φαρμακευτικός βοηθός. " +
                "Πάντα ξεκινάς με μια σύντομη ιατρική αποποίηση ευθύνης. " +
                "Απαντάς αυστηρά με βάση τα ιατρικά δεδομένα της δραστικής ουσίας που σου δίνεται. " +
                "Χρησιμοποίησε bullet points, bold κείμενο στα σημαντικά και ελληνική γλώσσα."

    private val vertexAi: VertexAI by lazy {
        VertexAI(PROJECT_ID, LOCATION)
    }

    // Δημιουργία μοντέλου με ενσωματωμένες οδηγίες συστήματος και ρυθμίσεις ασφαλείας
    private val model: GenerativeModel by lazy {
        val config = GenerationConfig.newBuilder()
            .setTemperature(0.2f) // Χαμηλό temperature για ακρίβεια
            .build()

        // Ρυθμίσεις για να μην μπλοκάρονται ιατρικές ερωτήσεις
        val safetySettings = listOf(
            SafetySetting.newBuilder()
                .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
                .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_NONE)
                .build(),
            SafetySetting.newBuilder()
                .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH)
                .build()
        )

        GenerativeModel.Builder()
            .setModelName(MODEL_NAME)
            .setVertexAi(vertexAi)
            .setSystemInstruction(Content.newBuilder().addParts(Part.newBuilder().setText(SYSTEM_INSTRUCTION).build()).build())
            .setGenerationConfig(config)
            .setSafetySettings(safetySettings)
            .build()
    }

    suspend fun processImage(imageBytes: ByteArray): MedicineOcrResult? = withContext(Dispatchers.IO) {
        try {
            val promptText = "Ανάλυσε την εικόνα του φαρμάκου. Επίστρεψε ΜΟΝΟ ένα JSON object με τα κλειδιά: brand, activeSubstance, strength, form. Αν κάτι λείπει, βάλε κενό string."

            val content = Content.newBuilder()
                .setRole("user")
                .addParts(Part.newBuilder().setText(promptText).build())
                .addParts(Part.newBuilder().setInlineData(Blob.newBuilder()
                    .setMimeType("image/jpeg")
                    .setData(ByteString.copyFrom(imageBytes)).build()).build())
                .build()

            val response = model.generateContent(content)
            val rawText = response.candidatesList.firstOrNull()?.content?.partsList?.firstOrNull()?.text ?: ""

            val jsonRegex = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL)
            val matchResult = jsonRegex.find(rawText)
            val cleanJson = matchResult?.value ?: return@withContext null

            val result = mapper.readValue(cleanJson, MedicineOcrResult::class.java)

            return@withContext MedicineOcrResult(
                brand = result.brand?.ifBlank { "Άγνωστο" } ?: "Άγνωστο",
                activeSubstance = result.activeSubstance ?: "",
                strength = result.strength ?: "",
                form = result.form ?: ""
            )

        } catch (e: Exception) {
            println("OCR ERROR: ${e.message}")
            MedicineOcrResult("Error Reading Image", "", "", "")
        }
    }

    suspend fun chat(message: String, historyId: String?, context: Map<String, Any?>?): ChatResponse = withContext(Dispatchers.IO) {
        val brand = context?.get("brand")?.toString() ?: ""
        val substance = context?.get("activeSubstance")?.toString() ?: ""
        val strength = context?.get("strength")?.toString() ?: ""

        // Βελτιωμένο Prompt για να "αναγκάσουμε" το μοντέλο να απαντήσει συγκεκριμένα
        val userPrompt = """
            ΠΛΗΡΟΦΟΡΙΕΣ ΦΑΡΜΑΚΟΥ:
            - Όνομα: $brand
            - Δραστική: $substance
            - Ισχύς: $strength
            
            ΕΡΩΤΗΣΗ ΧΡΗΣΤΗ: "$message"
            
            ΟΔΗΓΙΑ: Απάντησε συγκεκριμένα για το παραπάνω φάρμακο. Αν η ερώτηση αφορά παρενέργειες ή δοσολογία, χρησιμοποίησε τα δεδομένα της δραστικής ουσίας $substance.
        """.trimIndent()

        try {
            val content = Content.newBuilder()
                .setRole("user")
                .addParts(Part.newBuilder().setText(userPrompt).build())
                .build()

            val response = model.generateContent(content)

            // Έλεγχος αν η απάντηση είναι κενή λόγω φίλτρων
            val replyText = response.candidatesList.firstOrNull()?.content?.partsList?.firstOrNull()?.text
                ?: "Λυπάμαι, δεν μπορώ να παρέχω αυτές τις πληροφορίες για λόγους ασφαλείας. Συμβουλευτείτε το γιατρό σας."

            ChatResponse(
                sessionId = historyId ?: java.util.UUID.randomUUID().toString(),
                reply = replyText,
                metadata = mapOf("status" to "success")
            )
        } catch (e: Exception) {
            println("CHAT ERROR: ${e.message}")
            ChatResponse(
                sessionId = historyId ?: java.util.UUID.randomUUID().toString(),
                reply = "Σφάλμα επικοινωνίας με την AI.",
                metadata = mapOf("status" to "error")
            )
        }
    }
}

data class MedicineOcrResult(
    val brand: String?,
    val activeSubstance: String?,
    val strength: String?,
    val form: String?
)