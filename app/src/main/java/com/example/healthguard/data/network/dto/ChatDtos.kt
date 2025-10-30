package com.example.healthguard.data.network.dto




// ---------- Request sent to POST /chat ----------
data class ChatRequest(
    val sessionId: String? = null,
    val text: String,
    val medQuery: String? = null,
    val medId: String? = null,
    val context: Map<String, Any?>? = null
)

// ---------- Response from POST /chat ----------
data class ChatResponse(
    val sessionId: String,
    val reply: String,
    val metadata: Map<String, Any?>? = null
)

data class ChatSource(
    val title: String,
    val url: String
)