package app.util

import io.github.cdimascio.dotenv.Dotenv

object Env {
    private val dotenv: Dotenv = Dotenv.configure()
        .ignoreIfMissing() // so it doesn't crash if .env is missing
        .load()

    val port: Int by lazy {
        System.getenv("PORT")?.toIntOrNull()
            ?: dotenv["PORT"]?.toIntOrNull()
            ?: 8080
    }

    val geminiApiKey: String? by lazy {
        System.getenv("GEMINI_API_KEY")
            ?: dotenv["GEMINI_API_KEY"]
    }
}
