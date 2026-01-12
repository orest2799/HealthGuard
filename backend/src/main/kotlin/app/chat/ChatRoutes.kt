package app.meds

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// ---------- DTOs (request/response) ----------
private val env = dotenv()
data class ChatMessage(
    val role: String,   // "user" or "model"
    val text: String
)

data class ChatMedicineRequest(
    val ocrText: String,
    val userQuestion: String? = null,
    val chatHistory: List<ChatMessage> = emptyList()
)

// This matches the JSON you returned in your stub
@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatMedicineResponse(
    val medicineName: String? = null,
    val activeSubstance: String? = null,
    val strength: String? = null,
    val form: String? = null,
    val indication: String? = null,
    val generalAdultDosage: String? = null,
    val maxDailyDose: String? = null,
    val warnings: String? = null,
    val contraindications: String? = null,
    val commonSideEffects: String? = null,
    val otherNotes: String? = null,
    val disclaimer: String? = "This is general, non-personal information from public medicine leaflets. Always follow the instructions of your doctor or pharmacist."
)

// Minimal Gemini request/response structures
data class GeminiPart(val text: String)
data class GeminiContent(val role: String = "user", val parts: List<GeminiPart>)
data class GeminiGenerationConfig(val responseMimeType: String)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeminiCandidate(
    val content: GeminiContentResponse?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeminiContentResponse(
    val parts: List<GeminiPart> = emptyList()
)

// ---------- Shared HTTP/Jackson objects ----------

private val httpClient = OkHttpClient()
private val mapper = jacksonObjectMapper()

fun Route.chatRoutes() {
    route("/chat") {

        // ✅ Main endpoint your Android app & curl will call
        post("/medicine") {
            val body = call.receive<ChatMedicineRequest>()
            println(">>> /chat/medicine body: $body")

            if (body.ocrText.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "ocrText is required")
                )
                return@post
            }

            try {
                val geminiResult = callGeminiForMedicine(body)
                call.respond(HttpStatusCode.OK, geminiResult)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback stub on error
                val fallback = ChatMedicineResponse(
                    medicineName = "UNKNOWN_MEDICINE",
                    indication = "Error calling AI: ${e.message}",
                    otherNotes = "Fallback response. Check server logs and GEMINI_API_KEY.",
                )
                call.respond(HttpStatusCode.InternalServerError, fallback)
            }
        }

        // Optional: quick test endpoint (no Gemini)
        post("/medicine/test") {
            val raw = call.receiveText()
            println(">>> /chat/medicine/test RAW BODY: $raw")

            val responseJson = """
            {
              "medicineName": "TEST_MEDICINE",
              "activeSubstance": "Test Substance",
              "strength": "",
              "form": "",
              "indication": "Stub response - /chat/medicine/test endpoint is working.",
              "generalAdultDosage": "",
              "maxDailyDose": "",
              "warnings": "",
              "contraindications": "",
              "commonSideEffects": "",
              "otherNotes": "If you see this JSON, your backend route is OK.",
              "disclaimer": "This is general, non-personal information from public medicine leaflets. Always follow the instructions of your doctor or pharmacist."
            }
            """.trimIndent()

            call.respondText(
                responseJson,
                ContentType.Application.Json,
                HttpStatusCode.OK
            )
        }
    }
}

// ---------- Gemini call ----------

private fun callGeminiForMedicine(req: ChatMedicineRequest): ChatMedicineResponse {

    // ----------- LOAD API KEY SAFELY -----------
    val apiKey = env["GEMINI_API_KEY"]
    if (apiKey.isNullOrBlank()) {
        println("❌ GEMINI_API_KEY missing in .env")
        return ChatMedicineResponse(
            medicineName = null,
            indication = "Server misconfiguration: GEMINI_API_KEY missing.",
            otherNotes = "Place your .env file in /backend/.env"
        )
    }

    // ----------- SYSTEM INSTRUCTIONS -----------
    val systemInstruction = """
You are a cautious assistant that summarizes medicine information from OCR text.
You are NOT a doctor and you do NOT give personal medical advice.

Tasks:
1. Identify the medicine name, active substance, strength and form.
2. Give a short, neutral indication (what it is generally used for).
3. ONLY provide dosage if it exists in the OCR text.
4. Mention warnings/contraindications/side effects from OCR text.
5. Always include a strong medical disclaimer.

Rules:
- Do NOT invent doses.
- Do NOT recommend treatment.
- Do NOT ask for personal medical data.
""".trimIndent()

    // ----------- USER PROMPT -----------
    val userPrompt = """
OCR TEXT FROM MEDICINE PACK / LEAFLET:

${req.ocrText}

USER QUESTION:
"${req.userQuestion ?: "Explain briefly what this medicine is and what to watch out for."}"

Return ONLY a JSON matching EXACTLY this structure:

{
  "medicineName": string | null,
  "activeSubstance": string | null,
  "strength": string | null,
  "form": string | null,
  "indication": string,
  "generalAdultDosage": string,
  "maxDailyDose": string,
  "warnings": string,
  "contraindications": string,
  "commonSideEffects": string,
  "otherNotes": string,
  "disclaimer": string
}

No markdown, no commentary, ONLY JSON.
""".trimIndent()

    // ----------- BUILD CONTENTS -----------
    val contents = mutableListOf<GeminiContent>()

    contents += GeminiContent(
        role = "user",
        parts = listOf(GeminiPart(systemInstruction))
    )

    req.chatHistory.forEach { msg ->
        contents += GeminiContent(
            role = if (msg.role == "model") "model" else "user",
            parts = listOf(GeminiPart(msg.text))
        )
    }

    contents += GeminiContent(
        role = "user",
        parts = listOf(GeminiPart(userPrompt))
    )

    val geminiRequest = GeminiRequest(
        contents = contents,
        generationConfig = GeminiGenerationConfig(
            responseMimeType = "application/json"
        )
    )

    val jsonPayload = mapper.writeValueAsString(geminiRequest)
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = jsonPayload.toRequestBody(mediaType)

    val request = Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
        .post(requestBody)
        .build()

    // ----------- SAFE HTTP CALL -----------
    httpClient.newCall(request).execute().use { resp ->

        val bodyString = resp.body?.string() ?: ""

        // If HTTP error occurs, return JSON instead of crashing
        if (!resp.isSuccessful) {
            println("❌ Gemini HTTP error ${resp.code}: $bodyString")
            return ChatMedicineResponse(
                medicineName = null,
                indication = "Gemini HTTP error ${resp.code}",
                otherNotes = bodyString
            )
        }

        return try {
            // Parse Gemini wrapper
            val geminiResp: GeminiResponse = mapper.readValue(bodyString)

            val textFromModel = geminiResp
                .candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            if (textFromModel.isNullOrBlank()) {
                println("⚠ Empty Gemini response: $bodyString")
                return ChatMedicineResponse(
                    medicineName = null,
                    indication = "Gemini returned no text.",
                    otherNotes = bodyString
                )
            }

            // Parse the JSON generated by Gemini
            mapper.readValue<ChatMedicineResponse>(textFromModel)

        } catch (e: Exception) {

            println("⚠ JSON parse error: ${e.message}")
            println("Raw Gemini response: $bodyString")

            return ChatMedicineResponse(
                medicineName = null,
                indication = "Error parsing Gemini JSON.",
                otherNotes = e.message
            )
        }
    }
}
