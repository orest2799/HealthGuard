package app.auth

import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

fun ApplicationCall.getFirebaseUidOrNull(): String? {
    val authHeader = request.headers[HttpHeaders.Authorization] ?: return null
    if (!authHeader.startsWith("Bearer ")) return null

    val token = authHeader.removePrefix("Bearer ").trim()
    if (token.isBlank()) return null

    return try {
        val decoded = FirebaseAuth.getInstance().verifyIdToken(token)
        decoded.uid
    } catch (e: Exception) {
        println("AUTH ERROR: ${e.message}")
        null
    }
}
