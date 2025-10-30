package app

import app.chat.chatRoutes
import app.meds.medRoutes
import app.routes.dbRoutes
import app.scans.scanRoutes
import app.vision.visionRoutes
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.event.Level

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(
        Netty,
        host = "0.0.0.0",
        port = port,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    install(Compression) { gzip() }
    install(CallLogging) { level = Level.INFO }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
    }
    install(ContentNegotiation) {
        jackson {
            registerModule(KotlinModule.Builder().build())
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    // Primary routes under routing { }
    routing {
        get("/health") { call.respondText("OK") }
        visionRoutes()
        medRoutes()     // ← IMPORTANT: this MUST match the function name in MedRoutes.kt
        scanRoutes()
        dbRoutes()
        chatRoutes()
    }

    // Chat routes are defined as an Application extension

}

