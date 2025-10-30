package com.example.healthguard.presentation.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.network.dto.MedRecord
import com.example.healthguard.data.remote.MedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MedicineViewModel(
    private val repo: MedicineRepository = MedicineRepository()
) : ViewModel() {

    private val _results = MutableStateFlow<List<MedRecord>>(emptyList())
    val results: StateFlow<List<MedRecord>> = _results

    fun search(query: String, lang: String? = null) {
        viewModelScope.launch {
            try {
                _results.value = repo.searchMedicines(query, lang)
            } catch (e: Exception) {
                e.printStackTrace()
                _results.value = emptyList()
            }
        }
    }
}
