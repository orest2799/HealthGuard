package app

import app.scans.scanRoutes
import app.vision.visionRoutes
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

// ===========================================================
// JSON MAPPER (manual, used in some endpoints)
// ===========================================================
private val jsonMapper = jacksonObjectMapper()

// ===========================================================
// DATA CLASSES
// ===========================================================
data class ParsedOcr(
    val rawText: String,
    val normalizedText: String,
    val brandGuess: String?,
    val strengthGuess: String?,
    val formGuess: String?,
    val gemini: GeminiExtractedMedicine?
)

data class GeminiExtractedMedicine(
    val brand: String?,
    val activeSubstance: String?,
    val strength: String?,
    val form: String?
)

data class RemoteMedicine(
    val brand: String?,
    val activeSubstance: String?,
    val strength: String?,
    val form: String?,
    val source: String
)

data class MedSearchResponse(
    val brandQuery: String?,
    val substanceQuery: String?,
    val results: List<RemoteMedicine>
)

data class ResolveOcrResponse(
    val rawText: String,
    val normalizedText: String,
    val localBrandGuess: String?,
    val localStrengthGuess: String?,
    val localFormGuess: String?,
    val gemini: GeminiExtractedMedicine?,
    val searchBrandQuery: String?,
    val searchSubstanceQuery: String?,
    val results: List<RemoteMedicine>
)

// ===========================================================
// TEXT NORMALIZATION + HEURISTIC PARSER
// ===========================================================
fun normalizeOcrText(raw: String): String {
    val upper = raw.uppercase(Locale("el", "GR"))
    val decomposed = Normalizer.normalize(upper, Normalizer.Form.NFD)
    val noAccents = decomposed.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    val cleaned = noAccents.replace("[^A-ZΑ-Ω0-9\\s/%]".toRegex(), " ")
    return cleaned.replace("\\s+".toRegex(), " ").trim()
}

fun parseLocalMedicine(text: String): Triple<String?, String?, String?> {
    val tokens = text.split(" ")
    val brand = tokens.firstOrNull { it.any(Char::isLetter) }
    val strength = Regex("(\\d+\\s*(/\\s*\\d+)?\\s*(MG|G|ML|MCG|IU))").find(text)?.value
    val forms = listOf("ΔΙΣΚΙΑ", "ΚΑΨΟΥΛΕΣ", "ΣΙΡΟΠΙ", "TABLETS", "CAPSULES")
    val form = forms.firstOrNull { text.contains(it) }
    return Triple(brand, strength, form)
}

