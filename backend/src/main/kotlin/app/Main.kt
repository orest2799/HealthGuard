package app


import app.routes.chatRoutes
import app.routes.scanRoutes
import app.routes.stepRoutes
import app.routes.visionRoutes
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private const val VERSION = "v2.1.2"

//private const val PROJECT_ID = "healthguard-b443f"
private fun resolvedProjectId(): String =
    System.getenv("GOOGLE_CLOUD_PROJECT")
        ?: System.getenv("GCLOUD_PROJECT")
        ?: System.getenv("GCP_PROJECT")
        ?: "unknown"

fun main() {
    initFirebase()

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(
        factory = Netty,
        port = port,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}


fun initFirebase() {
    try {
        if (FirebaseApp.getApps().isEmpty()) {

            val credentials = GoogleCredentials.getApplicationDefault()

            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId("healthguard-b443f")   // 🔥 ΤΟ ΣΗΜΑΝΤΙΚΟ
                .build()

            val app = FirebaseApp.initializeApp(options)

            println("🔥 Firebase projectId from options: ${app.options.projectId}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}




fun Application.module() {

    install(ContentNegotiation) {
        jackson {
            enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
            registerKotlinModule()
        }
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("status" to "error", "message" to (cause.message ?: "Bad request"))
            )
        }

        exception<Throwable> { call, cause ->
            println("UNHANDLED ERROR: ${cause.message}")
            cause.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("status" to "error", "message" to "Internal server error")
            )
        }
    }

    routing {
        get("/") {
            call.respond(mapOf("message" to "HealthGuard API is Live", "version" to VERSION))
        }

        get("/health") {
            call.respond(mapOf("status" to "healthy-$VERSION"))
        }
        get("/version") {
            call.respond(
                mapOf(
                    "version" to VERSION,
                    "projectId" to resolvedProjectId()
                )
            )
        }


        route("/api") {
            scanRoutes()
            visionRoutes()
            chatRoutes()
            stepRoutes()

            route("/meds") {
                get("/search") {
                    val query = call.parameters["q"] ?: ""
                    call.respond(
                        mapOf(
                            "query" to query,
                            "results" to listOf(
                                mapOf(
                                    "brand" to query,
                                    "generic" to "N/A",
                                    "summary" to "Πληροφορίες για $query"
                                )
                            )
                        )
                    )
                }
            }
        }

    }
}