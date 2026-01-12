// app/meds/OpenFdaProvider.kt
package app.meds

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object OpenFdaProvider {

    private const val BASE_URL = "https://api.fda.gov/drug/label.json"

    suspend fun search(brand: String?, substance: String?): List<MedicineInfo> =
        withContext(Dispatchers.IO) {
            val searchParts = mutableListOf<String>()

            if (!brand.isNullOrBlank()) {
                // brand name search
                val b = escape(brand)
                searchParts += "openfda.brand_name:\"$b\""
            }
            if (!substance.isNullOrBlank()) {
                val s = escape(substance)
                searchParts += "openfda.substance_name:\"$s\""
            }

            if (searchParts.isEmpty()) return@withContext emptyList()

            val searchQuery = searchParts.joinToString(" AND ")
            val urlStr = "$BASE_URL?search=${urlEncode(searchQuery)}&limit=10"

            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            conn.inputStream.bufferedReader().use { reader ->
                val body = reader.readText()
                parseOpenFda(body)
            }
        }

    private fun parseOpenFda(jsonStr: String): List<MedicineInfo> {
        val root = JSONObject(jsonStr)
        if (!root.has("results")) return emptyList()

        val resultsJson = root.getJSONArray("results")
        val out = mutableListOf<MedicineInfo>()

        for (i in 0 until resultsJson.length()) {
            val obj = resultsJson.getJSONObject(i)

            val openFda = obj.optJSONObject("openfda")

            val brand = openFda?.optJSONArray("brand_name")?.optString(0)
            val substance = openFda?.optJSONArray("substance_name")?.optString(0)

            // very rough guesses from the big label text
            val dosageAndAdmin = obj.optJSONArray("dosage_and_administration")
                ?.optString(0)
                ?: ""
            val indications = obj.optJSONArray("indications_and_usage")
                ?.optString(0)
                ?: ""

            val combinedText = (dosageAndAdmin + " " + indications).uppercase()

            val strengthGuess = guessStrength(combinedText)
            val formGuess = guessForm(combinedText)

            out += MedicineInfo(
                brand = brand,
                activeSubstance = substance,
                strength = strengthGuess,
                form = formGuess,
                source = "openfda_label"
            )
        }

        return out
    }

    private fun guessStrength(text: String): String? {
        val regex = Regex("(\\d+\\s*(MG|G|ML|MCG|IU))")
        return regex.find(text)?.value
    }

    private fun guessForm(text: String): String? {
        val forms = listOf("TABLET", "TABLETS", "CAPSULE", "CAPSULES", "SYRUP", "SOLUTION")
        return forms.firstOrNull { text.contains(it) }
    }

    private fun escape(value: String): String =
        value.replace("\"", "\\\"")

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
