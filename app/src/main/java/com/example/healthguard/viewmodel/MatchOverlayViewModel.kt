package com.example.healthguard.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.network.dto.ChatSource
import com.example.healthguard.data.network.dto.MedRecord
import com.example.healthguard.data.repo.MedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MatchOverlayViewModelFactory(private val repository: MedicineRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchOverlayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MatchOverlayViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
class MatchOverlayViewModel(
    private val repository: MedicineRepository
) : ViewModel() {

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible

    private val _payload = MutableStateFlow<BubblePayload?>(null)
    val payload: StateFlow<BubblePayload?> = _payload

    private val _position = MutableStateFlow(Offset.Zero)
    val position: StateFlow<Offset> = _position

    fun saveToLibrary(name: String, info: String) {
        viewModelScope.launch {
            val record = MedRecord(
                brand = name,
                summary = info,
                source = "Camera Search"
            )

            repository.saveToCabinet(record) { success ->
                if (success) {
                    hide() // Reference is now resolved below
                }
            }
        }
    }

    fun hide() {
        _visible.value = false
    }

    fun showFromChatResult(sessionId: String, title: String, sources: List<ChatSource>) {
        _payload.value = BubblePayload(sessionId, title, sources)
        _visible.value = true
    }

    fun setPosition(offset: Offset) {
        _position.value = offset
    }
}

data class BubblePayload(
    val sessionId: String,
    val title: String,
    val sources: List<ChatSource> = emptyList()
)