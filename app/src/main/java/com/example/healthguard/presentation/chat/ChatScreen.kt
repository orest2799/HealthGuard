package com.example.healthguard.presentation.chat

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.healthguard.data.network.dto.ChatSource
import com.example.healthguard.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: ChatViewModel,
    startWithSessionId: String? = null,
    startTitle: String? = null
) {
    var input by remember { mutableStateOf("") }
    val messages by vm.messages.collectAsState()
    val listState = rememberLazyListState()
    val isSending by vm.isSending.collectAsState()

    // 1. Προσθήκη του state για το Dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    val quickQuestions = listOf(
        "Πώς να το πάρω;",
        "Παρενέργειες;",
        "Ξέχασα τη δόση μου",
        "Αλληλεπιδράσεις;",
        "Αντενδείξεις"
    )

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
                            // 2. Διόρθωση του Icon component
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Chat",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
                    if (msg.fromUser) {
                        UserBubble(text = msg.text)
                    } else {
                        BotBubble(text = msg.text, sources = msg.sources)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (isSending) {
                    item(key = "typing_indicator") {
                        AssistiveTyping()
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(8.dp)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickQuestions) { question ->
                        AssistChip(
                            onClick = { if (!isSending) vm.send(question) },
                            label = { Text(question) },
                            enabled = !isSending
                        )
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
                        maxLines = 4,
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

    // 3. Προσθήκη του AlertDialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Διαγραφή συνομιλίας;") },
            text = { Text("Είστε σίγουροι ότι θέλετε να καθαρίσετε όλο το ιστορικό αυτής της συζήτησης;") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearChat() // Βεβαιώσου ότι η clearChat() υπάρχει στο ViewModel
                        showDeleteDialog = false
                    }
                ) {
                    Text("Διαγραφή", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Ακύρωση")
                }
            }
        )
    }
}

// Οι Bubbles συναρτήσεις παραμένουν ως έχουν κάτω από την ChatScreen
@Composable
fun UserBubble(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(start = 60.dp)
            .fillMaxWidth()
    ) {
        Text(text, modifier = Modifier.padding(12.dp))
    }
}

@Composable
fun BotBubble(text: String, sources: List<ChatSource>) {
    val ctx = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(end = 60.dp)
            .fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text)
            if (sources.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sources) { s ->
                        AssistChip(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(s.url))
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

@Composable
fun AssistiveTyping() {
    Text(
        "Ο βοηθός πληκτρολογεί...",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(12.dp)
    )
}