package app.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.ocrRoutes() {
    route("/meds") {
        post("/parse-ocr") {
            try {
                // read body as raw text (no JSON parsing yet)
                val bodyText = call.receiveText()

                // send it back so we see exactly what arrived
                call.respond(
                    mapOf(
                        "received" to bodyText
                    )
                )
            } catch (t: Throwable) {
                t.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "error" to "Server error in /meds/parse-ocr",
                        "message" to (t.message ?: "no message"),
                        "exception" to t::class.qualifiedName
                    )
                )
            }
        }
    }
}
