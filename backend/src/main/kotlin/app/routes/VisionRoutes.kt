package app.routes

import app.services.GeminiService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.visionRoutes() {


    post("/ocr") {
        try {
            val bytes = call.receive<ByteArray>()
            val result = GeminiService.processImage(bytes)

            if (result != null) {
                println("SERVER SENDING TO ANDROID: $result") // Δες το στο Google Cloud Logs
                call.respond(HttpStatusCode.OK, result)
            } else {

                call.respond(HttpStatusCode.OK, mapOf("brand" to "Unknown"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.OK, mapOf("brand" to "Error"))
        }
    }


    get("/test-local") {
        call.respond(HttpStatusCode.OK, mapOf("status" to "HealthGuard API is working!"))
    }
}