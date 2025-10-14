package com.example.healthguard

import org.json.JSONObject

fun parseOpenFda(json: String): List<Map<String, String>> {
    val out = mutableListOf<Map<String, String>>()
    val root = runCatching { JSONObject(json) }.getOrNull() ?: return out
    val results = root.optJSONArray("results") ?: return out
    for (i in 0 until results.length()) {
        val obj = results.optJSONObject(i) ?: continue
        val brand = obj.optString("brand_name", obj.optJSONArray("brand_name")?.optString(0) ?: "")
        val generic = obj.optString("generic_name", obj.optJSONArray("generic_name")?.optString(0) ?: "")
        val labeler = obj.optString("labeler_name", obj.optJSONArray("labeler_name")?.optString(0) ?: "")
        out += mapOf("brand" to brand, "generic" to generic, "labeler" to labeler)
    }
    return out
}
