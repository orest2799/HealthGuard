package app.controllers

import app.api.ChatRequest
import app.api.ChatResponse
import app.api.GalinosFacts
import app.meds.GalinosProvider
import app.services.GeminiService
import java.util.UUID

class ChatController(
    private val galinos: GalinosProvider,
    private val gemini: GeminiService
) {
    suspend fun handle(req: ChatRequest): ChatResponse {
        val sessionId = req.sessionId ?: UUID.randomUUID().toString()

        // Try to extract medication query from multiple sources
        val medQuery: String? = req.medQuery
            ?: (req.context?.get("ocrText") as? String)
            ?: req.text

        // Resolve facts from Galinos
        val facts: GalinosFacts? = resolveFacts(medQuery)

        // DEBUG: Print what we got
        println("DEBUG: medQuery = $medQuery")
        println("DEBUG: facts = $facts")
        println("DEBUG: facts.url = ${facts?.url}")
        println("DEBUG: facts.sections = ${facts?.sections}")

        // If no facts found at all, return early with helpful message
        if (facts == null || (facts.url == null && facts.sections.isEmpty())) {
            println("DEBUG: Returning early - no facts")
            return ChatResponse(
                sessionId = sessionId,
                reply = "Δεν βρέθηκαν επαρκείς πληροφορίες για αυτό το φάρμακο. Παρακαλώ ελέγξτε το όνομα και δοκιμάστε ξανά.",
                metadata = null
            )
        }

        println("DEBUG: Calling Gemini with facts")
        // Get answer from Gemini
        val answer = gemini.answerFromGalinos(req.text, facts)
        println("DEBUG: Gemini answer = $answer")

        // If Gemini returns empty, provide fallback message
        val finalReply = if (answer.isBlank()) {
            "Δεν μπόρεσα να επεξεργαστώ την ερώτηση. Παρακαλώ διατυπώστε την πιο συγκεκριμένα."
        } else {
            answer
        }

        // Build sources list
        val sources = facts.url
            ?.let { listOf(mapOf("title" to (facts.name ?: "Galinos"), "url" to it)) }
            ?: emptyList()

        return ChatResponse(
            sessionId = sessionId,
            reply = finalReply,
            metadata = mapOf("sources" to sources)
        )
    }

    private fun resolveFacts(query: String?): GalinosFacts? {
        if (query.isNullOrBlank()) return null

        val hit = galinos.searchSingle(query)

        // Only return facts if we found actual data
        if (hit.url == null && hit.sections.isEmpty()) return null

        return GalinosFacts(
            name = hit.matchedTitle ?: hit.query,
            url = hit.url,
            language = "el",
            sections = hit.sections
        )
    }
}