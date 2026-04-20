package com.example.healthguard.presentation.chat

import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.healthguard.R
import com.example.healthguard.data.network.dto.ChatSource
import com.example.healthguard.viewmodel.ChatViewModel
import com.example.healthguard.viewmodel.LanguageViewModel
import io.noties.markwon.Markwon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: ChatViewModel,
    startWithSessionId: String? = null,
    startTitle: String? = null,
    languageViewModel: LanguageViewModel
) {
    val chatLanguage by vm.chatLanguage.collectAsState()
    val chatInGreek = chatLanguage == "el"

    var inputText by remember { mutableStateOf("") }
    val messages by vm.messages.collectAsState()
    val quickActions by vm.quickActions.collectAsState()
    val listState = rememberLazyListState()
    val isSending by vm.isSending.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val chatTitle = startTitle ?: stringResource(R.string.chat_title)

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
                title = { Text(chatTitle) },
                actions = {
                    TextButton(
                        onClick = {
                            val newLang = if (chatInGreek) "en" else "el"
                            vm.setChatLanguage(newLang)
                        }
                    ) {
                        Text(
                            // Use hardcoded strings — stringResource() reflects the APP locale,
                            // not the chat language, so it won't update when only the chat
                            // language is toggled (the app locale stays the same).
                            text = if (chatInGreek) "EN" else "GR",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.chat_delete_title),
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
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.chat_placeholder)) },
                        enabled = !isSending
                    )
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                vm.send(inputText)
                                inputText = ""
                            }
                        },
                        enabled = !isSending && inputText.isNotBlank(),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(56.dp)
                    ) {
                        Text(stringResource(R.string.chat_send))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.chat_delete_title)) },
            text = { Text(stringResource(R.string.chat_delete_body)) },
            confirmButton = {
                TextButton(onClick = { vm.clearChat(); showDeleteDialog = false }) {
                    Text(
                        stringResource(R.string.chat_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.chat_cancel))
                }
            }
        )
    }
}

@Composable
fun UserBubble(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
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
    val bubbleColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFE0E0E0)
    val textColorInt = if (isSystemInDarkTheme()) android.graphics.Color.WHITE
    else android.graphics.Color.BLACK

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
                    Text(stringResource(R.string.chat_sources), style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sources) { s ->
                            AssistChip(
                                onClick = {
                                    val fixedUrl = if (s.url.contains("galinos.gr")) {
                                        val uri = Uri.parse(s.url)
                                        val query = uri.getQueryParameter("q") ?: ""
                                        val cleanQuery = query.replace("+", " ").trim()
                                        val encodedQuery = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
                                        "https://www.galinos.gr/web/drugs/main/search?q=$encodedQuery"
                                    } else s.url
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl)))
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
        stringResource(R.string.chat_typing),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(12.dp),
        color = MaterialTheme.colorScheme.outline
    )
}