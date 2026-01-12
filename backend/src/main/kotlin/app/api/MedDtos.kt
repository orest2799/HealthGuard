import kotlinx.serialization.Serializable

@Serializable
data class GeminiTextPart(val text: String? = null)

@Serializable
data class GeminiContent(val parts: List<GeminiTextPart> = emptyList())

@Serializable
data class GeminiCandidate(val content: GeminiContent? = null)

@Serializable
data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>
)
