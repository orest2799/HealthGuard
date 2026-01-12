package app.api

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

object EmaAuth {
    private val http = OkHttpClient()

    /**
     * Load .env ONLY for local dev.
     * In Cloud Run / production, env vars should be provided via:
     * - Cloud Run env vars, or
     * - Secret Manager -> env var
     */
    private val dotenv: Dotenv? = runCatching {
        // Try ./.env (run from backend folder)
        val d1 = dotenv {
            directory = "."
            filename = ".env"
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }

        // If empty, try ./backend/.env (run from repo root)
        if (d1.entries().isEmpty()) {
            dotenv {
                directory = "backend"
                filename = ".env"
                ignoreIfMalformed = true
                ignoreIfMissing = true
            }
        } else {
            d1
        }
    }.getOrNull()

    private fun envOrDotenv(key: String): String? {
        // 1) Prefer real environment (Cloud Run / prod)
        val env = System.getenv(key)
        if (!env.isNullOrBlank()) return env

        // 2) Fallback to .env (local dev)
        val dot = dotenv?.get(key) ?: dotenv?.get(key) // harmless double, keeps it compatible
        val dot2 = dotenv?.get(key) ?: dotenv?.let { it[key] }
        return (dot ?: dot2)?.takeIf { !it.isNullOrBlank() }
    }

    // Read config from ENV first, fallback to .env
    private val clientId: String? get() = envOrDotenv("EMA_CLIENT_ID")
    private val clientSecret: String? get() = envOrDotenv("EMA_CLIENT_SECRET")
    private val tokenUrl: String? get() = envOrDotenv("EMA_TOKEN_URL")
    private val scope: String? get() = envOrDotenv("EMA_SCOPE")

    private var cachedToken: String? = null
    private var tokenExpiration: Long = 0L

    init {
        // Debug (safe, but do NOT print secrets)
        println("PWD (working dir): " + File(".").absolutePath)
        println(".env exists here? " + File(".env").exists())
        println("backend/.env exists? " + File("backend/.env").exists())
        println("EMA_CLIENT_ID present? " + (!clientId.isNullOrBlank()))
        println("EMA_TOKEN_URL present? " + (!tokenUrl.isNullOrBlank()))
        println("EMA_SCOPE present? " + (!scope.isNullOrBlank()))
        // never print clientSecret
    }

    suspend fun getToken(): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // Return cached token if still valid
        if (cachedToken != null && now < tokenExpiration) {
            return@withContext cachedToken
        }

        val cid = clientId
        val csec = clientSecret
        val turl = tokenUrl
        val scp = scope

        if (cid.isNullOrBlank() || csec.isNullOrBlank() || turl.isNullOrBlank() || scp.isNullOrBlank()) {
            println("⚠️ EMA credentials/config missing — skipping EMA search.")
            println("   EMA_CLIENT_ID set? ${!cid.isNullOrBlank()}")
            println("   EMA_CLIENT_SECRET set? ${!csec.isNullOrBlank()}")
            println("   EMA_TOKEN_URL set? ${!turl.isNullOrBlank()}")
            println("   EMA_SCOPE set? ${!scp.isNullOrBlank()}")
            return@withContext null
        }

        val form = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", cid)
            .add("client_secret", csec)
            .add("scope", scp)
            .build()

        val request = Request.Builder()
            .url(turl)
            .post(form)
            .build()

        try {
            http.newCall(request).execute().use { resp ->
                val body = resp.body?.string()
                println("EMA TOKEN HTTP ${resp.code}")

                if (!resp.isSuccessful || body.isNullOrBlank()) {
                    println("⚠️ EMA token request failed: HTTP ${resp.code}")
                    return@use null
                }

                val json = JSONObject(body)
                val token = json.optString("access_token", null)
                val expiresIn = json.optLong("expires_in", 3600L)

                if (token.isNullOrBlank()) {
                    println("⚠️ EMA token response missing access_token")
                    return@use null
                }

                cachedToken = token
                tokenExpiration = now + (expiresIn * 1000)
                token
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
