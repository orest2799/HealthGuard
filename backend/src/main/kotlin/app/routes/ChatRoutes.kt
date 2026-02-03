package app.routes

import app.dto.ChatRequest
import app.services.GeminiService
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post


fun Route.chatRoutes() {
    post("/chat") {
        try {
            val request = call.receive<ChatRequest>()
            val response = GeminiService.chat(
                message = request.text,
                historyId = request.sessionId,
                context = request.context
            )
            call.respond(response)
        } catch (e: Exception) {
            // This will print the EXACT error in your Android Studio console
            println("!!! SERVER ERROR: ${e.message}")
            e.printStackTrace()

            // Send the actual error message back to PowerShell
            call.respond(io.ktor.http.HttpStatusCode.InternalServerError, e.message ?: "Unknown Error")
        }
    }
}