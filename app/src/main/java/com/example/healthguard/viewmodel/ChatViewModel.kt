package com.example.healthguard.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.network.dto.ChatSource
import com.example.healthguard.data.repo.ChatRepository
import com.example.healthguard.data.repo.ChatSessionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ChatMessage(
    val id: String,
    val text: String,
    val fromUser: Boolean,
    val sources: List<ChatSource> = emptyList()
)

data class QuickAction(
    val id: String,
    val title: String,
    val message: String
)

class ChatViewModel : ViewModel() {

    private val chatRepo = ChatRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _quickActions = MutableStateFlow<List<QuickAction>>(emptyList())
    val quickActions: StateFlow<List<QuickAction>> = _quickActions.asStateFlow()

    private val _chatLanguage = MutableStateFlow("en")
    val chatLanguage: StateFlow<String> = _chatLanguage.asStateFlow()

    // Tracks whether the user has manually toggled the language in THIS session.
    // Reset to false on clearChat() so the next session follows the app language again.
    private var languageManuallySet = false

    // Holds the latest app-level language so clearChat() can restore to it correctly.
    private var appLanguageSnapshot = "en"

    private var currentSessionId: String? = null
    private var currentMedicineName: String? = null
    private var currentSubstance: String? = null
    private var currentStrength: String? = null

    /**
     * Called ONCE from MyApp when the app-level language is first known,
     * and again only when the user changes the app language from the Settings screen.
     * It must NOT be called repeatedly (e.g. from a LaunchedEffect that runs on every
     * recomposition) because that would fight against the in-chat toggle.
     *
     * Safe to call multiple times with the same value — no-ops if already set correctly.
     */
    fun syncAppLanguage(appLang: String) {
        appLanguageSnapshot = appLang
        // Only push to chatLanguage if the user hasn't manually overridden it this session
        if (!languageManuallySet) {
            _chatLanguage.value = appLang
        }
    }

    /**
     * Called only by the in-chat language toggle button.
     * Marks the language as manually set so syncAppLanguage won't interfere.
     */
    fun setChatLanguage(lang: String) {
        languageManuallySet = true
        _chatLanguage.value = lang
    }

    fun processScannedImage(imageFile: File) {
        viewModelScope.launch {
            try {
                _isSending.value = true
                when (val result = chatRepo.processMedicineImage(imageFile, _chatLanguage.value)) {
                    is ChatSessionResult.Success -> {
                        currentSessionId = result.sessionId
                        currentMedicineName = result.ocrResult.brand
                        currentSubstance = result.ocrResult.activeSubstance
                        currentStrength = result.ocrResult.strength
                        _messages.value = listOf(
                            ChatMessage(UUID.randomUUID().toString(), result.reply, false, result.sources)
                        )
                        _quickActions.value = result.quickActions.mapNotNull { m ->
                            val id = m["id"] ?: return@mapNotNull null
                            val title = m["title"] ?: return@mapNotNull null
                            val message = m["message"] ?: return@mapNotNull null
                            QuickAction(id, title, message)
                        }
                    }
                    is ChatSessionResult.Error -> addErrorMessage(result.message)
                }
            } catch (e: Exception) {
                addErrorMessage("chat_error_image")
            } finally {
                _isSending.value = false
            }
        }
    }

