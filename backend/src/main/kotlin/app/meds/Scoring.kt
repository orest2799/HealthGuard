package app.meds

import app.models.MedRecord

fun scoreRecord(
    r: MedRecord,
    ocrTokens: Set<String>,
    ocrDosages: Set<String>
): Double {
    var s = 0.0
    fun bump(v: Double) { s += v }

    // brand/generic tokens match OCR tokens
    listOfNotNull(r.brand, r.generic)
        .flatMap { it.split(Regex("\\s+")) }
        .map { it.lowercase() }
        .forEach { if (it in ocrTokens) bump(1.0) }


    r.substances.forEach { sub ->
        sub.split(Regex("\\s+"))
            .map { it.lowercase() }
            .forEach { if (it in ocrTokens) bump(0.8) }
    }

    // dosage hints found in strength/summary
    val strengthText: String =
        listOfNotNull(r.strength, r.summary).joinToString(" ").lowercase()
    if (ocrDosages.any { d -> strengthText.contains(d) }) bump(2.0)

    // small boost if OCR looks Greek and record is GR/EL
    if (ocrTokens.any { hasGreek(it) }) {
        val isGreekRecord =
            (r.country ?: "").equals("GR", true) ||
                    (r.language ?: "").lowercase().startsWith("el")
        if (isGreekRecord) bump(0.5)
    }

    return s
}
