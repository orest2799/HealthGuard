package com.example.healthguard.presentation.chat

import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.healthguard.data.network.dto.ChatSource
import com.example.healthguard.viewmodel.ChatViewModel
import io.noties.markwon.Markwon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: ChatViewModel,
    startWithSessionId: String? = null,
    startTitle: String? = null
) {
    var input by remember { mutableStateOf("") }
    val messages by vm.messages.collectAsState()
    val quickActions by vm.quickActions.collectAsState()   // ✅ NEW
    val listState = rememberLazyListState()
    val isSending by vm.isSending.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(startWithSessionId) {
        if (startWithSessionId != null) {
            vm.startWithSession(startWithSessionId, startTitle)
        }
    }

    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(if (isSending) messages.size else messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(startTitle ?: "Φαρμακευτικός Βοηθός") },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Chat",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    if (msg.fromUser) UserBubble(text = msg.text)
                    else BotBubble(text = msg.text, sources = msg.sources)

                    Spacer(Modifier.height(8.dp))
                }

                if (isSending) {
                    item { AssistiveTyping() }
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(8.dp)
            ) {
                // ✅ Backend-driven Quick Actions
                if (quickActions.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickActions, key = { it.id }) { action ->
                            AssistChip(
                                onClick = { if (!isSending) vm.send(action.message) },
                                label = { Text(action.title) },
                                enabled = !isSending
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ρωτήστε κάτι άλλο...") },
                        enabled = !isSending
                    )
                    Button(
                        onClick = {
                            if (input.isNotBlank()) {
                                vm.send(input)
                                input = ""
                            }
                        },
                        enabled = !isSending && input.isNotBlank(),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(56.dp)
                    ) {
                        Text("Αποστολή")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Διαγραφή συνομιλίας;") },
            text = { Text("Είστε σίγουροι ότι θέλετε να καθαρίσετε το ιστορικό;") },
            confirmButton = {
                TextButton(onClick = { vm.clearChat(); showDeleteDialog = false }) {
                    Text("Διαγραφή", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Ακύρωση") }
            }
        )
    }
}

@Composable
fun UserBubble(text: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.End) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(start = 60.dp)
        ) {
            Text(text, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
fun BotBubble(text: String, sources: List<ChatSource>) {
    val ctx = LocalContext.current
    val markwon = remember { Markwon.create(ctx) }
    val bubbleColor = if (androidx.compose.foundation.isSystemInDarkTheme()) {
        androidx.compose.ui.graphics.Color(0xFF2C2C2E)  // slightly lighter than black
    } else {
        androidx.compose.ui.graphics.Color(0xFFE0E0E0)  // medium grey, clearly visible
    }
    val textColorInt = if (androidx.compose.foundation.isSystemInDarkTheme()) {
        android.graphics.Color.WHITE
    } else {
        android.graphics.Color.BLACK
    }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Surface(
            color = bubbleColor,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(end = 60.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            textSize = 14f
                            setLineSpacing(4f, 1f)
                        }
                    },
                    update = { textView ->
                        textView.setTextColor(textColorInt)
                        markwon.setMarkdown(textView, text)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (sources.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Πηγές:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sources) { s ->
                            AssistChip(
                                onClick = {
                                    val fixedUrl = if (s.url.contains("galinos.gr")) {
                                        val uri = Uri.parse(s.url)
                                        val query = uri.getQueryParameter("q") ?: ""

                                        // 1. Clean the query: replace '+' with spaces and trim extra whitespace
                                        val cleanQuery = query.replace("+", " ").trim()

                                        // 2. Encode the full string (Name + Strength)
                                        val encodedQuery = java.net.URLEncoder.encode(cleanQuery, "UTF-8")

                                        // 3. Use the search endpoint
                                        "https://www.galinos.gr/web/drugs/main/search?q=$encodedQuery"
                                    } else {
                                        s.url
                                    }

                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl))
                                    ctx.startActivity(intent)
                                },
                                label = {
                                    Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssistiveTyping() {
    Text(
        "Ο HealthGuard αναζητά πληροφορίες...",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(12.dp),
        color = MaterialTheme.colorScheme.outline
    )
}