package com.example.healthguard.presentation.camera

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import java.io.File


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(navController: NavController) {
    val context = LocalContext.current

    var refreshFlag by remember { mutableStateOf(false) }
    var fullscreenImage by remember { mutableStateOf<File?>(null) }
    var deleteImage by remember { mutableStateOf<File?>(null) }

    val folderImagesMap = remember(refreshFlag) {
        getGroupedImages(context)
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")

                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(8.dp)
            ) {
                var isEmpty = true

                folderImagesMap.forEach { (folderName, images) ->
                    if (images.isNotEmpty()) {
                        isEmpty = false
                        Text(
                            text = "📂 ${folderName.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(images) { file ->
                                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = file.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .combinedClickable(
                                                onClick = { fullscreenImage = file },
                                                onLongClick = { deleteImage = file }
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                if (isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No images found.")
                    }
                }
            }
        }
    )

    // Fullscreen Preview
    fullscreenImage?.let { file ->
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        bitmap?.let {
            Dialog(onDismissRequest = { fullscreenImage = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    deleteImage?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteImage = null },
            title = { Text("Delete Image") },
            text = { Text("Are you sure you want to delete this image?") },
            confirmButton = {
                TextButton(onClick = {
                    file.delete()
                    refreshFlag = !refreshFlag
                    deleteImage = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteImage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun getGroupedImages(context: Context): Map<String, List<File>> {
    val rootDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return emptyMap()

    val medicineDir = File(rootDir, "Medicine")
    val photosDir = File(rootDir, "Photos")

    val medicines = medicineDir
        .listFiles()
        ?.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

    val photos = photosDir
        .listFiles()
        ?.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

    return mapOf(
        "Medicine" to medicines,
        "Photos" to photos
    ).filterValues { it.isNotEmpty() }
}

