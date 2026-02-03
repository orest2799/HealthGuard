package app.routes

import app.services.GeminiService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

// ΑΦΑΙΡΕΣΗ: Η παράμετρος dataStore έφυγε
fun Route.visionRoutes() {

    // 1. OCR ENDPOINT - Αυτό είναι το σημαντικό για το AI
    post("/ocr") {
        try {
            val bytes = call.receive<ByteArray>()
            val result = GeminiService.processImage(bytes)

            if (result != null) {
                println("SERVER SENDING TO ANDROID: $result") // Δες το στο Google Cloud Logs
                call.respond(HttpStatusCode.OK, result)
            } else {
                // Μην στέλνεις 500, στείλε ένα άδειο αντικείμενο για να μη σκάσει το κινητό
                call.respond(HttpStatusCode.OK, mapOf("brand" to "Unknown"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.OK, mapOf("brand" to "Error"))
        }
    }

    // 2. TEST ENDPOINT - Για έλεγχο αν δουλεύει ο server
    get("/test-local") {
        call.respond(HttpStatusCode.OK, mapOf("status" to "HealthGuard API is working!"))
    }
}