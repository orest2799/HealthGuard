package app.meds

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.Duration

/**
 * Minimal working version - extracts what's publicly available
 */
class GalinosProvider(
    private val userAgent: String = DEFAULT_UA,
    private val connectTimeout: Duration = Duration.ofSeconds(12),
    private val maxBodyChars: Int = 10_000
) {

    data class GalinosHit(
        val query: String,
        val matchedTitle: String? = null,
        val url: String? = null,
        val sections: Map<String, String> = emptyMap()
    )

    companion object {
        private const val DEFAULT_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        private val KNOWN_MEDICINES = mapOf(
            "depon" to "https://www.galinos.gr/web/drugs/main/substances/paracetamol",
            "paracetamol" to "https://www.galinos.gr/web/drugs/main/substances/paracetamol",
            "παρακεταμολη" to "https://www.galinos.gr/web/drugs/main/substances/paracetamol",
        )
    }

    fun searchSingle(query: String): GalinosHit {
        val cleanQuery = query.trim().lowercase()

        // Try known medicines first
        val knownUrl = KNOWN_MEDICINES[cleanQuery]
        if (knownUrl != null) {
            return fetchAndParse(query, knownUrl)
        }

        // Try direct URL construction
        val directUrl = "https://www.galinos.gr/web/drugs/main/substances/${cleanQuery.replace(" ", "_")}"
        return fetchAndParse(query, directUrl)
    }

    private fun fetchAndParse(query: String, url: String): GalinosHit {
        val doc = fetch(url) ?: return GalinosHit(query, url = null)

        val title = doc.select("h1").firstOrNull()?.text()?.trim() ?: query
        val sections = extractSections(doc)

        return GalinosHit(
            query = query,
            matchedTitle = title,
            url = url,
            sections = sections
        )
    }

    private fun fetch(url: String): Document? = try {
        Jsoup.connect(url)
            .userAgent(userAgent)
            .timeout(connectTimeout.toMillis().toInt())
            .get()
    } catch (_: Throwable) {
        null
    }

    private fun extractSections(doc: Document): Map<String, String> {
        val sections = mutableMapOf<String, String>()

        // Extract meta description (always available)
        doc.select("meta[name=description]").firstOrNull()?.attr("content")?.let { metaDesc ->
            if (metaDesc.length > 20) {
                sections["Περιγραφή"] = metaDesc.trim().take(maxBodyChars)
            }
        }

        // Extract from div.textile elements
        doc.select("div.textile").forEachIndexed { index, element ->
            val text = element.text().trim()
            if (text.length > 50) {
                val key = "Πληροφορίες ${if (index > 0) index + 1 else ""}"
                sections[key] = text.take(maxBodyChars)
            }
        }

        // Extract any paragraph with substantial content as fallback
        if (sections.size < 2) {
            doc.select("article p, main p").forEach { p ->
                val text = p.text().trim()
                if (text.length > 100 && sections.size < 5) {
                    sections["Γενικές Πληροφορίες"] = text.take(maxBodyChars)
                }
            }
        }

        return sections
    }
}