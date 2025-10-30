package app.chat

import app.controllers.ChatController
import app.meds.GalinosProvider
import app.services.GeminiService
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

// Changed from Application.chatRoutes() to Route.chatRoutes()
fun Route.chatRoutes() {
    val galinos = GalinosProvider()
    val gemini = GeminiService.fromEnv()
    val controller = ChatController(galinos, gemini)

    post("/chat") {
        val req = call.receive<app.api.ChatRequest>()
        val resp = controller.handle(req)
        call.respond(resp)
    }
}