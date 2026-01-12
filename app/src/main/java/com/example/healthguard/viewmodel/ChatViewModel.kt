package com.example.healthguard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.network.dto.ChatSource
import com.example.healthguard.data.repo.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ChatViewModel for the existing ChatScreen.kt
 * Matches the interface expected by ChatScreen (messages, isSending, send(), startWithSession())
 */
class ChatViewModel : ViewModel() {

    private val chatRepo = ChatRepository()

    // State flows that ChatScreen expects
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Internal state
    private var currentSessionId: String? = null

    companion object {
        private const val TAG = "ChatViewModel"
    }

    /**
     * Start with an existing session (called from ChatScreen when navigating from bubble)
     */
    fun startWithSession(sessionId: String?, title: String?) {
        if (sessionId == null) return

        currentSessionId = sessionId
        Log.d(TAG, "Started with session: $sessionId, title: $title")

        // In a real implementation, you might load message history here
        // For now, just set the session ID
    }

    /**
     * Send a message in the current chat session
     * This is called when user clicks "Send" button
     */
    fun send(messageText: String) {
        if (messageText.isBlank()) return

        val sessionId = currentSessionId
        if (sessionId == null) {
            Log.e(TAG, "Cannot send message: no active session")
            // Start a new session if none exists
            startNewChatWithMessage(messageText)
            return
        }

        viewModelScope.launch {
            try {
                // Add user message immediately to UI
                val userMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = messageText,
                    fromUser = true,
                    sources = emptyList()
                )
                _messages.value += userMessage

                // Show sending indicator
                _isSending.value = true

                Log.d(TAG, "Sending message to session $sessionId: $messageText")

                // Send to backend
                chatRepo.sendMessage(sessionId, messageText)
                    .onSuccess { response ->
                        Log.d(TAG, "Response received: ${response.reply.take(50)}...")

                        // Extract sources from metadata
                        val sources = extractSources(response.metadata)

                        // Add AI response to messages
                        val aiMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = response.reply,
                            fromUser = false,
                            sources = sources
                        )
                        _messages.value += aiMessage

                        // Hide sending indicator
                        _isSending.value = false
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Error sending message", error)

                        // Add error message
                        val errorMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = "Σφάλμα: ${error.message}",
                            fromUser = false,
                            sources = emptyList()
                        )
                        _messages.value += errorMessage

                        _isSending.value = false
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Exception sending message", e)

                // Add error message
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Σφάλμα: ${e.message}",
                    fromUser = false,
                    sources = emptyList()
                )
                _messages.value += errorMessage

                _isSending.value = false
            }
        }
    }

    /**
     * Start a new chat with a medicine name (when no session exists yet)
     */
    private fun startNewChatWithMessage(message: String) {
        viewModelScope.launch {
            try {
                _isSending.value = true

                Log.d(TAG, "Starting new chat with message: $message")

                // Start new chat
                chatRepo.startChatWithMedicine(message)
                    .onSuccess { response ->
                        currentSessionId = response.sessionId

                        Log.d(TAG, "New chat started: ${response.sessionId}")

                        // Extract sources
                        val sources = extractSources(response.metadata)

                        // Add user message
                        val userMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = message,
                            fromUser = true,
                            sources = emptyList()
                        )

                        // Add AI response
                        val aiMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = response.reply,
                            fromUser = false,
                            sources = sources
                        )

                        _messages.value = listOf(userMessage, aiMessage)
                        _isSending.value = false
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Error starting new chat", error)

                        val errorMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = "Αποτυχία έναρξης συνομιλίας: ${error.message}",
                            fromUser = false,
                            sources = emptyList()
                        )
                        _messages.value = listOf(errorMessage)
                        _isSending.value = false
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Exception starting new chat", e)

                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Σφάλμα: ${e.message}",
                    fromUser = false,
                    sources = emptyList()
                )
                _messages.value = listOf(errorMessage)
                _isSending.value = false
            }
        }
    }

    /**
     * Extract sources from metadata
     */
    private fun extractSources(metadata: Map<String, Any?>?): List<ChatSource> {
        val sourcesList = metadata?.get("sources") as? List<*> ?: return emptyList()

        return sourcesList.mapNotNull { source ->
            if (source is Map<*, *>) {
                val title = source["title"] as? String ?: return@mapNotNull null
                val url = source["url"] as? String ?: return@mapNotNull null
                ChatSource(title = title, url = url)
            } else {
                null
            }
        }
    }

    /**
     * Clear the chat (optional - can be used for "new chat" button)
     */
    fun clearChat() {
        currentSessionId = null
        _messages.value = emptyList()
        _isSending.value = false
        Log.d(TAG, "Chat cleared")
    }
}

/**
 * Message data class that matches what ChatScreen expects
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val fromUser: Boolean,
    val sources: List<ChatSource> = emptyList()
)