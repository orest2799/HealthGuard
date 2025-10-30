package app.api

import com.fasterxml.jackson.annotation.JsonInclude

data class ChatRequest(
    val sessionId: String? = null,
    val text: String,
    // First turn only: OCR text, language hint, or anything useful
    val context: Map<String, Any?>? = null,
    // Optional hints for Galinos
    val medQuery: String? = null,
    val medId: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChatResponse(
    val sessionId: String,
    val reply: String,
    val metadata: Map<String, Any?>? = null
)

/** Used internally to pass structured Galinos facts into Gemini */
data class GalinosFacts(
    val name: String? = null,
    val atc: String? = null,
    val form: String? = null,
    val substances: List<String> = emptyList(),
    val indications: String? = null,
    val contraindications: String? = null,
    val warnings: String? = null,
    val dosage: String? = null,
    val interactions: String? = null,
    val url: String? = null,
    val language: String = "el",
    val sections: Map<String, String> = emptyMap()
)
