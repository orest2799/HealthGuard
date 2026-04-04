package app.dto

data class ChatRequest(
    val sessionId: String? = null,
    val text: String = "",
    val context: Map<String, String>? = null
)

data class ChatResponse(
    val sessionId: String,
    val reply: String,
    val metadata: Map<String, Any?>? = null
)

data class ChatSource(
    val title: String,
    val url: String
)
