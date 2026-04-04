package app.services



import java.util.concurrent.ConcurrentHashMap

object ChatMemory {
    data class Turn(val role: String, val text: String)

    private const val MAX_TURNS = 12 // keep last N messages (user+assistant)

    private val sessions = ConcurrentHashMap<String, MutableList<Turn>>()

    fun get(sessionId: String): List<Turn> =
        sessions[sessionId]?.toList() ?: emptyList()

    fun appendUser(sessionId: String, text: String) {
        append(sessionId, Turn("user", text))
    }

    fun appendAssistant(sessionId: String, text: String) {
        append(sessionId, Turn("assistant", text))
    }

    private fun append(sessionId: String, turn: Turn) {
        val list = sessions.computeIfAbsent(sessionId) { mutableListOf() }
        list.add(turn)
        // trim old turns
        if (list.size > MAX_TURNS) {
            val extra = list.size - MAX_TURNS
            repeat(extra) { if (list.isNotEmpty()) list.removeAt(0) }
        }
    }

    fun clear(sessionId: String) {
        sessions.remove(sessionId)
    }
}
