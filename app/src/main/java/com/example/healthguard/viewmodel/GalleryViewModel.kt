package com.example.healthguard.viewmodel

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class GalleryState(
    val images: Map<String, List<File>> = emptyMap(),
    val isLoading: Boolean = false
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(GalleryState(isLoading = true))
    val state: StateFlow<GalleryState> = _state.asStateFlow()

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true)
            val images = getGroupedImages(getApplication())
            _state.value = GalleryState(images = images, isLoading = false)
        }
    }

    fun deleteImage(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            file.delete()
            loadImages()
        }
    }

    private fun getGroupedImages(context: Context): Map<String, List<File>> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

        // Must match EXACTLY the path used in ChatViewModel.saveMedicineImage()
        val rootDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            userId
        )

        val medicineDir = File(rootDir, "Medicine")
        val photosDir   = File(rootDir, "Photos")

        // Debug — check in Logcat with tag "GalleryVM"
        Log.d("GalleryVM", "Root path: ${rootDir.absolutePath}")
        Log.d("GalleryVM", "Medicine dir exists: ${medicineDir.exists()}")
        Log.d("GalleryVM", "Medicine files: ${medicineDir.listFiles()?.map { it.name } ?: "null"}")

        val medicines = medicineDir
            .listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        val photos = photosDir
            .listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        Log.d("GalleryVM", "Found ${medicines.size} medicines, ${photos.size} photos")

        return buildMap {
            if (medicines.isNotEmpty()) put("Medicine", medicines)
            if (photos.isNotEmpty()) put("Photos", photos)
        }
    }
}