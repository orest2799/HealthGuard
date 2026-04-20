package com.example.healthguard.presentation.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.healthguard.R
import com.example.healthguard.viewmodel.CameraViewModel
import com.example.healthguard.viewmodel.utils.Detection
import java.io.File
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun CameraScreen(
    navController: NavController,
    onImageCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraViewModel: CameraViewModel = viewModel()

    LaunchedEffect(Unit) { cameraViewModel.initDetector(context) }

    // --- Camera Permission ---
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // --- Gallery Permission ---
    val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasGalleryPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, galleryPermission)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasGalleryPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val tempFile = uriToFile(context, uri)
            if (tempFile != null) onImageCaptured(tempFile)
        }
    }

    // --- CameraX Setup ---
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val analyzerExec = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(hasCameraPermission, cameraViewModel.lensFacing) {
        if (!hasCameraPermission) return@LaunchedEffect
        try {
            val provider = ProcessCameraProvider.getInstance(context).await()
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build().also { ia ->
                    ia.setAnalyzer(analyzerExec) { proxy ->
                        if (!cameraViewModel.isBusy) {
                            val frame = proxy.toRgbaBitmap()
                            cameraViewModel.runDetection(frame)
                        }
                        proxy.close()
                    }
                }

            val selector = CameraSelector.Builder()
                .requireLensFacing(cameraViewModel.lensFacing)
                .build()

            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, analysis)
        } catch (e: Exception) {
            Log.e("CameraScreen", "Camera setup failed", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose { analyzerExec.shutdown() }
    }

    // --- UI ---
    Scaffold(containerColor = Color.Black) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {

                // Camera Preview
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })

                // YOLO Bounding Boxes
                LiveBoxesOverlay(cameraViewModel.detections)

                // Top Left: Back Arrow
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        // back arrow
                        contentDescription = stringResource(R.string.camera_back) ,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Bottom Bar: Gallery | Capture | Flip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Button
                    IconButton(
                        onClick = {
                            if (hasGalleryPermission) {
                                galleryLauncher.launch("image/*")
                            } else {
                                galleryPermissionLauncher.launch(galleryPermission)
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.camera_open_gallery),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Capture Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(if (cameraViewModel.isBusy) Color.Gray else Color.White)
                            .clickable(enabled = !cameraViewModel.isBusy) {
                                cameraViewModel.isBusy = true
                                val tempFile = File(
                                    context.cacheDir,
                                    "scan_${System.currentTimeMillis()}.jpg"
                                )
                                imageCapture.takePicture(
                                    ImageCapture.OutputFileOptions.Builder(tempFile).build(),
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(res: ImageCapture.OutputFileResults) {
                                            onImageCaptured(tempFile)  // this triggers ChatViewModel which saves properly
                                            cameraViewModel.isBusy = false
                                        }
                                        override fun onError(e: ImageCaptureException) {
                                            cameraViewModel.isBusy = false
                                            Log.e("CameraScreen", "Capture failed", e)
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cameraViewModel.isBusy) {
                            Text("...", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // Flip Camera Button
                    IconButton(
                        onClick = { cameraViewModel.flipCamera() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = stringResource(R.string.camera_flip),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

            } else {
                Text(
                    stringResource(R.string.camera_permission_required),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// --- Helpers ---

private fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File(context.cacheDir, "gallery_pick_${System.currentTimeMillis()}.jpg")
        tempFile.outputStream().use { output -> inputStream.copyTo(output) }
        inputStream.close()
        tempFile
    } catch (e: Exception) {
        Log.e("CameraScreen", "URI to file failed", e)
        null
    }
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
