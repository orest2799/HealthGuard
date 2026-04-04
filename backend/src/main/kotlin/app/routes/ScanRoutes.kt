package app.routes



import app.models.MedProvider
import app.models.MedQuery
import app.models.MedRecord
import app.meds.extractDosages
import app.meds.generateCandidates
import app.meds.scoreRecord
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ScanRequest(
    @param:JsonProperty("ocr_text") val ocrText: String,
    @param:JsonProperty("image_base64") val imageBase64: String? = null
)

data class ScanSavedResponse(
    val id: String,
    val saved: Boolean
)

private val mapper = ObjectMapper()
    .registerKotlinModule()
    .enable(SerializationFeature.INDENT_OUTPUT)

fun Route.scanRoutes() {
    val http = OkHttpClient() // currently unused but kept if you add providers later

    // 🔧 For now, no MedProviders wired here. If you still have an old provider
    // that implements MedProvider (e.g. GalinosProvider), put it in this list.
    val providers: List<MedProvider> = emptyList()

    post("/scan") {
        val body = call.receive<ScanRequest>()
        val q = body.ocrText.trim()
        if (q.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing ocr_text"))
            return@post
        }

        // 1) derive candidates + OCR tokens/dosages
        val candidates = generateCandidates(q)
        val ocrTokens = q.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s/+-]"), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .toSet()
        val ocrDosages = extractDosages(q).toSet()

        // 2) query providers in parallel
        val raw = mutableListOf<MedRecord>()
        for (cand in candidates) {
            raw += queryAllProviders(providers, MedQuery(cand, lang = null))
        }

        // 3) score + rank
        val bestByKey = HashMap<String, MedRecord>()
        val scored = raw.map { r -> r.copy(score = scoreRecord(r, ocrTokens, ocrDosages)) }

        for (r in scored) {
            val key = listOf(r.brand ?: "", r.generic ?: "", r.holder ?: "")
                .joinToString("|")
                .lowercase()

            val prev = bestByKey[key]
            val prevScore = prev?.score ?: Double.NEGATIVE_INFINITY
            val newScore = r.score ?: Double.NEGATIVE_INFINITY

            if (prev == null || newScore > prevScore) {
                bestByKey[key] = r
            }
        }

        val ranked = bestByKey.values
            .filter { (it.score ?: 0.0) > 0.0 }
            .sortedByDescending { it.score ?: Double.NEGATIVE_INFINITY }
            .take(10)

        // 4) write JSON file
        val outDir = File("/tmp/scans").apply { mkdirs() }
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val safeQ = q.take(40).replace(Regex("[^\\w\\-]+"), "_")
        val id = "${ts}_${safeQ}"
        val outFile = File(outDir, "scan_$id.json")

        val payload = mapOf(
            "id" to id,
            "ocr_text" to q,
            "image_base64" to body.imageBase64,
            "candidates" to candidates,
            "results" to ranked
        )
        runCatching { outFile.writeText(mapper.writeValueAsString(payload)) }

        call.respond(HttpStatusCode.OK, ScanSavedResponse(id = id, saved = outFile.exists()))
    }
}

/** Query all providers in parallel (renamed to avoid conflicts). */
private suspend fun queryAllProviders(
    providers: List<MedProvider>,
    q: MedQuery
): List<MedRecord> = coroutineScope {
    val tasks = providers.map { p ->
        async { runCatching { p.search(q) }.getOrElse { emptyList() } }
    }
    tasks.flatMap { it.await() }
}
