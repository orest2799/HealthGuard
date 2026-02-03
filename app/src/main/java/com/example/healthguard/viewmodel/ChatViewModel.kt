package com.example.healthguard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.models.MedicineOcrResult
import com.example.healthguard.data.network.dto.ChatSource
import com.example.healthguard.data.repo.ChatRepository
import com.example.healthguard.data.repo.ChatSessionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ChatViewModel : ViewModel() {

    private val chatRepo = ChatRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var currentSessionId: String? = null
    private var currentMedicineName: String? = null

    // Προσθήκη των μεταβλητών που έλειπαν ή προκαλούσαν σφάλματα
    private var isInitialized = false
    private var currentSubstance: String? = null
    private var currentStrength: String? = null

    fun processScannedImage(imageFile: File) {
        viewModelScope.launch {
            try {
                _isSending.value = true
                val result = chatRepo.processMedicineImage(imageFile)

                when (result) {
                    is ChatSessionResult.Success -> {
                        currentSessionId = result.sessionId
                        currentMedicineName = result.extractedText

                        // Λύση: Παίρνουμε τα δεδομένα απευθείας από το ocrResult
                        // και όχι από το summaryText για να αποφύγουμε σφάλματα στο substring
                        currentSubstance = result.ocrResult.activeSubstance
                        currentStrength = result.ocrResult.strength

                        val aiMsg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = result.reply,
                            fromUser = false,
                            sources = result.sources
                        )
                        _messages.value = listOf(aiMsg)
                        isInitialized = true
                    }

                    is ChatSessionResult.Error -> {
                        _messages.value = listOf(
                            ChatMessage(UUID.randomUUID().toString(), result.message, false)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "OCR Crash", e)
                _messages.value = listOf(
                    ChatMessage(UUID.randomUUID().toString(), "Παρουσιάστηκε σφάλμα συστήματος.", false)
                )
            } finally {
                _isSending.value = false
            }
        }
    }

    fun startWithSession(sessionId: String?, title: String?) {
        if (sessionId == "new") return

        if (sessionId != null && sessionId != currentSessionId) {
            currentSessionId = sessionId
            currentMedicineName = title
            _messages.value = emptyList()
            isInitialized = true
        }
    }

    fun send(messageText: String) {
        if (messageText.isBlank()) return

        val sessionId = currentSessionId
        if (sessionId == null) {
            currentMedicineName = messageText
            startNewChatWithMessage(messageText)
            return
        }

        viewModelScope.launch {
            try {
                _isSending.value = true
                updateMessages(ChatMessage(UUID.randomUUID().toString(), messageText, true))

                // Χρήση Safe Calls (?.) για την αποστολή στο Repo
                val result = chatRepo.sendMessage(
                    sessionId = sessionId,
                    message = messageText,
                    medicineContext = currentMedicineName,
                    activeSubstance = currentSubstance, // Επιτρέπεται ως nullable String?
                    strength = currentStrength          // Επιτρέπεται ως nullable String?
                )

                result.onSuccess { response ->
                    val aiMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = response.reply ?: "Δεν βρέθηκε απάντηση.",
                        fromUser = false,
                        sources = extractSources(response.metadata)
                    )
                    updateMessages(aiMsg)
                }.onFailure { error ->
                    updateMessages(ChatMessage(UUID.randomUUID().toString(), "Σφάλμα: ${error.localizedMessage}", false))
                }
            } finally {
                _isSending.value = false
            }
        }
    }

    private fun startNewChatWithMessage(message: String) {
        viewModelScope.launch {
            try {
                _isSending.value = true
                val dummyOcr = MedicineOcrResult(brand = message, activeSubstance = "Άγνωστο", strength = "", form = "")

                chatRepo.startChatWithMedicine(
                    ocrResult = dummyOcr,
                    medicineName = message,
                    userQuestion = message
                ).onSuccess { response ->
                    currentSessionId = response.sessionId
                    val aiMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = response.reply ?: "Γεια! Πώς μπορώ να βοηθήσω;",
                        fromUser = false,
                        sources = extractSources(response.metadata)
                    )
                    _messages.value = listOf(aiMsg)
                }.onFailure { error ->
                    updateMessages(ChatMessage(UUID.randomUUID().toString(), "Σφάλμα: ${error.message}", false))
                }
            } finally {
                _isSending.value = false
            }
        }
    }

    private fun updateMessages(newMessage: ChatMessage) {
        _messages.value = _messages.value.toMutableList().apply { add(newMessage) }
    }

    private fun extractSources(metadata: Map<String, Any?>?): List<ChatSource> {
        val sourcesList = metadata?.get("sources") as? List<*> ?: return emptyList()
        return sourcesList.mapNotNull { source ->
            if (source is Map<*, *>) {
                val title = source["title"] as? String ?: return@mapNotNull null
                val url = source["url"] as? String ?: return@mapNotNull null
                ChatSource(title = title, url = url)
            } else null
        }
    }

    fun clearChat() {
        currentSessionId = null
        currentMedicineName = null
        currentSubstance = null
        currentStrength = null
        isInitialized = false
        _messages.value = emptyList()
        _isSending.value = false
    }
}

data class ChatMessage(
    val id: String,
    val text: String,
    val fromUser: Boolean,
    val sources: List<ChatSource> = emptyList()
)