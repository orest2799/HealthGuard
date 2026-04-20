package app.routes

import app.dto.ChatRequest
import app.services.GeminiService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.chatRoutes() {

    post("/chat") {
        try {
            val request = call.receive<ChatRequest>()
            if (request.text.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "text is required"))
                return@post
            }

            // language comes from the request context — "el" or "en"
            val language = request.context?.get("language") ?: "el"

            val response = GeminiService.chat(
                message = request.text,
                historyId = request.sessionId,
                context = request.context,
                language = language
            )
            call.respond(HttpStatusCode.OK, response)

        } catch (e: Exception) {
            println("!!! SERVER ERROR: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown Error")))
        }
    }

    post("/chat/from-image") {
        try {
            val imageBytes = call.receive<ByteArray>()

            if (imageBytes.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Empty image body"))
                return@post
            }
            if (imageBytes.size > 10 * 1024 * 1024) {
                call.respond(HttpStatusCode.PayloadTooLarge, mapOf("error" to "Image too large (max 10MB)"))
                return@post
            }

            val ocr = GeminiService.processImage(imageBytes)
            if (ocr == null || ocr.brand.isNullOrBlank()) {
                call.respond(HttpStatusCode.UnprocessableEntity, mapOf("error" to "Could not read medicine from image"))
                return@post
            }

            val brand = ocr.brand ?: "the medicine"
            val substance = ocr.activeSubstance ?: ""
            val strength = ocr.strength ?: ""
            val form = ocr.form ?: ""

            // Default to Greek for image scans (can be changed if needed)
            val language = call.request.headers["X-Chat-Language"] ?: "el"

            val firstMessage = if (language == "el") """
                Ο χρήστης έστειλε φωτογραφία συσκευασίας φαρμάκου.
                MEDICINE_BRAND: $brand
                ACTIVE_SUBSTANCE: $substance
                STRENGTH: $strength
                FORM: $form
                Δώσε γενικές πληροφορίες για το φάρμακο.
            """.trimIndent() else """
                The user sent a photo of a medicine package.
                MEDICINE_BRAND: $brand
                ACTIVE_SUBSTANCE: $substance
                STRENGTH: $strength
                FORM: $form
                Give general information about this medicine.
            """.trimIndent()

            val chat = GeminiService.chat(
                message = firstMessage,
                historyId = null,
                context = mapOf(
                    "brand" to brand,
                    "activeSubstance" to substance,
                    "strength" to strength,
                    "form" to form,
                    "language" to language
                ),
                language = language
            )

            val meta = (chat.metadata ?: emptyMap()) + mapOf("ocr" to ocr)
            call.respond(HttpStatusCode.OK, chat.copy(metadata = meta))

        } catch (e: Exception) {
            println("!!! IMAGE CHAT ERROR: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown Error")))
        }
    }
}