    fun send(messageText: String) {
        if (messageText.isBlank() || _isSending.value) return
        val sessionIdToSend = currentSessionId

        viewModelScope.launch {
            try {
                _isSending.value = true
                val userMsg = ChatMessage(UUID.randomUUID().toString(), messageText, true)
                _messages.value += userMsg

                val result = chatRepo.sendMessage(
                    sessionId = sessionIdToSend,
                    message = messageText,
                    medicineContext = currentMedicineName,
                    activeSubstance = currentSubstance,
                    strength = currentStrength,
                    language = _chatLanguage.value
                )

                result.onSuccess { response ->
                    currentSessionId = response.sessionId
                    val botMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = response.reply,
                        fromUser = false,
                        sources = extractSourcesFromMetadata(response.metadata)
                    )
                    _messages.value += botMsg
                    _quickActions.value = extractQuickActions(response.metadata)
                }.onFailure {
                    addErrorMessage("chat_error_connection")
                }
            } catch (e: Exception) {
                addErrorMessage("chat_error_system")
            } finally {
                _isSending.value = false
            }
        }
    }

    fun startWithSession(sessionId: String?, title: String?) {
        if (sessionId == null || sessionId == currentSessionId) return
        currentSessionId = sessionId
        currentMedicineName = title
        _messages.value = emptyList()
        _quickActions.value = emptyList()
    }

    fun clearChat() {
        // Reset manual flag so the next session follows the app language again
        languageManuallySet = false
        // Restore to whatever the app language currently is
        _chatLanguage.value = appLanguageSnapshot
        currentSessionId = null
        currentMedicineName = null
        currentSubstance = null
        currentStrength = null
        _messages.value = emptyList()
        _quickActions.value = emptyList()
        _isSending.value = false
    }

    fun processScannedImageAndSave(imageFile: File, context: Context) {
        viewModelScope.launch {
            try {
                _isSending.value = true
                when (val result = chatRepo.processMedicineImage(imageFile, _chatLanguage.value)) {
                    is ChatSessionResult.Success -> {
                        Log.d("MEDICINE_SAVE", "brand=${result.ocrResult.brand}, strength=${result.ocrResult.strength}")
                        currentSessionId = result.sessionId
                        currentMedicineName = result.ocrResult.brand
                        currentSubstance = result.ocrResult.activeSubstance
                        currentStrength = result.ocrResult.strength

                        val name = result.ocrResult.brand?.lowercase()?.replace(" ", "_") ?: "unknown"
                        val strength = result.ocrResult.strength?.lowercase()?.replace(" ", "") ?: "unknown"
                        saveMedicineImage(context, imageFile, name, strength)

                        _messages.value = listOf(
                            ChatMessage(UUID.randomUUID().toString(), result.reply, false, result.sources)
                        )
                        _quickActions.value = result.quickActions.mapNotNull { m ->
                            val id = m["id"] ?: return@mapNotNull null
                            val title = m["title"] ?: return@mapNotNull null
                            val message = m["message"] ?: return@mapNotNull null
                            QuickAction(id, title, message)
                        }
                    }
                    is ChatSessionResult.Error -> addErrorMessage(result.message)
                }
            } catch (e: Exception) {
                addErrorMessage("chat_error_image")
            } finally {
                _isSending.value = false
            }
        }
    }

    fun startChatFromGallery(medicineName: String, strength: String) {
        clearChat()
        currentMedicineName = medicineName
        currentStrength = strength
        val lang = if (_chatLanguage.value == "el") "Greek" else "English"
        send("Tell me information about $medicineName $strength. Please respond in $lang.")
    }

    private fun addErrorMessage(text: String) {
        _messages.value += ChatMessage(UUID.randomUUID().toString(), text, false)
    }

    private fun extractSourcesFromMetadata(metadata: Map<String, Any?>?): List<ChatSource> {
        val list = metadata?.get("sources") as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            ChatSource(
                title = m["title"]?.toString() ?: "Source",
                url = m["url"]?.toString() ?: ""
            )
        }
    }

    private fun extractQuickActions(metadata: Map<String, Any?>?): List<QuickAction> {
        val list = metadata?.get("quickActions") as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val id = m["id"]?.toString() ?: return@mapNotNull null
            val title = m["title"]?.toString() ?: return@mapNotNull null
            val message = m["message"]?.toString() ?: return@mapNotNull null
            QuickAction(id = id, title = title, message = message)
        }
    }

    private fun saveMedicineImage(context: Context, sourceFile: File, name: String, strength: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
        val medicineDir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "$userId/Medicine"
        )
        if (!medicineDir.exists()) medicineDir.mkdirs()

        val safeName = name.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
        val safeStrength = strength.lowercase().replace(" ", "").replace("/", "-").replace(Regex("[^a-z0-9_\\-]"), "")
        val targetFile = File(medicineDir, "${safeName}_${safeStrength}.jpg")

        if (targetFile.exists()) return
        sourceFile.copyTo(targetFile, overwrite = false)
    }
}