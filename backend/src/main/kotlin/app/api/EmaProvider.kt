// backend/src/main/kotlin/app/api/EmaProvider.kt
package app.api

import app.RemoteMedicine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object EmaProvider {
    private val http = OkHttpClient()

    suspend fun searchByName(name: String): List<RemoteMedicine> = withContext(Dispatchers.IO) {
        val token = EmaAuth.getToken() ?: return@withContext emptyList()

        val url = "https://api.ema.europa.eu/medicines/v1/products" +
                "?search=${URLEncoder.encode(name, StandardCharsets.UTF_8)}"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        return@withContext try {
            http.newCall(request).execute().use { resp ->
                val raw = resp.body?.string()
                println("EMA RAW: $raw")

                if (!resp.isSuccessful || raw == null) {
                    println("EMA search HTTP error: ${resp.code}")
                    return@use emptyList<RemoteMedicine>()
                }

                val json = JSONObject(raw)
                val items = json.optJSONArray("items") ?: return@use emptyList<RemoteMedicine>()

                (0 until items.length()).mapNotNull { i ->
                    val item = items.getJSONObject(i)

                    RemoteMedicine(
                        brand = item.optString("name", null),
                        activeSubstance = item.optJSONArray("activeSubstances")?.optString(0, null),
                        strength = item.optString("strength", null),
                        form = item.optString("pharmaceuticalForm", null),
                        source = "ema"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
