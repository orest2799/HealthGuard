package app.meds

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.math.max

/**
 * Provider that:
 *  1) Finds a Galinos page for a query using DuckDuckGo HTML (no API keys).
 *  2) Scrapes key sections from the resulting Galinos page.
 *
 * NOTE: This scraper is best-effort. Galinos HTML can change at any time.
 * Keep the code defensive and tolerant to structure changes.
 */
class GalinosProvider(
    private val userAgent: String = DEFAULT_UA,
    private val connectTimeout: Duration = Duration.ofSeconds(12),
    private val readTimeout: Duration = Duration.ofSeconds(20),
    private val maxBodyChars: Int = 10_000
) {

    data class GalinosHit(
        val query: String,
        val matchedTitle: String? = null,
        val url: String? = null,
        /** Map<section-title, text> */
        val sections: Map<String, String> = emptyMap()
    )

    companion object {
        private const val DEFAULT_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"

        private val TARGET_SECTION_HINTS = listOf(
            "Ενδείξεις",
            "Δοσολογία",
            "Αντενδείξεις",
            "Προειδοποιήσεις",
            "Αλληλεπιδράσεις",
            "Ανεπιθύμητες ενέργειες",
            "Κυοφορία",
            "Γαλουχία",
            "Υπερδοσολογία",
            "Φαρμακοδυναμική",
            "Φαρμακοκινητική",
        )
    }

    /** Public: one-shot search that returns the first/best Galinos page parsed. */
    fun searchSingle(query: String): GalinosHit {
        // 1) find a Galinos URL for the query
        val url = findGalinosUrlWithDuckDuckGo(query)

        // 2) if nothing found, return a minimal hit (lets the chat say it didn't find details)
        if (url == null) {
            return GalinosHit(
                query = query,
                matchedTitle = null,
                url = null,
                sections = emptyMap()
            )
        }

        // 3) fetch & parse sections from that page
        val doc = fetch(url) ?: return GalinosHit(query, url = url)
        val title = doc.title().orEmpty().trim().ifBlank { null }
        val sections = extractSections(doc)

        return GalinosHit(
            query = query,
            matchedTitle = title,
            url = url,
            sections = sections
        )
    }

    /** --------------------------------------------------------------------- **/
    /**                            SEARCH (DuckDuckGo)                         **/
    /** --------------------------------------------------------------------- **/

    /**
     * Use DuckDuckGo HTML results: https://duckduckgo.com/html/?q=site:galinos.gr+Augmentin
     * We scan the first result links for host `galinos.gr` (prefer paths containing `/web/`).
     */
    private fun findGalinosUrlWithDuckDuckGo(query: String): String? {
        val q = URLEncoder.encode("site:galinos.gr $query", StandardCharsets.UTF_8)
        val url = "https://duckduckgo.com/html/?q=$q"

        val doc = fetch(url) ?: return null

        // DuckDuckGo HTML page links are under 'a.result__a' or 'a.result__a[href]'
        val links = doc.select("a.result__a[href]")
        for (a in links) {
            val href = a.absUrl("href").trim()
            if (href.contains("galinos.gr")) {
                // prefer drug pages with /web/drugs or /web/...
                if (href.contains("/web/")) return href
                // otherwise, still return a galinos.gr link
                return href
            }
        }
        return null
    }

    /** --------------------------------------------------------------------- **/
    /**                           FETCH & PARSE PAGE                           **/
    /** --------------------------------------------------------------------- **/

    private fun fetch(url: String): Document? = try {
        Jsoup.connect(url)
            .userAgent(userAgent)
            .timeout(connectTimeout.toMillis().toInt())
            .get()
    } catch (_: Throwable) {
        null
    }

    /**
     * Extract sections from the Galinos page. We build a (title -> text) map by
     * iterating headings (h1/h2/h3) and concatenating subsequent text nodes
     * until the next heading.
     *
     * We then keep the most relevant Greek sections (TARGET_SECTION_HINTS).
     */
    private fun extractSections(doc: Document): Map<String, String> {
        // Grab main content node if possible, fall back to whole body
        val container: Element = doc.selectFirst("article,main,.content,.container,.page-content,body") ?: doc.body()

        // Build a raw section map from headings
        val rawSections = LinkedHashMap<String, StringBuilder>()
        var currentTitle = "Πληροφορίες"

        fun pushText(text: String) {
            if (text.isBlank()) return
            rawSections.getOrPut(currentTitle) { StringBuilder() }
                .apply {
                    if (isNotEmpty()) append("\n\n")
                    append(text)
                }
        }

        // iterate through elements; when you see a heading, start a new section
        for (el in container.allElements) {
            val tag = el.tagName().lowercase()
            when (tag) {
                "h1", "h2", "h3" -> {
                    val title = el.text().clean()
                    if (title.isNotBlank()) {
                        currentTitle = title
                        rawSections.putIfAbsent(currentTitle, StringBuilder())
                    }
                }
                "p", "li", "td", "div", "section" -> {
                    val text = el.text().clean()
                    if (text.isNotBlank() && text.length < maxBodyChars) {
                        pushText(text)
                    }
                }
            }
        }

        // Convert StringBuilders to Strings + trim
        val all = rawSections.mapValues { (_, sb) ->
            sb.toString().trim().take(maxBodyChars)
        }.filterValues { it.isNotBlank() }

        if (all.isEmpty()) return emptyMap()

        // If we can match the well-known Greek section names, keep/rename them
        val scored = all.entries.map { (k, v) ->
            val score = sectionNameScore(k)
            k to Pair(score, v)
        }

        // Sort by how well the title matches known Greek hints, keep top first of each hint
        val byBest = LinkedHashMap<String, String>()

        // 1) try to map to target hints
        for (hint in TARGET_SECTION_HINTS) {
            val best = scored
                .filter { (title, _) -> title.contains(hint, ignoreCase = true) }
                .maxByOrNull { it.second.first }  // best score
            if (best != null) byBest[hint] = best.second.second
        }

        // 2) if nothing matched, keep the first couple of large sections
        if (byBest.isEmpty()) {
            val top = scored.sortedByDescending { it.second.first }.take(3)
            for ((title, pair) in top) {
                byBest[title] = pair.second
            }
        }

        return byBest
    }

    /** Simple score: longer overlap with known words → larger score. */
    private fun sectionNameScore(title: String): Int {
        val t = title.lowercase()
        var s = 0
        for (hint in TARGET_SECTION_HINTS) {
            if (t.contains(hint.lowercase())) {
                s += max(1, hint.length / 3)
            }
        }
        return s
    }

    /** Basic cleanup & normalization for Greek text. */
    private fun String.clean(): String =
        this.replace(Regex("\\s+"), " ").trim()
}
// inside GalinosProvider
