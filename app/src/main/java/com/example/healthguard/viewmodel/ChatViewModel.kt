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


    private var currentSessionId: String? = null
    private var currentMedicineName: String? = null
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
                        currentMedicineName = result.ocrResult.brand
                        currentSubstance = result.ocrResult.activeSubstance
                        currentStrength = result.ocrResult.strength

                        _messages.value = listOf(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                text = result.reply,
                                fromUser = false,
                                sources = result.sources
                            )
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
                addErrorMessage("Παρουσιάστηκε σφάλμα κατά την επεξεργασία της εικόνας.")
            } finally {
                _isSending.value = false
            }
        }
    }

    fun send(messageText: String) {
        if (messageText.isBlank() || _isSending.value) return


        val sessionIdToSend: String? = currentSessionId

        viewModelScope.launch {
            try {
                _isSending.value = true


                val userMsg = ChatMessage(UUID.randomUUID().toString(), messageText, true)
                _messages.value = _messages.value + userMsg

                val result = chatRepo.sendMessage(
                    sessionId = sessionIdToSend,
                    message = messageText,
                    medicineContext = currentMedicineName,
                    activeSubstance = currentSubstance,
                    strength = currentStrength
                )

                result.onSuccess { response ->
                    currentSessionId = response.sessionId

                    val botMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = response.reply,
                        fromUser = false,
                        sources = extractSourcesFromMetadata(response.metadata)
                    )
                    _messages.value = _messages.value + botMsg

                    // Backend-driven quick actions
                    _quickActions.value = extractQuickActions(response.metadata)
                }.onFailure {
                    addErrorMessage("Αδυναμία σύνδεσης με τον διακομιστή HealthGuard.")
                }
            } catch (e: Exception) {
                addErrorMessage("Σφάλμα συστήματος.")
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
        currentSessionId = null
        currentMedicineName = null
        currentSubstance = null
        currentStrength = null
        _messages.value = emptyList()
        _quickActions.value = emptyList()
        _isSending.value = false
    }

    private fun addErrorMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(UUID.randomUUID().toString(), text, false)
    }

    private fun extractSourcesFromMetadata(metadata: Map<String, Any?>?): List<ChatSource> {
        val list = metadata?.get("sources") as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            ChatSource(
                title = m["title"]?.toString() ?: "Πηγή",
                url = m["url"]?.toString() ?: ""
            )
        }
    }
    fun processScannedImageAndSave(imageFile: File, context: Context) {
        viewModelScope.launch {
            try {
                _isSending.value = true
                val result = chatRepo.processMedicineImage(imageFile)

                when (result) {
                    is ChatSessionResult.Success -> {
                        Log.d("MEDICINE_SAVE", "brand=${result.ocrResult.brand}, strength=${result.ocrResult.strength}")
                        currentSessionId = result.sessionId
                        currentMedicineName = result.ocrResult.brand
                        currentSubstance = result.ocrResult.activeSubstance
                        currentStrength = result.ocrResult.strength

                        // Save crop AFTER we have the medicine name
                        val name = result.ocrResult.brand?.lowercase()
                            ?.replace(" ", "_") ?: "unknown"
                        val strength = result.ocrResult.strength?.lowercase()
                            ?.replace(" ", "") ?: "unknown"
                        saveMedicineImage(context, imageFile, name, strength)

                        _messages.value = listOf(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                text = result.reply,
                                fromUser = false,
                                sources = result.sources
                            )
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
                addErrorMessage("Παρουσιάστηκε σφάλμα κατά την επεξεργασία της εικόνας.")
            } finally {
                _isSending.value = false
            }
        }
    }

    fun startChatFromGallery(medicineName: String, strength: String) {
        clearChat()
        currentMedicineName = medicineName
        currentStrength = strength
        // Send a pre-built first message
        send("Πες μου πληροφορίες για το $medicineName $strength")
    }

    private fun saveMedicineImage(context: Context, sourceFile: File, name: String, strength: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance()
            .currentUser?.uid ?: "unknown"
        val medicineDir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "$userId/Medicine"
        )
        if (!medicineDir.exists()) medicineDir.mkdirs()

        // Sanitize: remove or replace characters invalid in filenames
        val safeName = name.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")

        val safeStrength = strength.lowercase()
            .replace(" ", "")
            .replace("/", "-")          // 875/125 mg → 875-125mg
            .replace(Regex("[^a-z0-9_\\-]"), "")

        val targetFile = File(medicineDir, "${safeName}_${safeStrength}.jpg")

        if (targetFile.exists()) return  // no duplicates

        sourceFile.copyTo(targetFile, overwrite = false)
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
}
