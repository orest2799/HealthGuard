package app.services

import app.api.GalinosFacts
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
import java.io.File

class GeminiService(
    private val apiKey: String,
    private val model: String = "gemini-2.5-flash"
) {
    private val client = HttpClient(Java) {
        install(ContentNegotiation) { jackson() }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class ContentPart(val text: String)

    data class Content(val role: String? = null, val parts: List<ContentPart>)

    data class GenerateContentRequest(val contents: List<Content>)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Candidate(
        val content: Content? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GenerateContentResponse(
        val candidates: List<Candidate> = emptyList()
    )

    companion object {
        private val log = LoggerFactory.getLogger("GeminiService")

        fun fromEnv(): GeminiService {
            // Try multiple locations for .env file
            val possiblePaths = listOf(
                "./backend/.env",           // From project root
                "./.env",                    // Project root
                ".env",                      // Current directory
                "../.env"                    // Parent directory
            )

            var key: String? = null

            // Try to load from .env file
            for (path in possiblePaths) {
                val envFile = File(path)
                log.info("Checking for .env at: ${envFile.absolutePath}")

                if (envFile.exists()) {
                    log.info("Found .env file at: ${envFile.absolutePath}")
                    try {
                        envFile.readLines().forEach { line ->
                            val trimmed = line.trim()
                            if (trimmed.startsWith("GEMINI_API_KEY=")) {
                                key = trimmed.substringAfter("GEMINI_API_KEY=").trim()
                                log.info("Loaded GEMINI_API_KEY from $path")
                            }
                        }
                        if (key != null) break
                    } catch (e: Exception) {
                        log.error("Error reading .env file at $path: ${e.message}")
                    }
                }
            }

            // Fallback to system environment variable
            if (key == null) {
                key = System.getenv("GEMINI_API_KEY")
                if (key != null) {
                    log.info("Loaded GEMINI_API_KEY from environment variable")
                }
            }

            if (key == null) {
                error("""
                    GEMINI_API_KEY not found!
                    
                    Checked locations:
                    ${possiblePaths.joinToString("\n") { "  - ${File(it).absolutePath}" }}
                    
                    Also checked system environment variables.
                    
                    Please create a .env file in one of these locations with:
                    GEMINI_API_KEY=your-api-key-here
                """.trimIndent())
            }

            return GeminiService(apiKey = key, model = "gemini-2.5-flash")
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
            if (facts.substances.isNotEmpty()) {
                appendLine("Δραστικές ουσίες: ${facts.substances.joinToString()}")
            }

            facts.sections.forEach { (title, content) ->
                appendLine("\n$title:")
                appendLine(content)
            }

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

        val httpResp: HttpResponse = try {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        } catch (e: Exception) {
            log.error("Gemini request failed: ${e.message}", e)
            return ""
        }

        if (!httpResp.status.isSuccess()) {
            val raw = httpResp.bodyAsText()
            log.error("Gemini error {}: {}", httpResp.status.value, raw)
            return ""
        }

        val parsed: GenerateContentResponse = httpResp.body()
        val text = parsed.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text

        return text?.trim().orEmpty()
    }
}