package com.example.healthguard.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healthguard.data.network.dto.ApiClient
import com.example.healthguard.data.network.dto.ChatRequest
import kotlinx.coroutines.launch

/**
 * Simple screen to test all backend endpoints
 * Use this to verify your backend connection works before testing the full app
 */
@Composable
fun BackendTestScreen() {
    var result by remember { mutableStateOf("Press buttons to test endpoints") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🧪 Backend Test Screen",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Backend URL:\nhttps://healthguard-backend-wuhgp7pn3a-oc.a.run.app/",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider()

        // Test 1: Health Check
        TestButton(
            text = "1. Test Health Endpoint",
            enabled = !isLoading,
            onClick = {
                scope.launch {
                    isLoading = true
                    result = try {
                        val response = ApiClient.health.getHealth()
                        if (response.isSuccessful) {
                            "✅ Health: ${response.body()}\nCode: ${response.code()}"
                        } else {
                            "❌ Error: ${response.code()} ${response.message()}"
                        }
                    } catch (e: Exception) {
                        "❌ Exception: ${e.message}\n${e.javaClass.simpleName}"
                    }
                    isLoading = false
                }
            }
        )

        // Test 2: Medicine Search
        TestButton(
            text = "2. Test Medicine Search (Depon)",
            enabled = !isLoading,
            onClick = {
                scope.launch {
                    isLoading = true
                    result = try {
                        val response = ApiClient.med.search(
                            q = "Depon",
                            lang = "el",
                            source = "galinos"
                        )
                        val medicines = response.results
                        val top3 = medicines.take(3).joinToString("\n") {
                            "  • ${it.brand ?: it.generic ?: "Unknown"}"
                        }
                        "✅ Found ${medicines.size} medicines:\n$top3"
                    } catch (e: Exception) {
                        "❌ Exception: ${e.message}\n${e.javaClass.simpleName}"
                    }
                    isLoading = false
                }
            }
        )

        // Test 3: Medicine Search (Greek)
        TestButton(
            text = "3. Test Search (Παρακεταμόλη)",
            enabled = !isLoading,
            onClick = {
                scope.launch {
                    isLoading = true
                    result = try {
                        val response = ApiClient.med.search(
                            q = "Παρακεταμόλη",
                            lang = "el",
                            source = "galinos"
                        )
                        val medicines = response.results
                        val top3 = medicines.take(3).joinToString("\n") {
                            "  • ${it.brand ?: it.generic ?: "Unknown"}"
                        }
                        "✅ Found ${medicines.size} medicines:\n$top3"
                    } catch (e: Exception) {
                        "❌ Exception: ${e.message}\n${e.javaClass.simpleName}"
                    }
                    isLoading = false
                }
            }
        )

        // Test 4: Chat with Medicine
        TestButton(
            text = "4. Test Chat (Depon question)",
            enabled = !isLoading,
            onClick = {
                scope.launch {
                    isLoading = true
                    result = try {
                        val response = ApiClient.chat.chat(
                            ChatRequest(
                                sessionId = null,
                                text = "Τι είναι το Depon;",
                                medQuery = "Depon"
                            )
                        )
                        "✅ Session: ${response.sessionId}\n\n" +
                                "Reply: ${response.reply.take(200)}...\n\n" +
                                "Sources: ${response.metadata?.get("sources")}"
                    } catch (e: Exception) {
                        "❌ Exception: ${e.message}\n${e.javaClass.simpleName}"
                    }
                    isLoading = false
                }
            }
        )

        // Test 5: Chat with Greek
        TestButton(
            text = "5. Test Chat (Greek medicine)",
            enabled = !isLoading,
            onClick = {
                scope.launch {
                    isLoading = true
                    result = try {
                        val response = ApiClient.chat.chat(
                            ChatRequest(
                                sessionId = null,
                                text = "Ποιες είναι οι ενδείξεις του Depon;",
                                medQuery = "Depon",
                                context = mapOf("language" to "el")
                            )
                        )
                        "✅ Session: ${response.sessionId}\n\n" +
                                "Reply:\n${response.reply.take(300)}...\n\n" +
                                "Has metadata: ${response.metadata != null}"
                    } catch (e: Exception) {
                        "❌ Exception: ${e.message}\n${e.javaClass.simpleName}"
                    }
                    isLoading = false
                }
            }
        )

        // Test 6: Follow-up message
        var testSessionId by remember { mutableStateOf<String?>(null) }
        TestButton(
            text = "6. Test Follow-up (2 messages)",
            enabled = !isLoading,
            onClick = {
                scope.launch {
                    isLoading = true
                    result = try {
                        // First message
                        val response1 = ApiClient.chat.chat(
                            ChatRequest(
                                sessionId = null,
                                text = "Τι είναι το Depon;",
                                medQuery = "Depon"
                            )
                        )
                        testSessionId = response1.sessionId

                        // Follow-up message
                        val response2 = ApiClient.chat.chat(
                            ChatRequest(
                                sessionId = response1.sessionId,
                                text = "Ποια είναι η δοσολογία του;"
                            )
                        )

                        "✅ Two-message conversation:\n\n" +
                                "Session: ${response1.sessionId}\n\n" +
                                "Message 1: ${response1.reply.take(100)}...\n\n" +
                                "Message 2: ${response2.reply.take(100)}..."
                    } catch (e: Exception) {
                        "❌ Exception: ${e.message}\n${e.javaClass.simpleName}"
                    }
                    isLoading = false
                }
            }
        )

        Divider()

        // Result display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📋 Result:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💡 Testing Instructions:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = """
                        1. Test each button in order
                        2. All tests should show ✅
                        3. If any fail with ❌, check:
                           • Internet connection
                           • Backend URL is correct
                           • Logcat for detailed errors
                        4. Greek text should display correctly
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TestButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}