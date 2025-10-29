package com.example.healthguard.presentation.chat

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.example.healthguard.data.network.dto.AnalyzeOcrResponse
import com.example.healthguard.data.network.dto.SourceRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BubblePayload(
    val sessionId: String,
    val title: String,
    val sources: List<SourceRef> = emptyList()
)

class MatchOverlayViewModel : ViewModel() {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible

    private val _payload = MutableStateFlow<BubblePayload?>(null)
    val payload: StateFlow<BubblePayload?> = _payload

    private val _position = MutableStateFlow(Offset.Zero)
    val position: StateFlow<Offset> = _position

    fun showFromAnalyzeResponse(res: AnalyzeOcrResponse) {
        val title = res.medicines.firstOrNull()?.name ?: (res.sources.firstOrNull()?.name ?: "Φάρμακο")
        _payload.value = BubblePayload(res.sessionId, title, res.sources)
        _visible.value = true
    }

    fun hide() { _visible.value = false }
    fun setPosition(offset: Offset) { _position.value = offset }
}
