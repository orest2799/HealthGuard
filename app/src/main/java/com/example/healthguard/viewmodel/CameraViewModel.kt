package com.example.healthguard.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.healthguard.viewmodel.utils.Detection
import com.example.healthguard.viewmodel.utils.YoloV8Detector

class CameraViewModel : ViewModel() {

    private var detector: YoloV8Detector? = null
    private var isDestroyed = false
    var isBusy by mutableStateOf(false)

    var detections by mutableStateOf<List<Detection>>(emptyList())
        private set

    var lensFacing by mutableStateOf(androidx.camera.core.CameraSelector.LENS_FACING_BACK)
        private set

    fun initDetector(context: Context) {
        if (detector == null) {
            detector = YoloV8Detector(context)
        }
    }
    private val detectorLock = Any()

    fun runDetection(bitmap: Bitmap) {
        if (isBusy) return
        synchronized(detectorLock) {
            if (isDestroyed) {
                bitmap.recycle()
                return
            }
            val result = detector?.detect(bitmap, 0.7f, 0.5f) ?: run {
                bitmap.recycle()
                return
            }
            detections = result
            bitmap.recycle()
        }
    }

    override fun onCleared() {
        super.onCleared()
        synchronized(detectorLock) {
            isDestroyed = true
            detector?.close()
            detector = null
        }
    }


    fun flipCamera() {
        lensFacing = if (lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK) {
            androidx.camera.core.CameraSelector.LENS_FACING_FRONT
        } else {
            androidx.camera.core.CameraSelector.LENS_FACING_BACK
        }
        detections = emptyList()
    }


}