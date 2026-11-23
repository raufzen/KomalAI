package com.komal.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komal.ai.KomalApp
import com.komal.ai.accessibility.KomalAccessibilityService
import com.komal.ai.ui.theme.*
import kotlinx.coroutines.launch

data class ConsoleLogItem(
    val time: String,
    val sender: String,
    val message: String,
    val isAction: Boolean = false
)

@Composable
fun TestConsoleScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as KomalApp
    val prefs = app.preferencesManager
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var inputText by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }
    var logs by remember {
        mutableStateOf(
            listOf(
                ConsoleLogItem("00:00", "System", "कोमल AI टेस्ट कंसोल तैयार है। नीचे दिए गए बटन दबाएं या अपना कमांड टाइप करें।")
            )
        )
    }

    fun addLog(sender: String, message: String, isAction: Boolean = false) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        logs = logs + ConsoleLogItem(time, sender, message, isAction)
    }

    fun executeTestCommand(cmd: String) {
        if (cmd.isBlank() || isExecuting) return
        val apiKey = prefs.apiKey
        if (apiKey.isBlank()) {
            addLog("System", "त्रुटि: पहले Gemini API Key सेट करें!")
            return
        }

        focusManager.clearFocus()
        isExecuting = true
        addLog("User", cmd)
        inputText = ""

        scope.launch {
            try {
                val screenContext = KomalAccessibilityService.instance?.dumpScreenHierarchy() ?: ""
                val model = prefs.selectedMainModel

                addLog("Gemini", "मॉडल '$model' से योजना बन रही है...")
                val result = app.geminiClient.planAction(
                    apiKey = apiKey,
                    modelName = model,
                    userPrompt = cmd,
                    screenContext = screenContext
                )

                result.onSuccess { candidate ->
                    val part = candidate.content?.parts?.firstOrNull()
                    if (part?.functionCall != null) {
                        val call = part.functionCall
                        addLog("Executor", "टूल क्रिया: ${call.name}\nतर्क: ${call.args}", isAction = true)
                        app.actionExecutor.executeFunctionCall(
                            call = call,
                            apiKey = apiKey,
                            selectedModel = model,
                            onStatusUpdate = { update ->
                                addLog("Komal", update)
                            }
                        )
                    } else if (!part?.text.isNullOrBlank()) {
                        addLog("Komal", part.text ?: "")
                        app.ttsManager.speak(part.text ?: "")
                    }
                }.onFailure { e ->
                    addLog("System", "त्रुटि: ${e.localizedMessage}")
                }
            } catch (e: Exception) {
                addLog("System", "Exception: ${e.message}")
            } finally {
                isExecuting = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sample command quick chips
        Text(
            text = "त्वरित परीक्षण कमांड्स (Quick Commands):",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AccentCyanLight
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = { executeTestCommand("YouTube ओपन करके Arijit Singh सर्च करो") },
                label = { Text("YouTube गाने", fontSize = 11.sp) }
            )
            SuggestionChip(
                onClick = { executeTestCommand("WhatsApp ओपन करो") },
                label = { Text("WhatsApp खोलें", fontSize = 11.sp) }
            )
            SuggestionChip(
                onClick = { executeTestCommand("आज का ताजा न्यूज़ बोलो") },
                label = { Text("आज का न्यूज़", fontSize = 11.sp) }
            )
        }

        // Live Log List
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (log.isAction) DarkCard else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = log.sender,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = when (log.sender) {
                                    "User" -> AccentCyanLight
                                    "Gemini" -> AccentAmber
                                    "Executor" -> AccentEmerald
                                    "Komal" -> PrimaryIndigo
                                    else -> TextSecondary
                                }
                            )
                            Text(
                                text = log.time,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = log.message,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Bottom Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("कमांड लिखें... जैसे: आज का न्यूज़ बोलो") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = DarkCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            IconButton(
                onClick = { executeTestCommand(inputText) },
                enabled = !isExecuting && inputText.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .background(PrimaryIndigo, CircleShape)
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
