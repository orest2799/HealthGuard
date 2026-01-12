package app.chat

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.chatRoutes() {
    route("/chat") {

        // TEST GET endpoint (works)
        get("/medicine/test") {
            println(">>> HIT /chat/medicine/test (GET)")
            call.respondText(
                """{"status":"test-ok"}""",
                ContentType.Application.Json,
                HttpStatusCode.OK
            )
        }

        // TEST POST endpoint – should now work 100%
        post("/medicine/test") {
            val raw = call.receiveText()
            println(">>> HIT /chat/medicine/test (POST), body = '$raw'")

            val responseJson = """
            {
              "status": "test-post-ok",
              "receivedBody": "$raw"
            }
            """.trimIndent()

            call.respondText(
                responseJson,
                ContentType.Application.Json,
                HttpStatusCode.OK
            )
        }
    }
}
