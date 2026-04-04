package com.example.healthguard.viewmodel



import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.healthguard.data.repo.StepRepository

class StepViewModelFactory(
    private val repository: StepRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == StepViewModel::class.java) {
            "StepViewModelFactory can only create StepViewModel. Requested: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return StepViewModel(repository) as T
    }
}
