package app.meds

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class EmaProvider : MedProvider {
    override val source: String = "ema"

    override suspend fun search(q: MedQuery): List<MedRecord> {
        val http = OkHttpClient()
        val url = "https://epi.ema.europa.eu/consuming/api/fhir/Bundle"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("carrierValue", q.q)
            .addQueryParameter("lang", q.lang ?: "en")
            .build()

        val req = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || body.isBlank()) return emptyList()

                val json = JSONObject(body)
                val entries = json.optJSONArray("entry") ?: return emptyList()

                (0 until entries.length()).mapNotNull { i ->
                    val res = entries.optJSONObject(i)
                        ?.optJSONObject("resource") ?: return@mapNotNull null

                    MedRecord(
                        source = source,
                        id = res.optString("id", null),
                        brand = res.optString("title", null),    // adjust to real fields
                        generic = null,
                        substances = emptyList(),
                        strength = null,
                        form = null,
                        route = null,
                        holder = null,
                        country = "EU",
                        atc = null,
                        summary = res.optString("description", null),
                        url = null,
                        language = q.lang ?: "en",
                        score = 0.0
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}

