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
                ConsoleLogItem("00:00", "System", "Komal AI Test Console is ready. Using model: ${prefs.selectedTestModel.removePrefix("models/")}")
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
            addLog("System", "Error: Please set your Gemini API Key in Gemini AI tab first!")
            return
        }

        focusManager.clearFocus()
        isExecuting = true
        addLog("User", cmd)
        inputText = ""

        scope.launch {
            try {
                val screenContext = KomalAccessibilityService.instance?.dumpScreenHierarchy() ?: ""
                val testModel = prefs.selectedTestModel
                val langMode = prefs.appLanguage

                addLog("Gemini", "Planning action using Test Model '$testModel' ($langMode)...")
                val result = app.geminiClient.planAction(
                    apiKey = apiKey,
                    modelName = testModel,
                    userPrompt = cmd,
                    screenContext = screenContext,
                    languageMode = langMode,
                    voicePersona = prefs.geminiVoicePersona,
                    requestAudio = true
                )

                result.onSuccess { candidate ->
                    val part = candidate.content?.parts?.firstOrNull()
                    if (part?.functionCall != null) {
                        val call = part.functionCall
                        addLog("Executor", "Tool Call: ${call.name}\nArgs: ${call.args}", isAction = true)
                        app.actionExecutor.executeFunctionCall(
                            call = call,
                            apiKey = apiKey,
                            selectedModel = testModel,
                            onStatusUpdate = { update ->
                                addLog("Komal", update)
                            }
                        )
                    }

                    // Play Gemini native voice
                    val audioPart = candidate.content?.parts?.firstOrNull { it.inlineData != null }
                    if (audioPart?.inlineData != null) {
                        app.audioPlayer.playBase64Audio(audioPart.inlineData.data, audioPart.inlineData.mimeType)
                    } else if (!part?.text.isNullOrBlank()) {
                        addLog("Komal", part.text ?: "")
                        app.voiceSpeaker.speak(part.text ?: "")
                    }
                }.onFailure { e ->
                    addLog("System", "Error: ${e.localizedMessage}")
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
            text = "Quick Test Commands (Using: ${prefs.selectedTestModel.removePrefix("models/")}):",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AccentCyanLight
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = { executeTestCommand("YouTube pe Arijit Singh ke gaane chalao") },
                label = { Text("YouTube Songs", fontSize = 11.sp) }
            )
            SuggestionChip(
                onClick = { executeTestCommand("WhatsApp open karo") },
                label = { Text("Open WhatsApp", fontSize = 11.sp) }
            )
            SuggestionChip(
                onClick = { executeTestCommand("Aaj ki latest news batao") },
                label = { Text("Today's News", fontSize = 11.sp) }
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
                placeholder = { Text("Type instruction... e.g. Open YouTube") },
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
