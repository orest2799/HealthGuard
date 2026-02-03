package app.routes

import app.services.GeminiService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.ocrRoutes() {
    route("/ocr") {
        post {
            try {
                // 1. Receive the raw image bytes
                val imageBytes = call.receive<ByteArray>()

                // 2. Call GeminiService (ensure it returns MedicineOcrResult? now)
                val result = GeminiService.processImage(imageBytes)

                if (result != null) {
                    // 3. Respond with the structured JSON object
                    call.respond(result)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Gemini failed to parse image"))
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (t.message ?: "Unknown server error"))
                )
            }
        }
    }
}