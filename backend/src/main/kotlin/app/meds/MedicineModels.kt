package app.meds

data class Substance(
    val name: String,
    val strength: String? = null
)


// Request body from client
data class OcrParseRequest(
    val ocrText: String
)

// Response back to client
data class OcrParseResponse(
    val normalizedText: String,
    val heuristicBrand: String?,
    val heuristicStrength: String?,
    val heuristicForm: String?,
    val geminiParsed: MedicineInfo? = null,
    val error: String? = null
)
