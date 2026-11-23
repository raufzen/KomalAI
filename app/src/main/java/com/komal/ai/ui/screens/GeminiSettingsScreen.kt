package com.komal.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komal.ai.KomalApp
import com.komal.ai.gemini.GeminiModelItem
import com.komal.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiSettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as KomalApp
    val prefs = app.preferencesManager
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var apiKeyInput by remember { mutableStateOf(prefs.apiKey) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoadingModels by remember { mutableStateOf(false) }
    var availableModels by remember { mutableStateOf(prefs.cachedModels) }
    var selectedMainModel by remember { mutableStateOf(prefs.selectedMainModel) }
    var selectedVoiceModel by remember { mutableStateOf(prefs.selectedVoiceModel) }

    var mainDropdownExpanded by remember { mutableStateOf(false) }
    var voiceDropdownExpanded by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isTestingModel by remember { mutableStateOf(false) }

    fun fetchModelsFromApi() {
        if (apiKeyInput.isBlank()) {
            Toast.makeText(context, "Please enter your Gemini API Key first", Toast.LENGTH_SHORT).show()
            return
        }

        isLoadingModels = true
        statusMessage = "Fetching available models from Gemini API..."
        focusManager.clearFocus()

        scope.launch {
            val result = app.geminiClient.fetchModels(apiKeyInput)
            isLoadingModels = false
            result.onSuccess { models ->
                availableModels = models
                prefs.apiKey = apiKeyInput
                prefs.cachedModels = models

                if (models.isNotEmpty()) {
                    if (!models.any { it.name == selectedMainModel }) {
                        selectedMainModel = models.first().name
                        prefs.selectedMainModel = selectedMainModel
                    }
                    if (!models.any { it.name == selectedVoiceModel }) {
                        selectedVoiceModel = models.first().name
                        prefs.selectedVoiceModel = selectedVoiceModel
                    }
                    statusMessage = "Success! Loaded ${models.size} models from your API key."
                    Toast.makeText(context, "Loaded ${models.size} Gemini Models", Toast.LENGTH_SHORT).show()
                } else {
                    statusMessage = "No chat-compatible models returned."
                }
            }.onFailure { e ->
                statusMessage = "Error: ${e.localizedMessage}"
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Google Gemini API & Model Hub",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter your Google Gemini API key. All available models (Pro, Flash, Vision) will be dynamically loaded directly from Google's server.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        // API Key Input Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Gemini API Key",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccentCyanLight
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        fetchModelsFromApi()
                    }),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = DarkCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Button(
                    onClick = { fetchModelsFromApi() },
                    enabled = !isLoadingModels && apiKeyInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    if (isLoadingModels) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetching Models...")
                    } else {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetch Models from API")
                    }
                }

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        fontSize = 12.sp,
                        color = if (statusMessage!!.startsWith("Error")) AccentRose else AccentEmerald
                    )
                }
            }
        }

        // Model Selection Section
        if (availableModels.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Available Models (${availableModels.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyanLight
                        )
                        Badge(containerColor = PrimaryIndigoDark) {
                            Text(
                                "Live API",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Selector 1: Main Controller Brain
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "1. Phone Controller Brain (Primary Agent Model)",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Used for UI analysis, WhatsApp, YouTube, and phone action planning",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        ExposedDropdownMenuBox(
                            expanded = mainDropdownExpanded,
                            onExpandedChange = { mainDropdownExpanded = !mainDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedMainModel.removePrefix("models/"),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mainDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = DarkCard,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = mainDropdownExpanded,
                                onDismissRequest = { mainDropdownExpanded = false }
                            ) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = model.cleanId,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (!model.displayName.isNullOrBlank()) {
                                                    Text(
                                                        text = model.displayName,
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedMainModel = model.name
                                            prefs.selectedMainModel = model.name
                                            mainDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = DarkCard)

                    // Selector 2: Voice & Fast Response Model
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "2. Fast Voice Response Model",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Low-latency fast model for instant voice feedback (e.g. gemini-1.5-flash / gemini-2.0-flash)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        ExposedDropdownMenuBox(
                            expanded = voiceDropdownExpanded,
                            onExpandedChange = { voiceDropdownExpanded = !voiceDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedVoiceModel.removePrefix("models/"),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = DarkCard,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = voiceDropdownExpanded,
                                onDismissRequest = { voiceDropdownExpanded = false }
                            ) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = model.cleanId,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (!model.displayName.isNullOrBlank()) {
                                                    Text(
                                                        text = model.displayName,
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedVoiceModel = model.name
                                            prefs.selectedVoiceModel = model.name
                                            voiceDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Test Model Connection Button
                    OutlinedButton(
                        onClick = {
                            isTestingModel = true
                            scope.launch {
                                val testResult = app.geminiClient.planAction(
                                    apiKey = prefs.apiKey,
                                    modelName = selectedMainModel,
                                    userPrompt = "Hello Komal, test connection!",
                                    screenContext = "",
                                    languageMode = prefs.appLanguage
                                )
                                isTestingModel = false
                                testResult.onSuccess {
                                    Toast.makeText(context, "Success! Model connected perfectly", Toast.LENGTH_LONG).show()
                                    app.ttsManager.speak(if (prefs.appLanguage == "english") "Connection successful! Komal is ready." else "Connection successful! Komal taiyar hai.")
                                }.onFailure { e ->
                                    Toast.makeText(context, "Test Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isTestingModel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTestingModel) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Connection...")
                        } else {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Model Connection")
                        }
                    }
                }
            }
        }
    }
}
