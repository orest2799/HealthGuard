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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.healthguard.data.network.dto.ChatSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: ChatViewModel,
    startWithSessionId: String? = null,
    startTitle: String? = null
) {
    var input by remember { mutableStateOf("") }
    val messages by vm.messages.collectAsState()
    val sending by vm.isSending.collectAsState()

    LaunchedEffect(startWithSessionId) {
        if (startWithSessionId != null) vm.startWithSession(startWithSessionId, startTitle)
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Φαρμακευτικός Βοηθός") })
        Divider()

        LazyColumn(
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

            if (sending) item { AssistiveTyping() }
        }

        Divider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ρώτησε π.χ. Δοσολογία; Αντενδείξεις;") }
            )
            Spacer(Modifier.height(0.dp)) // spacer for symmetry
            Button(
                onClick = { vm.send(input); input = "" },
                enabled = !sending && input.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("Send") }
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun BotBubble(text: String, sources: List<ChatSource>) {
    val ctx = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text)
            if (sources.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sources) { s ->
                        AssistChip(
                            onClick = {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.url)))
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
private fun AssistiveTyping() {
    Text(
        "Ο βοηθός πληκτρολογεί…",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(12.dp)
    )
}
