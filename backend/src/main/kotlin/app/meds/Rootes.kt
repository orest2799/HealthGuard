package app.meds

// Jackson for pretty JSON file output
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/* -------------------------------------------------------------------------- */
/*  Debug DTOs (only used by /meds/debug)                                     */
/* -------------------------------------------------------------------------- */
private data class DebugBySource(
    val openfda: List<MedRecord>,
    val emaParsed: List<MedRecord>   // ← renamed (no underscore)
)

private data class DebugResponse(
    val query: String,
    val lang: String?,
    val source: String,
    val candidates: List<String>,
    val ocrTokens: List<String>,
    val ocrDosages: List<String>,
    val rankedResults: List<MedRecord>,
    val bySource: DebugBySource,
    val emaRaw: String?
)

/* Jackson mapper (pretty print) */
private val debugJsonMapper: ObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .enable(SerializationFeature.INDENT_OUTPUT)

/* -------------------------------------------------------------------------- */
/*  Routes                                                                     */
/* -------------------------------------------------------------------------- */
fun Route.medsRoutes() {
    val http = OkHttpClient()

    // Keep single instances and pick the subset per request
    val ema = EmaProvider()
    val fda = OpenFdaProvider(http)

    /* ------------------------------- /meds/search ------------------------------ */
    get("/meds/search") {
        val q = call.request.queryParameters["q"]?.trim().orEmpty()
        val lang = call.request.queryParameters["lang"]?.trim()
        val sourceParam = (call.request.queryParameters["source"] ?: "auto").trim().lowercase()

        if (q.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing q"))
            return@get
        }

        // Decide which providers to query
        val providers: List<MedProvider> = when (sourceParam) {
            "ema"     -> listOf(ema)
            "openfda" -> listOf(fda)
            "both"    -> listOf(ema, fda)
            "auto"    -> when {
                // Greek -> EMA first, then OpenFDA as fallback
                lang?.startsWith("el") == true -> listOf(ema, fda)
                // US/EN -> OpenFDA first, then EMA
                lang == "us" || lang?.startsWith("en") == true -> listOf(fda, ema)
                else -> listOf(ema, fda)
            }
            else -> listOf(ema, fda)
        }

        val candidates = generateCandidates(q)
        val ocrTokens = q.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s/+-]"), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .toSet()
        val ocrDosages = extractDosages(q).toSet()

        val rawResults = mutableListOf<MedRecord>()
        for (cand in candidates) {
            rawResults += aggregate(providers, MedQuery(cand, lang))
        }

        // de-dupe by a composite key and keep best score
        val bestByKey = HashMap<String, MedRecord>()
        val scored = rawResults.map { r ->
            r.copy(score = scoreRecord(r, ocrTokens, ocrDosages))
        }
        for (r in scored) {
            val key = listOf(r.brand ?: "", r.generic ?: "", r.holder ?: "")
                .joinToString("|")
                .lowercase()
            val prev = bestByKey[key]
            if (prev == null || (r.score ?: 0.0) > (prev.score ?: 0.0)) bestByKey[key] = r
        }

        val results = bestByKey.values
            .filter { (it.score ?: 0.0) > 0.0 }
            .sortedByDescending { it.score ?: 0.0 }
            .take(10)

        call.respond(
            HttpStatusCode.OK,
            MedSearchResponse(query = q, lang = lang, results = results) // <- lang now valid
        )
    }

    /* -------------------------------- /meds/debug ------------------------------ */
    get("/meds/debug") {
        val q = call.request.queryParameters["q"]?.trim().orEmpty()
        val lang = call.request.queryParameters["lang"]?.trim()
        val sourceParam = (call.request.queryParameters["source"] ?: "auto").trim().lowercase()

        if (q.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing q"))
            return@get
        }

        val providers: List<MedProvider> = when (sourceParam) {
            "ema"     -> listOf(ema)
            "openfda" -> listOf(fda)
            "both"    -> listOf(ema, fda)
            "auto"    -> when {
                lang?.startsWith("el") == true -> listOf(ema, fda)
                lang == "us" || lang?.startsWith("en") == true -> listOf(fda, ema)
                else -> listOf(ema, fda)
            }
            else -> listOf(ema, fda)
        }

        val candidates = generateCandidates(q)
        val ocrTokens = q.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s/+-]"), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .toSet()
        val ocrDosages = extractDosages(q).toSet()

        val rawResults = mutableListOf<MedRecord>()
        for (cand in candidates) {
            rawResults += aggregate(providers, MedQuery(cand, lang))
        }

        val bestByKey = HashMap<String, MedRecord>()
        val scored = rawResults.map { r ->
            r.copy(score = scoreRecord(r, ocrTokens, ocrDosages))
        }
        for (r in scored) {
            val key = listOf(r.brand ?: "", r.generic ?: "", r.holder ?: "")
                .joinToString("|")
                .lowercase()
            val prev = bestByKey[key]
            if (prev == null || (r.score ?: 0.0) > (prev.score ?: 0.0)) bestByKey[key] = r
        }
        val ranked = bestByKey.values
            .filter { (it.score ?: 0.0) > 0.0 }
            .sortedByDescending { it.score ?: 0.0 }
            .take(10)

        // Individual sources for visibility
        val openfdaOnly = fda.search(MedQuery(q, lang))
        val emaParsed   = ema.search(MedQuery(q, lang))
        val emaRaw      = emaRawSearch(http, q) // raw text (or error string)

        val payload = DebugResponse(
            query = q,
            lang = lang,
            source = sourceParam,
            candidates = candidates,
            ocrTokens = ocrTokens.toList(),
            ocrDosages = ocrDosages.toList(),
            rankedResults = ranked,
            bySource = DebugBySource(
                openfda = openfdaOnly,
                emaParsed = emaParsed        // ← renamed field used here
            ),
            emaRaw = emaRaw
        )

        // Save debug JSON to disk
        try {
            val outDir = File("build/outputs/debug").apply { mkdirs() }
            val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val safeQ = q.take(40).replace(Regex("[^\\w\\-]+"), "_")
            val outFile = File(outDir, "debug_${ts}_${safeQ}.json")
            outFile.writeText(debugJsonMapper.writeValueAsString(payload))
        } catch (_: Throwable) {
            // ignore file IO errors
        }

        call.respond(HttpStatusCode.OK, payload)
    }
}

/* -------------------------------------------------------------------------- */
/*  Helpers                                                                    */
/* -------------------------------------------------------------------------- */

private suspend fun aggregate(providers: List<MedProvider>, q: MedQuery): List<MedRecord> =
    coroutineScope {
        val tasks = providers.map { p ->
            async { runCatching { p.search(q) }.getOrElse { emptyList() } }
        }
        tasks.flatMap { it.await() }
    }

/** Raw EMA request – returns response text (even on error) so you can inspect it. */
private fun emaRawSearch(http: OkHttpClient, search: String): String? {
    val url = "https://epi.ema.europa.eu/consuming/api/fhir/ListBySearchParameter"
        .toHttpUrl()
        .newBuilder()
        .addQueryParameter("search", search)
        .build()

    val req = Request.Builder()
        .url(url)
        .header("Accept", "application/json")
        .get()
        .build()

    return try {
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string()
            if (resp.isSuccessful) text
            else "HTTP ${resp.code}: ${resp.message}\n${text.orEmpty()}"
        }
    } catch (t: Throwable) {
        "ERROR: ${t.message}"
    }
}
