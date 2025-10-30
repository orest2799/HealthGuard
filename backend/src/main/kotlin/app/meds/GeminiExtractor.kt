package app.meds

import app.api.GalinosFacts
import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import org.slf4j.LoggerFactory

class GeminiService(
    private val apiKey: String,
    private val model: String = "gemini-1.5-flash-latest" // ← no "models/" prefix here
) {
    private val log = LoggerFactory.getLogger("GeminiService")

    private val client = HttpClient(Java) {
        install(ContentNegotiation) { jackson() }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class ContentPart(val text: String)
    data class Content(val role: String? = null, val parts: List<ContentPart>)
    data class GenerateContentRequest(val contents: List<Content>)
    data class Candidate(val content: Content?)
    data class GenerateContentResponse(val candidates: List<Candidate> = emptyList())

    companion object {
        fun fromEnv(): GeminiService {
            val key = System.getenv("GEMINI_API_KEY")
                ?: error("GEMINI_API_KEY env var is not set")
            return GeminiService(apiKey = key, model = "gemini-1.5-flash-latest")
        }
    }

    suspend fun answerFromGalinos(question: String, facts: GalinosFacts): String {
        val systemPreamble = """
            You are a Greek medical assistant.
            Answer STRICTLY based on the structured data provided from Galinos (official Greek drug compendium).
            If a requested detail is not present, say you don't have sufficient information.
            Be concise, write in Greek, and append: "Δεν υποκαθιστά ιατρική συμβουλή."
        """.trimIndent()

        val factsText = buildString {
            appendLine("### Galinos Facts")
            facts.name?.let { appendLine("Ονομασία: $it") }
            facts.form?.let { appendLine("Μορφή: $it") }
            facts.atc?.let { appendLine("ATC: $it") }
            if (facts.substances.isNotEmpty()) appendLine("Δραστικές ουσίες: ${facts.substances.joinToString()}")
            facts.indications?.let { appendLine("\nΕνδείξεις:\n$it") }
            facts.contraindications?.let { appendLine("\nΑντενδείξεις:\n$it") }
            facts.warnings?.let { appendLine("\nΠροειδοποιήσεις/Προφυλάξεις:\n$it") }
            facts.dosage?.let { appendLine("\nΔοσολογία:\n$it") }
            facts.interactions?.let { appendLine("\nΑλληλεπιδράσεις:\n$it") }
            facts.url?.let { appendLine("\nΠηγή: $it") }
        }

        val req = GenerateContentRequest(
            contents = listOf(
                Content(role = "user", parts = listOf(ContentPart(systemPreamble))),
                Content(role = "user", parts = listOf(ContentPart(factsText))),
                Content(role = "user", parts = listOf(ContentPart(question)))
            )
        )

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val httpResp: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(req)
        }

        if (!httpResp.status.isSuccess()) {
            val raw = httpResp.bodyAsText()
            log.error("Gemini error {}: {}", httpResp.status.value, raw)
            return "" // let caller fall back to "Δεν βρήκα επαρκείς πληροφορίες."
        }

        val parsed: GenerateContentResponse = httpResp.body()
        val text = parsed.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
        return text?.trim().orEmpty()
    }
}
