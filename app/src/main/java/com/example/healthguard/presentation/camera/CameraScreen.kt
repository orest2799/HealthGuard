package com.example.healthguard.presentation.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsIntent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.example.healthguard.data.network.dto.MedRecord
import com.example.healthguard.presentation.utils.Detection
import com.example.healthguard.presentation.utils.YoloV8Detector
import java.io.File
import java.util.concurrent.Executors


private const val TAG = "CameraScreen"
private const val DETECTION_CONFIDENCE_THRESHOLD = 0.65f

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    onImageCaptured: (File) -> Unit // Αυτή η παράμετρος θα καλέσει το chatVm.processScannedImage
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCam by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    // UI State
    var isBusy by remember { mutableStateOf(false) }
    var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }

    // CameraX Setup
    val previewView = remember { PreviewView(context) }
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val detector = remember { YoloV8Detector(context) }
    val analyzerExec = remember { Executors.newSingleThreadExecutor() }

    // Permission handling
    val askCam = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCam = it }
    LaunchedEffect(Unit) { if (!hasCam) askCam.launch(Manifest.permission.CAMERA) }

    LaunchedEffect(hasCam) {
        if (hasCam) {
            try {
                val provider = ProcessCameraProvider.getInstance(context).await()
                provider.unbindAll()

                // Real-time Object Detection (YOLO)
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build().also { ia ->
                        ia.setAnalyzer(analyzerExec) { proxy ->
                            if (!isBusy) {
                                val frame = proxy.toRgbaBitmap()
                                detections = detector.detect(frame, 0.45f, 0.5f).map { d ->
                                    Detection(
                                        RectF(d.boundingBox.left / frame.width, d.boundingBox.top / frame.height,
                                            d.boundingBox.right / frame.width, d.boundingBox.bottom / frame.height),
                                        d.label, d.confidence
                                    )
                                }
                                frame.recycle()
                            }
                            proxy.close()
                        }
                    }
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, analysis)
                preview.surfaceProvider = previewView.surfaceProvider
            } catch (e: Exception) { Log.e("CameraScreen", "Setup failed", e) }
        }
    }

    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (hasCam) {
                // Camera View
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })

                // YOLO Bounding Boxes
                LiveBoxesOverlay(detections)

                // Capture Button
                Button(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                    enabled = !isBusy,
                    onClick = {
                        isBusy = true
                        val tempFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")

                        imageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(tempFile).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(res: ImageCapture.OutputFileResults) {
                                    // ΕΔΩ ΕΙΝΑΙ Η ΚΡΙΣΙΜΗ ΣΥΝΔΕΣΗ:
                                    // Στέλνουμε το αρχείο στο ViewModel και αλλάζουμε οθόνη
                                    onImageCaptured(tempFile)
                                    isBusy = false
                                }
                                override fun onError(e: ImageCaptureException) {
                                    isBusy = false
                                    Log.e("CameraScreen", "Capture failed", e)
                                }
                            }
                        )
                    }
                ) {
                    Text(if (isBusy) "Επεξεργασία..." else "Λήψη Φωτογραφίας")
                }
            }
        }
    }
}
@Composable
private fun ResultCard(
    thumb: Bitmap?,
    ocr: String,
    status: String?,
    meds: List<MedRecord>,
    onOpenUrl: (String) -> Unit,
    onSave: (String, String) -> Unit // New callback for saving
) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                thumb?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(90.dp).padding(end = 12.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Detected Name", fontWeight = FontWeight.Bold)
                    Text(ocr, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    if (!status.isNullOrBlank()) Text(status, color = Color.Gray)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Text("Select Match to Save to Cabinet", fontWeight = FontWeight.Bold)

            meds.take(3).forEach { m ->
                val name = m.brand ?: m.generic ?: "Unknown"
                val info = m.summary ?: "${m.strength ?: ""} ${m.form ?: ""}"

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSave(name, info) } // CLICKING SAVES TO CABINET
                        .padding(vertical = 12.dp)
                ) {
                    Text(name, color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold)
                    Text(info, style = MaterialTheme.typography.bodySmall)
                    Text("Click to add to Cabinet", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun LiveBoxesOverlay(detections: List<Detection>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        detections.forEach { d ->
            val r = d.boundingBox
            drawRect(
                color = Color(0xFF00B0FF),
                topLeft = Offset(r.left * w, r.top * h),
                size = Size(
                    (r.right - r.left) * w,
                    (r.bottom - r.top) * h
                ),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

private fun openCustomTab(context: Context, url: String) {
    runCatching { CustomTabsIntent.Builder().build().launchUrl(context, url.toUri()) }
        .onFailure { e -> Log.e("CameraScreen", "CustomTab error", e) }
}

private fun cropFromNormalized(photo: Bitmap, norm: RectF): Bitmap {
    val left = (norm.left * photo.width).toInt().coerceIn(0, photo.width - 1)
    val top = (norm.top * photo.height).toInt().coerceIn(0, photo.height - 1)
    val right = (norm.right * photo.width).toInt().coerceIn(left + 1, photo.width)
    val bottom = (norm.bottom * photo.height).toInt().coerceIn(top + 1, photo.height)
    return Bitmap.createBitmap(photo, left, top, right - left, bottom - top)
}


private fun ImageProxy.toRgbaBitmap(): Bitmap {
    val plane = planes[0].buffer
    val w = width
    val h = height
    plane.rewind()
    val bitmap = createBitmap(w, h)
    bitmap.copyPixelsFromBuffer(plane)
    return bitmap
}