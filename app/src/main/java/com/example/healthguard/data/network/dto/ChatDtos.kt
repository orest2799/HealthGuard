package com.example.healthguard.data.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val sessionId: String? = null,
    val text: String,
    val context: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val sessionId: String,
    val reply: String,
    val metadata: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class ChatSource(
    val title: String,
    val url: String
)