package app.meds

private val NUM_UNIT = Regex("""(?i)\b(\d+(?:[.,]\d+)?)\s*(mg|mcg|µg|g|ml|mL|IU|units)\b""")
private val STRENGTH_COMBO =
    Regex("""(?i)\b(\d+(?:[.,]\d+)?\s*(?:mg|mcg|µg|g|ml|mL|IU|units)(?:\s*/\s*\d+(?:[.,]\d+)?\s*(?:ml|mL|g))?)\b""")

fun extractDosages(text: String): List<String> {
    val hits = LinkedHashSet<String>()
    STRENGTH_COMBO.findAll(text).forEach { hits += it.value.lowercase() }
    NUM_UNIT.findAll(text).forEach { hits += it.value.lowercase() }
    return hits.toList()
}

fun generateCandidates(text: String): List<String> {
    val cleaned = normalize(text)
    val toks = tokenize(text)

    val singles = toks.sortedByDescending { it.length }.take(4)
    val pairs = toks.windowed(2, 1).map { it.joinToString(" ") }.take(3)

    return (listOf(cleaned) + singles + pairs).distinct().filter { it.isNotBlank() }
}

private fun normalize(text: String): String =
    text.replace(Regex("[^\\p{L}\\p{N}\\s/+-]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun tokenize(text: String): List<String> =
    normalize(text).split(" ").filter { it.length >= 3 }.distinct()

fun hasGreek(s: String): Boolean =
    s.any { ch -> ch in 'α'..'ω' || ch in 'Α'..'Ω' || ch in "άέήίόύώΆΈΉΊΌΎΏ" }

