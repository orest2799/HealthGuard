package app.meds

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
import okhttp3.OkHttpClient

// ---------------- DTOs used by /meds/search responses ----------------





private val debugJsonMapper: ObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .enable(SerializationFeature.INDENT_OUTPUT)

/** REGISTER MED ROUTES HERE — this name must match Server.module() usage */
fun Route.medRoutes() {
    val http = OkHttpClient()
    val ema = EmaProvider()
    val fda = OpenFdaProvider(http)

    // Quick probe to verify group is mounted
    get("/meds/ping") {
        call.respond(HttpStatusCode.OK, mapOf("ok" to true))
    }

    /* --------------------------- /meds/search --------------------------- */
    get("/meds/search") {
        val q = call.request.queryParameters["q"]?.trim().orEmpty()
        val lang = call.request.queryParameters["lang"]?.trim()
        val sourceParam = (call.request.queryParameters["source"] ?: "auto").trim().lowercase()

        if (q.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing q"))
            return@get
        }

        val providers: List<MedProvider> = when (sourceParam) {
            "ema" -> listOf(ema)
            "openfda" -> listOf(fda)
            "both" -> listOf(ema, fda)
            "auto" -> when {
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
        val scored = rawResults.map { r -> r.copy(score = scoreRecord(r, ocrTokens, ocrDosages)) }

        for (r in scored) {
            val key = listOf(r.brand ?: "", r.generic ?: "", r.holder ?: "")
                .joinToString("|").lowercase()
            val prev = bestByKey[key]
            if (prev == null || (r.score ?: 0.0) > (prev.score ?: 0.0)) {
                bestByKey[key] = r
            }
        }

        val results = bestByKey.values
            .filter { (it.score ?: 0.0) > 0.0 }
            .sortedByDescending { it.score ?: 0.0 }
            .take(10)

        call.respond(HttpStatusCode.OK, MedSearchResponse(query = q, lang = lang, results = results))
    }

    /* --------------------------- /meds/debug ---------------------------- */
    get("/meds/debug") {
        val q = call.request.queryParameters["q"]?.trim().orEmpty()
        val lang = call.request.queryParameters["lang"]?.trim()
        val sourceParam = (call.request.queryParameters["source"] ?: "auto").trim().lowercase()

        if (q.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing q"))
            return@get
        }

        val providers: List<MedProvider> = when (sourceParam) {
            "ema" -> listOf(ema)
            "openfda" -> listOf(fda)
            "both" -> listOf(ema, fda)
            "auto" -> when {
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
        val scored = rawResults.map { r -> r.copy(score = scoreRecord(r, ocrTokens, ocrDosages)) }

        for (r in scored) {
            val key = listOf(r.brand ?: "", r.generic ?: "", r.holder ?: "")
                .joinToString("|").lowercase()
            val prev = bestByKey[key]
            if (prev == null || (r.score ?: 0.0) > (prev.score ?: 0.0)) {
                bestByKey[key] = r
            }
        }

        val ranked = bestByKey.values
            .filter { (it.score ?: 0.0) > 0.0 }
            .sortedByDescending { it.score ?: 0.0 }
            .take(10)



    }
}

/* ------------------------------- helpers ------------------------------- */
private suspend fun aggregate(providers: List<MedProvider>, q: MedQuery): List<MedRecord> = coroutineScope {
    val tasks = providers.map { p ->
        async { runCatching { p.search(q) }.getOrElse { emptyList() } }
    }
    tasks.flatMap { it.await() }
}

/** Raw EMA request – returns response text (even on error) so you can inspect it. */
