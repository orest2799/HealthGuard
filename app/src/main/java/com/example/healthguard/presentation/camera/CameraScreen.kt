package com.example.healthguard.presentation.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.example.healthguard.data.network.dto.MedRecord
import com.example.healthguard.data.network.dto.VisionDto
import com.example.healthguard.data.remote.MedicineRepository
import com.example.healthguard.data.remote.VisionRepository
import com.example.healthguard.data.repo.ChatRepository
import com.example.healthguard.data.repo.ChatSessionResult
import com.example.healthguard.presentation.chat.MatchOverlayViewModel
import com.example.healthguard.presentation.utils.Detection
import com.example.healthguard.presentation.utils.YoloV8Detector
import com.example.healthguard.presentation.utils.decodeAndFixOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/* ------------------------------------------------------------- *
 * Camera screen (scrollable; app-only saves; conditional folder)
 * FIXED: Now uses ChatRepository.processMedicineImage() instead of analyzeOcr
 * FIXED: Crash prevention when detector closes during navigation
 * ------------------------------------------------------------- */
@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    overlayVm: MatchOverlayViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val pageScroll = rememberScrollState()

    // Permissions
    var hasCam by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val askCam = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCam = granted }

    LaunchedEffect(Unit) { if (!hasCam) askCam.launch(Manifest.permission.CAMERA) }

    // Detector + analyzer executor
    val detector = remember { YoloV8Detector(context) }
    val analyzerExec = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
            analyzerExec.shutdown()
        }
    }

    // State
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var best by remember { mutableStateOf<Detection?>(null) }
    var conf by remember { mutableFloatStateOf(0.45f) }

    // Results
    var thumb by remember { mutableStateOf<Bitmap?>(null) }
    var cropPreview by remember { mutableStateOf<Bitmap?>(null) }
    var ocrText by remember { mutableStateOf<String?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    // Search results
    var medResults by remember { mutableStateOf<List<MedRecord>>(emptyList()) }
    var medError by remember { mutableStateOf<String?>(null) }

    // repositories
    val visionRepo = remember { VisionRepository() }
    val medsRepo = remember { MedicineRepository() }
    val chatRepo = remember { ChatRepository() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
                .verticalScroll(pageScroll)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (!hasCam) {
                    Text("Camera permission is required.")
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                            }
                        },
                        update = { pv ->
                            scope.launch {
                                val provider = context.getCameraProvider()
                                provider.unbindAll()

                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = pv.surfaceProvider
                                }

                                imageCapture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()

                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                    .build()
                                    .also { ia ->
                                        ia.setAnalyzer(analyzerExec) { proxy ->
                                            try {
                                                // ✅ Check if busy before processing
                                                if (isBusy) {
                                                    proxy.close()
                                                    return@setAnalyzer
                                                }

                                                val frame = proxy.toBitmap()

                                                // ✅ Safely detect with try-catch for closed detector
                                                val raw = try {
                                                    detector.detect(frame, conf, 0.5f)
                                                } catch (e: IllegalStateException) {
                                                    // Detector was closed (navigated away from screen)
                                                    Log.d("CameraScreen", "Detector closed, stopping analysis")
                                                    proxy.close()
                                                    return@setAnalyzer
                                                }

                                                val norm = raw.map { d ->
                                                    val r = d.boundingBox
                                                    Detection(
                                                        RectF(
                                                            (r.left / frame.width.toFloat()).coerceIn(0f, 1f),
                                                            (r.top / frame.height.toFloat()).coerceIn(0f, 1f),
                                                            (r.right / frame.width.toFloat()).coerceIn(0f, 1f),
                                                            (r.bottom / frame.height.toFloat()).coerceIn(0f, 1f)
                                                        ),
                                                        d.label,
                                                        d.confidence
                                                    )
                                                }
                                                detections = norm
                                                best = norm.maxByOrNull { it.confidence }
                                            } catch (e: Exception) {
                                                Log.e("CameraScreen", "Analyzer error", e)
                                            } finally {
                                                proxy.close()
                                            }
                                        }
                                    }

                                try {
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageCapture,
                                        analysis
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Camera bind failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )

                    LiveBoxesOverlay(detections)

                    Button(
                        enabled = !isBusy && best != null,
                        onClick = {
                            val ic = imageCapture ?: return@Button
                            val temp = File(context.cacheDir, "IMG_${System.currentTimeMillis()}.jpg")

                            isBusy = true
                            ocrText = null
                            statusMsg = null
                            thumb = null
                            cropPreview = null
                            medResults = emptyList()
                            medError = null

                            ic.takePicture(
                                ImageCapture.OutputFileOptions.Builder(temp).build(),
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onError(exc: ImageCaptureException) {
                                        isBusy = false
                                        statusMsg = "Capture failed: ${exc.message}"
                                    }

                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        scope.launch(Dispatchers.Default) {
                                            try {
                                                val photo = decodeAndFixOrientation(temp.absolutePath)
                                                val detectionsAbs = detections.map { d ->
                                                    val nr = d.boundingBox
                                                    val absRect = RectF(
                                                        nr.left * photo.width,
                                                        nr.top * photo.height,
                                                        nr.right * photo.width,
                                                        nr.bottom * photo.height
                                                    )
                                                    Detection(absRect, d.label, d.confidence)
                                                }

                                                val annotated = com.example.healthguard.drawDetectionsOnBitmap(
                                                    photo, detectionsAbs
                                                )

                                                val name = "IMG_${timestamp()}.jpg"
                                                val hasDetection = best != null
                                                val folder = if (hasDetection) "Medicine" else "Photos"
                                                val savedApp = withContext(Dispatchers.IO) {
                                                    saveToAppPictures(context, annotated, name, folder)
                                                }

                                                thumb = try {
                                                    annotated.scale(
                                                        320,
                                                        (320f / annotated.width * annotated.height).toInt()
                                                    )
                                                } catch (_: Exception) {
                                                    null
                                                }

                                                val bestBox = best
                                                ocrText = if (bestBox != null) {
                                                    val crop = cropFromNormalized(photo, bestBox.boundingBox)
                                                    cropPreview = try {
                                                        crop.scale(
                                                            140,
                                                            (140f / crop.width * crop.height).toInt()
                                                        )
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                    val dto = visionRepo.ocr(crop, 88)
                                                    dto.textFromServer() ?: "(no text found)"
                                                } else "(no detection)"

                                                // 🔵 NEW APPROACH: Use ChatRepository.processMedicineImage
                                                val ocr = ocrText?.trim().orEmpty()
                                                if (ocr.length >= 3 && temp.exists()) {
                                                    try {
                                                        // Process the full image
                                                        val imageToProcess = File(context.cacheDir, "process_${System.currentTimeMillis()}.jpg")
                                                        imageToProcess.outputStream().use { os ->
                                                            annotated.compress(Bitmap.CompressFormat.JPEG, 90, os)
                                                        }

                                                        Log.d("CameraScreen", "Starting processMedicineImage...")
                                                        val result = chatRepo.processMedicineImage(imageToProcess)

                                                        when (result) {
                                                            is ChatSessionResult.Success -> {
                                                                Log.d("CameraScreen", "Success! SessionId: ${result.sessionId}")

                                                                // Show bubble on Main thread with try-catch
                                                                withContext(Dispatchers.Main) {
                                                                    try {
                                                                        val title = result.medicines.firstOrNull()?.brand
                                                                            ?: result.medicines.firstOrNull()?.generic
                                                                            ?: "Φάρμακο"

                                                                        Log.d("CameraScreen", "Showing overlay with title: $title")

                                                                        overlayVm.showFromChatResult(
                                                                            sessionId = result.sessionId,
                                                                            title = title,
                                                                            sources = result.sources
                                                                        )

                                                                        Log.d("CameraScreen", "Overlay shown successfully")
                                                                    } catch (e: Exception) {
                                                                        Log.e("CameraScreen", "Error showing overlay", e)
                                                                        Toast.makeText(
                                                                            context,
                                                                            "Βρέθηκε: ${result.medicines.firstOrNull()?.brand ?: "φάρμακο"}",
                                                                            Toast.LENGTH_LONG
                                                                        ).show()
                                                                    }
                                                                }
                                                            }
                                                            is ChatSessionResult.Error -> {
                                                                Log.e("CameraScreen", "Chat error: ${result.message}")
                                                                withContext(Dispatchers.Main) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Σφάλμα: ${result.message}",
                                                                        Toast.LENGTH_LONG
                                                                    ).show()
                                                                }
                                                            }
                                                        }

                                                        // Clean up temp file
                                                        imageToProcess.delete()

                                                    } catch (e: Exception) {
                                                        Log.e("CameraScreen", "Processing failed", e)
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                context,
                                                                "Σφάλμα επεξεργασίας: ${e.message}",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                }

                                                // Optional: local medicine search (keep this for preview)
                                                val qCandidate = ocrText?.trim().orEmpty()
                                                if (qCandidate.length >= 3) {
                                                    try {
                                                        val q = qCandidate.lineSequence()
                                                            .firstOrNull()
                                                            .orEmpty()
                                                            .take(80)
                                                        val results = medsRepo.searchMedicines(
                                                            qRaw = q,
                                                            lang = null,
                                                            source = "both"
                                                        )
                                                        medResults = results
                                                        medError = null
                                                    } catch (e: Exception) {
                                                        medResults = emptyList()
                                                        medError = "Search failed: ${e.message}"
                                                    }
                                                }

                                                statusMsg = if (savedApp)
                                                    "Saved to app in $folder."
                                                else
                                                    "App save failed."
                                            } catch (e: Exception) {
                                                statusMsg = "Error: ${e.message}"
                                            } finally {
                                                withContext(Dispatchers.Main) { isBusy = false }
                                                temp.delete()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    ) { Text("Capture") }
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(visible = thumb != null || isBusy) {
                ResultCard(
                    thumb = thumb,
                    crop = cropPreview,
                    ocr = ocrText ?: if (isBusy) "Running OCR…" else "(no text)",
                    status = statusMsg ?: if (isBusy) "Saving…" else null,
                    meds = medResults,
                    medsError = medError,
                    onOpenUrl = { url -> openCustomTab(context, url) }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("Confidence Threshold: %.2f".format(conf))
            Slider(value = conf, onValueChange = { conf = it }, valueRange = 0f..1f)
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/* ------------------------------------------------------------- *
 * UI pieces
 * ------------------------------------------------------------- */

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
                size = androidx.compose.ui.geometry.Size(
                    (r.right - r.left) * w,
                    (r.bottom - r.top) * h
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
private fun ResultCard(
    thumb: Bitmap?,
    crop: Bitmap?,
    ocr: String,
    status: String?,
    meds: List<MedRecord>,
    medsError: String?,
    onOpenUrl: (String) -> Unit
) {
    val cardScroll = rememberScrollState()
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 0.dp, max = 360.dp)
    ) {
        Column(Modifier.verticalScroll(cardScroll)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                thumb?.let {
                    Image(it.asImageBitmap(), null, Modifier.size(84.dp).padding(end = 12.dp))
                }
                crop?.let {
                    Image(it.asImageBitmap(), null, Modifier.size(84.dp).padding(end = 12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("OCR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(ocr, style = MaterialTheme.typography.bodyLarge)
                    if (!status.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(status, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Matches", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    when {
                        !medsError.isNullOrBlank() -> Text(medsError, color = Color(0xFFB00020))
                        meds.isEmpty() -> Text("No results.")
                        else -> meds.take(20).forEach { m ->
                            val line1 = listOfNotNull(m.brand, m.generic)
                                .distinct().joinToString(" • ")
                                .ifBlank { m.generic ?: m.brand ?: "Unknown" }
                            val line2 = listOfNotNull(m.strength, m.form).joinToString(" • ")
                            val clickable = (m.url?.isNotBlank() == true)
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .then(if (clickable) Modifier.clickable { onOpenUrl(m.url!!) } else Modifier)
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(line1, style = MaterialTheme.typography.bodyLarge,
                                    color = if (clickable) Color(0xFF1A73E8) else Color.Unspecified)
                                if (line2.isNotBlank()) {
                                    Text(line2, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------- *
 * Helpers
 * ------------------------------------------------------------- */
private fun openCustomTab(context: Context, url: String) {
    runCatching { CustomTabsIntent.Builder().build().launchUrl(context, url.toUri()) }
}

private fun timestamp(): String =
    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

private fun saveToAppPictures(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
    subFolder: String
): Boolean = try {
    val root = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return false
    val outDir = File(root, subFolder).apply { if (!exists()) mkdirs() }
    val f = File(outDir, fileName)
    f.outputStream().use { os -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, os) }
    true
} catch (_: Exception) { false }

private fun cropFromNormalized(photo: Bitmap, norm: RectF): Bitmap {
    val left = (norm.left * photo.width).toInt().coerceIn(0, photo.width - 1)
    val top = (norm.top * photo.height).toInt().coerceIn(0, photo.height - 1)
    val right = (norm.right * photo.width).toInt().coerceIn(left + 1, photo.width)
    val bottom = (norm.bottom * photo.height).toInt().coerceIn(top + 1, photo.height)
    return Bitmap.createBitmap(photo, left, top, right - left, bottom - top)
}

/* --------- CameraX helpers ---------- */
private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }

private fun ImageProxy.toBitmap(): Bitmap {
    val plane = planes[0].buffer
    val w = width
    val h = height
    plane.rewind()
    val pixelStride = planes[0].pixelStride
    val rowStride = planes[0].rowStride
    val rowPadding = rowStride - pixelStride * w
    val bmp = createBitmap(w + rowPadding / pixelStride, h)
    bmp.copyPixelsFromBuffer(plane)
    return Bitmap.createBitmap(bmp, 0, 0, w, h)
}

/* --------- VisionDto helper ---------- */
private fun VisionDto.textFromServer(): String? =
    (text ?: fullText ?: words?.joinToString(" "))
        ?.takeIf { it.isNotBlank() }