// ===========================================================
// GEMINI API KEY (Cloud Run friendly)
// ===========================================================
private val geminiKey: String? = System.getenv("GEMINI_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }

// Safe log (does not print the key)
private fun logGeminiKeyPresence() {
    println("GEMINI_API_KEY present? ${!geminiKey.isNullOrBlank()}")
}

// ===========================================================
// GEMINI API CALL
// ===========================================================
private val http = OkHttpClient()

suspend fun callGeminiExtraction(ocrText: String): GeminiExtractedMedicine? {
    val key = geminiKey ?: return null

    val prompt = """
        You read OCR text from a medicine box.

        OCR text:
        $ocrText

        Return a single JSON object with exactly these keys:
        {
          "brand": string or null,
          "activeSubstance": string or null,
          "strength": string or null,
          "form": string or null
        }

        Return ONLY the raw JSON object.
    """.trimIndent()

    val payload = """
        {
          "contents": [
            { "parts": [ { "text": ${JSONObject.quote(prompt)} } ] }
          ]
        }
    """.trimIndent()

    val req = Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$key")
        .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
        .build()

    return withContext(Dispatchers.IO) {
        try {
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return@use null
                val root = JSONObject(body)

                // If Gemini returns an error payload, surface it
                if (root.has("error")) {
                    val err = root.getJSONObject("error")
                    println("Gemini error: ${err.optString("message")}")
                    return@use null
                }

                val cand = root.getJSONArray("candidates").getJSONObject(0)
                val content = cand.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val text = parts.getJSONObject(0).getString("text").trim()

                val clean = text.replace("```json", "").replace("```", "").trim()
                val j = JSONObject(clean)

                GeminiExtractedMedicine(
                    brand = j.optString("brand", null),
                    activeSubstance = j.optString("activeSubstance", null),
                    strength = j.optString("strength", null),
                    form = j.optString("form", null)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// ===========================================================
// openFDA SEARCH (global)
// ===========================================================
private val openFdaClient = OkHttpClient()

suspend fun searchOpenFda(brand: String?, substance: String?): List<RemoteMedicine> {
    if (brand.isNullOrBlank() && substance.isNullOrBlank()) return emptyList()

    val parts = mutableListOf<String>()
    if (!brand.isNullOrBlank()) parts += "openfda.brand_name:\"${brand.replace("\"", "\\\"")}\""
    if (!substance.isNullOrBlank()) parts += "openfda.substance_name:\"${substance.replace("\"", "\\\"")}\""
    if (parts.isEmpty()) return emptyList()

    val searchQuery = parts.joinToString(" AND ")
    val encoded = URLEncoder.encode(searchQuery, "UTF-8")
    val url = "https://api.fda.gov/drug/label.json?search=$encoded&limit=10"

    return withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).get().build()
            openFdaClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList<RemoteMedicine>()
                val body = resp.body?.string() ?: return@use emptyList<RemoteMedicine>()
                val root = JSONObject(body)
                val arr = root.optJSONArray("results") ?: return@use emptyList<RemoteMedicine>()

                val out = mutableListOf<RemoteMedicine>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val openfda = item.optJSONObject("openfda")

                    val brandName = openfda?.optJSONArray("brand_name")?.optString(0, null)
                        ?: openfda?.optString("brand_name", null)

                    val substanceName = openfda?.optJSONArray("substance_name")?.optString(0, null)
                        ?: openfda?.optString("substance_name", null)

                    out += RemoteMedicine(
                        brand = brandName,
                        activeSubstance = substanceName,
                        strength = null,
                        form = null,
                        source = "openfda_label"
                    )
                }
                out
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

// ===========================================================
// KTOR MODULE
// ===========================================================
fun Application.medsModule() {

    install(CallLogging)
    install(CORS) { anyHost() }

    install(ContentNegotiation) {
        jackson {
            registerModule(KotlinModule.Builder().build())
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(StatusPages) {
        exception<Throwable> { call, t ->
            t.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to "Unhandled exception",
                    "message" to (t.message ?: "no message"),
                    "type" to (t::class.qualifiedName ?: "unknown")
                )
            )
        }
    }

    routing {
        get("/") { call.respondText("OK") } // stop 404 noise
        get("/health") { call.respondText("OK") }

        visionRoutes()
        scanRoutes()

        get("/meds/parse-ocr") {
            val text = call.request.queryParameters["text"] ?: ""
            val normalized = normalizeOcrText(text)
            val (brand, strength, form) = parseLocalMedicine(normalized)
            val gemini = callGeminiExtraction(text)

            val response = ParsedOcr(
                rawText = text,
                normalizedText = normalized,
                brandGuess = brand,
                strengthGuess = strength,
                formGuess = form,
                gemini = gemini
            )

            call.respondText(
                jsonMapper.writeValueAsString(response),
                ContentType.Application.Json
            )
        }
    }
}

// ===========================================================
// MAIN
// ===========================================================
fun main() {
    println("Working dir = " + java.io.File(".").absolutePath)
    logGeminiKeyPresence()

    embeddedServer(
        Netty,
        host = "0.0.0.0",
        port = 8080,
        configure = {
            requestQueueLimit = 128
            runningLimit = 128
            callGroupSize = 16
            workerGroupSize = 16
        }
    ) {
        medsModule()
    }.start(wait = true)
}
