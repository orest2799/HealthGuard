package app

import app.routes.chatRoutes
import app.routes.ocrRoutes
import app.routes.scanRoutes
import app.routes.visionRoutes
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun main() {
    // Το Cloud Run ορίζει τη θύρα μέσω της μεταβλητής PORT
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        jackson {
            enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
            registerKotlinModule()
        }
    }

    routing {
        // 1. Διόρθωση Health Check (Το Android ζητάει /health, όχι σκέτο /)
        get("/health") {
            call.respond(mapOf("status" to "healthy", "version" to "1.0.1"))
        }

        // Καλό είναι να κρατήσεις και το root
        get("/") {
            call.respond(mapOf("message" to "Server is running"))
        }

        // 2. Οργάνωση των API routes για να αποφεύγονται τα 404
        route("/api") {
            route("/vision") {
                // Αν το Android καλεί /api/vision/scan, αυτό θα το πιάσει
                visionRoutes()
            }
            chatRoutes()
        }

        // 3. Προσθήκη του meds endpoint (για να μη βγάζει 404 στο aspirin)
        route("/meds") {
            get("/search") {
                val query = call.parameters["q"] ?: "Unknown"

                // Κατασκευή του Response σύμφωνα με το MedSearchResponse.kt
                val response = mapOf(
                    "query" to query,
                    "lang" to "el",
                    "results" to listOf(
                        mapOf(
                            "id" to "temp_123",
                            "brand" to query,      // ΣΗΜΑΝΤΙΚΟ: Το Android θέλει "brand"
                            "generic" to "N/A",    // ΣΗΜΑΝΤΙΚΟ: Το Android θέλει "generic"
                            "summary" to "Αυτόματη αναγνώριση για το φάρμακο $query. Ρωτήστε τον Gemini για δοσολογία.",
                            "score" to 1.0
                        )
                    )
                )

                call.respond(response)
            }
        }

        // Τα υπόλοιπα routes όπως τα είχες
        scanRoutes()
        ocrRoutes()
    }
}