package app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.dbRoutes() {

    post("/api/ocr/save") {
        val raw = call.receiveText()
        println(">>> HIT /api/ocr/save, body = $raw")

        call.respondText(
            """{"id":"test-ocr-id","echo":$raw}""",
            ContentType.Application.Json,
            HttpStatusCode.OK
        )
    }

    post("/api/meds/save") {
        val raw = call.receiveText()
        println(">>> HIT /api/meds/save, body = $raw")

        call.respondText(
            """{"id":"test-meds-id","echo":$raw}""",
            ContentType.Application.Json,
            HttpStatusCode.OK
        )
    }
}
