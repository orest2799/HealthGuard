package app.meds

/* ---------- Search/aggregation models (EMA/OpenFDA) ---------- */

data class MedRecord(
    val source: String? = null,
    val id: String? = null,

    val generic: String? = null,
    val brand: String? = null,

    val strength: String? = null,
    val form: String? = null,
    val route: String? = null,

    val holder: String? = null,
    val country: String? = null,
    val atc: String? = null,

    // tokens (substances) that may match OCR
    val substances: List<String> = emptyList(),

    val summary: String? = null,
    val url: String? = null,

    // hint for language (“en”, “el”, …)
    val language: String? = null,

    // computed match score (higher is better)
    val score: Double? = null
)

data class MedSearchResponse(
    val query: String,
    val lang: String? = null,
    val results: List<MedRecord> = emptyList()
)

data class MedQuery(
    val q: String,
    val lang: String? = null
)

/* ---------- Gemini OCR extraction models ---------- */

data class MedField(
    val name: String,
    val strength: String? = null,
    val form: String? = null,
    val frequency: String? = null,
    val route: String? = null
)

data class ExtractionResult(
    val medicines: List<MedField> = emptyList()
)
