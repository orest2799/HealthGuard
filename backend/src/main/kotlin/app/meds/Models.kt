package app.meds

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

    // ✅ NEW: list of active substances (tokens you may match against OCR text)
    val substances: List<String> = emptyList(),

    val summary: String? = null,
    val url: String? = null,

    // ✅ NEW: language hint for the record (“en”, “el”, etc.)
    val language: String? = null,

    val score: Double? = null
)

data class MedSearchResponse(
    val query: String,
    val lang: String? = null,
    val results: List<MedRecord> = emptyList()
)

// Used by providers
data class MedQuery(
    val q: String,
    val lang: String? = null
)
