package com.example.healthguard.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.example.healthguard.data.network.dto.ChatSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BubblePayload(
    val sessionId: String,
    val title: String,
    val sources: List<ChatSource> = emptyList()  // Changed from SourceRef to ChatSource
)

class MatchOverlayViewModel : ViewModel() {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible

    private val _payload = MutableStateFlow<BubblePayload?>(null)
    val payload: StateFlow<BubblePayload?> = _payload

    private val _position = MutableStateFlow(Offset.Zero)
    val position: StateFlow<Offset> = _position

    /**
     * NEW: Show bubble from ChatSessionResult
     * This replaces the old showFromAnalyzeResponse method
     */
    fun showFromChatResult(
        sessionId: String,
        title: String,
        sources: List<ChatSource>
    ) {
        _payload.value = BubblePayload(sessionId, title, sources)
        _visible.value = true
    }

    fun hide() {
        _visible.value = false
    }

    fun setPosition(offset: Offset) {
        _position.value = offset
    }
}