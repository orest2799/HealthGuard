package app.meds

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class OpenFdaProvider(
    private val http: OkHttpClient,
    private val apiKey: String? = System.getenv("OPENFDA_API_KEY")
) : MedProvider {

    override val source: String = "openfda"
    private val base = "https://api.fda.gov/drug/label.json"

    override suspend fun search(q: MedQuery): List<MedRecord> {
        val out = LinkedHashSet<MedRecord>()
        val candidates = generateCandidates(q.q)

        for (cand in candidates) {
            val url = buildUrl(cand)
            val req = Request.Builder().url(url).get().build()

            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@use

                val json = runCatching { JSONObject(body) }.getOrNull() ?: return@use
                val results = json.optJSONArray("results") ?: return@use

                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val openfda = item.optJSONObject("openfda")

                    val productTypes = openfda?.optJSONArray("product_type")?.toStringList()
                    val isHumanDrug = productTypes?.any {
                        it.contains("HUMAN", true) &&
                                (it.contains("PRESCRIPTION", true) || it.contains("OTC", true))
                    } == true
                    if (!isHumanDrug) continue

                    if (looksLikeCosmeticOrSupplement(item)) continue

                    out += toRecord(item)
                }
            }
            if (out.size >= 20) break
        }

        return out.toList()
    }

    private fun buildUrl(query: String, limit: Int = 20) =
        base.toHttpUrl().newBuilder()
            .addQueryParameter(
                "search",
                "(openfda.brand_name:\"$query\" " +
                        "OR openfda.generic_name:\"$query\" " +
                        "OR active_ingredient:\"$query\" " +
                        "OR openfda.substance_name:\"$query\") " +
                        "AND (openfda.product_type:\"HUMAN PRESCRIPTION DRUG\" " +
                        "OR openfda.product_type:\"HUMAN OTC DRUG\")"
            )
            .addQueryParameter("limit", limit.toString())
            .apply { apiKey?.takeIf { it.isNotBlank() }?.let { addQueryParameter("api_key", it) } }
            .build()

    private fun toRecord(item: JSONObject): MedRecord {
        val openfda = item.optJSONObject("openfda")

        val brand   = openfda.optFirstString("brand_name")
        val generic = openfda.optFirstString("generic_name")
        val subs    = openfda.optArrayStrings("substance_name")
            .takeIf { it.isNotEmpty() }
            ?: item.optArrayStrings("active_ingredient")

        val labeler = openfda.optFirstString("manufacturer_name")
            ?: openfda.optFirstString("labeler_name")

        val form = openfda.optFirstString("dosage_form")
        val strength = guessStrength(item)

        val summary = item.optFirstString("indications_and_usage")
            ?: item.optFirstString("purpose")
            ?: item.optFirstString("package_label_principal_display_panel")

        val splId = openfda.optFirstString("spl_id")
        val url = splId?.let { "https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=$it" }

        return MedRecord(
            source = source,
            brand = brand,
            generic = generic,
            substances = subs,
            strength = strength,
            form = form,
            atc = null,
            holder = labeler,
            country = "US",
            url = url,
            summary = summary,
            language = "en",
            score = 0.0
        )
    }

    private fun guessStrength(item: JSONObject): String? {
        val text = buildString {
            listOf(
                "active_ingredient",
                "package_label_principal_display_panel",
                "dosage_and_administration",
                "description"
            ).forEach { f ->
                val arr = item.optJSONArray(f)
                if (arr != null) for (i in 0 until arr.length()) append(' ').append(arr.optString(i))
                else item.optString(f, null)?.let { append(' ').append(it) }
            }
        }.lowercase()

        val combo = Regex("""\b\d+(?:[.,]\d+)?\s*/\s*\d+(?:[.,]\d+)?\s*(?:mg|g|mcg|µg|ml|mL)\b""")
        val single = Regex("""\b\d+(?:[.,]\d+)?\s*(?:mg|g|mcg|µg|ml|mL|iu|units)\b""", RegexOption.IGNORE_CASE)

        combo.find(text)?.let { return it.value.replace("ml", "mL") }
        single.find(text)?.let { return it.value.replace("ml", "mL") }
        return null
    }

    private fun looksLikeCosmeticOrSupplement(item: JSONObject): Boolean {
        val hay = buildString {
            fun addArr(field: String) {
                val arr = item.optJSONArray(field) ?: return
                for (i in 0 until arr.length()) append(' ').append(arr.optString(i))
            }
            val open = item.optJSONObject("openfda")
            fun addOpen(field: String) {
                val arr = open?.optJSONArray(field) ?: return
                for (i in 0 until arr.length()) append(' ').append(arr.optString(i))
            }
            addArr("purpose"); addArr("indications_and_usage"); addArr("description")
            addArr("package_label_principal_display_panel"); addArr("warnings"); addOpen("route")
        }.lowercase()

        val cosmetic = listOf(
            "cosmetic","serum","skin","shampoo","conditioner","soap","lotion",
            "sunscreen","spf","beauty","anti-aging","makeup"
        )
        val supplement = listOf("dietary supplement","food supplement","supplement facts")
        return cosmetic.any { it in hay } || supplement.any { it in hay }
    }
}

/* ---- JSON helpers ---- */

private fun JSONObject?.optFirstString(key: String): String? {
    val obj = this ?: return null
    val v = obj.opt(key) ?: return null
    return when (v) {
        is String -> v
        is JSONArray -> if (v.length() > 0) v.optString(0) else null
        else -> v.toString()
    }
}
private fun JSONObject?.optArrayStrings(key: String): List<String> {
    val obj = this ?: return emptyList()
    val v = obj.opt(key) ?: return emptyList()
    return when (v) {
        is String -> listOf(v)
        is JSONArray -> v.toStringList()
        else -> emptyList()
    }
}
private fun JSONArray.toStringList(): List<String> =
    (0 until length()).mapNotNull { idx -> optString(idx, null) }
