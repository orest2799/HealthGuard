package com.example.healthguard.presentation.gallery

import android.graphics.BitmapFactory
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.healthguard.R
import com.example.healthguard.viewmodel.ChatViewModel
import com.example.healthguard.viewmodel.GalleryViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    chatVm: ChatViewModel,
    onImageSelected: (File) -> Unit,
    galleryViewModel: GalleryViewModel = viewModel()
) {
    val state by galleryViewModel.state.collectAsState()
    var deleteImage by remember { mutableStateOf<File?>(null) }
    var selectedImage by remember { mutableStateOf<File?>(null) }

    // Reload images every time the screen is entered — so newly scanned medicines appear
    LaunchedEffect(Unit) {
        galleryViewModel.loadImages()
    }

    // Full screen image viewer dialog
    selectedImage?.let { file ->
        val bitmap = remember(file) { BitmapFactory.decodeFile(file.absolutePath) }
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
                    navController.navigate(
                        "chat/new?title=${java.net.URLEncoder.encode(displayName, "UTF-8")}"
                    )
                }) {
                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.gallery_info))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteImage = file; selectedImage = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.gallery_delete))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gallery_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.images.isEmpty() -> {
                    Text(
                        stringResource(R.string.gallery_no_images),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column {
                        state.images.forEach { (_, images) ->
                            if (images.isNotEmpty()) {
                                LazyVerticalGrid(columns = GridCells.Fixed(3)) {
                                    items(images) { file ->
                                        val bitmap = remember(file) {
                                            BitmapFactory.decodeFile(file.absolutePath)
                                        }
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
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    deleteImage?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteImage = null },
            title = { Text(stringResource(R.string.gallery_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        galleryViewModel.deleteImage(file)
                        deleteImage = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.gallery_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteImage = null }) {
                    Text(stringResource(R.string.gallery_cancel))
                }
            }
        )
    }
}