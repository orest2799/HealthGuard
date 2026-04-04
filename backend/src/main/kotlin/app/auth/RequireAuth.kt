package app.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

suspend fun ApplicationCall.requireUid(): String? {
    val uid = getFirebaseUidOrNull()
    if (uid == null) {
        respond(
            HttpStatusCode.Unauthorized,
            mapOf("status" to "error", "message" to "missing/invalid token")
        )
        return null
    }
    return uid
}
