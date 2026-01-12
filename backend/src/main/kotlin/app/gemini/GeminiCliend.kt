package app.gemini

import app.meds.MedicineInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson

class GeminiClient(
    private val apiKey: String,
    private val model: String = "gemini-2.5-flash"
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    private val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    suspend fun extractMedicineFromOcr(ocrText: String): MedicineInfo? {
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val prompt = """
            You are a medicine label parser for the Greek market.
            Input is noisy OCR text (Greek + English).
            
            Extract a single JSON object with this exact schema:
            {
              "brand_name": "string | null",
              "substances": [
                { "name": "string", "strength": "string | null" }
              ],
              "form": "string | null",
              "country": "GR",
              "language": "mixed"
            }
            
            Rules:
            - Output ONLY the JSON object. No explanations, no comments, no markdown.
            - If you are not sure about a field, set it to null or [].
            
            OCR_TEXT:
            $ocrText
        """.trimIndent()

        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )
        )

        val response: GeminiGenerateContentResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()

        val rawText = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: return null

        // Sometimes model might wrap JSON in ``` or text around it – try to extract the first {...}
        val json = extractFirstJsonObject(rawText) ?: rawText

        return try {
            mapper.readValue<MedicineInfo>(json)
        } catch (e: Exception) {
            println("Failed to parse MedicineInfo JSON from Gemini: ${e.message}")
            null
        }
    }

    private fun extractFirstJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start == -1) return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return text.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }
}

// --- minimal DTOs for Gemini response ---

data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

data class GeminiContent(
    val parts: List<GeminiPart>?
)

data class GeminiPart(
    val text: String?
)
