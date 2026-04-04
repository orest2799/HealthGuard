package com.example.healthguard.presentation.gallery

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthguard.viewmodel.ChatViewModel
import java.io.File


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    chatVm: ChatViewModel,
    onImageSelected: (File) -> Unit
) {
    val context = LocalContext.current
    var refreshFlag by remember { mutableStateOf(false) }
    var deleteImage by remember { mutableStateOf<File?>(null) }
    var selectedImage by remember { mutableStateOf<File?>(null) }

    val folderImagesMap = remember(refreshFlag) { getGroupedImages(context) }

    // Full screen image viewer
    selectedImage?.let { file ->
        val bitmap = remember(file) { BitmapFactory.decodeFile(file.absolutePath) }

        // Full display name: augmentin_875-125mg → Augmentin 875-125mg
        val displayName = file.nameWithoutExtension
            .replace("_", " ")
            .replaceFirstChar { it.uppercase() }

        AlertDialog(
            onDismissRequest = { selectedImage = null },
            title = { Text(displayName, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedImage = null
                    chatVm.startChatFromGallery(displayName, "")
                    navController.navigate("chat/new?title=${
                        java.net.URLEncoder.encode(displayName, "UTF-8")
                    }")
                }) {
                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Πληροφορίες")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteImage = file
                        selectedImage = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Διαγραφή")
                }
            }
        )
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
        }
    ) { paddingValues ->
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

                    LazyVerticalGrid(columns = GridCells.Fixed(3)) {
                        items(images) { file ->
                            val bitmap = remember(file) {
                                BitmapFactory.decodeFile(file.absolutePath)
                            }
                            // augmentin_875-125mg → Augmentin 875-125mg
                            val displayName = file.nameWithoutExtension
                                .replace("_", " ")
                                .replaceFirstChar { it.uppercase() }

                            bitmap?.let {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .combinedClickable(
                                            onClick = { selectedImage = file },
                                            onLongClick = { deleteImage = file }
                                        )
                                ) {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(MaterialTheme.shapes.small)
                                    )
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (isEmpty) {
                Box(Modifier.fillMaxSize()) {
                    Text("No images found.", Modifier.align(Alignment.Center))
                }
            }
        }
    }

    deleteImage?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteImage = null },
            title = { Text("Διαγραφή εικόνας;") },
            confirmButton = {
                TextButton(
                    onClick = {
                        file.delete()
                        refreshFlag = !refreshFlag
                        deleteImage = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Διαγραφή") }
            },
            dismissButton = {
                TextButton(onClick = { deleteImage = null }) { Text("Ακύρωση") }
            }
        )
    }
}

fun getGroupedImages(context: Context): Map<String, List<File>> {
    val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
    val rootDir = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        userId
    )

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