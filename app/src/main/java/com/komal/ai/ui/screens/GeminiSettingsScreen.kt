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

    var selectedControllerModel by remember { mutableStateOf(prefs.selectedControllerModel) }
    var selectedVoiceModel by remember { mutableStateOf(prefs.selectedVoiceModel) }
    var selectedTestModel by remember { mutableStateOf(prefs.selectedTestModel) }

    var controllerDropdownExpanded by remember { mutableStateOf(false) }
    var voiceDropdownExpanded by remember { mutableStateOf(false) }
    var testDropdownExpanded by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    fun fetchModelsFromApi() {
        if (apiKeyInput.isBlank()) {
            Toast.makeText(context, "Please enter your Gemini API Key first", Toast.LENGTH_SHORT).show()
            return
        }

        isLoadingModels = true
        statusMessage = "Loading available models from Google Gemini API..."
        focusManager.clearFocus()

        scope.launch {
            val result = app.geminiClient.fetchModels(apiKeyInput)
            isLoadingModels = false
            result.onSuccess { models ->
                availableModels = models
                prefs.apiKey = apiKeyInput
                prefs.cachedModels = models

                if (models.isNotEmpty()) {
                    if (!models.any { it.name == selectedControllerModel }) {
                        selectedControllerModel = models.first().name
                        prefs.selectedControllerModel = selectedControllerModel
                    }
                    if (!models.any { it.name == selectedVoiceModel }) {
                        selectedVoiceModel = models.first().name
                        prefs.selectedVoiceModel = selectedVoiceModel
                    }
                    if (!models.any { it.name == selectedTestModel }) {
                        selectedTestModel = models.first().name
                        prefs.selectedTestModel = selectedTestModel
                    }
                    statusMessage = "Success! Loaded ${models.size} models from your API key."
                    Toast.makeText(context, "Loaded ${models.size} Gemini Models", Toast.LENGTH_SHORT).show()
                } else {
                    statusMessage = "No chat-compatible models found."
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Hub, contentDescription = null, tint = AccentCyan)
                    Text(
                        text = "Gemini Models & API Hub",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Configure separate Gemini models for Phone Controlling, Voice Generation, and Test Console.",
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
                    text = "Google Gemini API Key",
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
                        Text("Loading Models from API...")
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

        // Section: Independent Model Selectors
        if (availableModels.isNotEmpty()) {
            Text(
                text = "Independent Model Selection (${availableModels.size} Loaded)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = AccentCyanLight
            )

            // Selector 1: Phone Controller Brain Model
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = AccentEmerald)
                        Text(
                            text = "1. Phone Controller Brain Model",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Used for Accessibility screen reading, WhatsApp messaging, and app navigation planning",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    ExposedDropdownMenuBox(
                        expanded = controllerDropdownExpanded,
                        onExpandedChange = { controllerDropdownExpanded = !controllerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedControllerModel.removePrefix("models/"),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = controllerDropdownExpanded) },
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
                            expanded = controllerDropdownExpanded,
                            onDismissRequest = { controllerDropdownExpanded = false }
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(model.cleanId, fontWeight = FontWeight.Bold)
                                            if (!model.displayName.isNullOrBlank()) {
                                                Text(model.displayName, fontSize = 11.sp, color = TextSecondary)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedControllerModel = model.name
                                        prefs.selectedControllerModel = model.name
                                        controllerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Selector 2: Voice & Speech Model
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = AccentCyan)
                        Text(
                            text = "2. Voice Output Model (Gemini Speech)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Dedicated model for generating Gemini native AI audio speech (e.g. gemini-2.0-flash)",
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
                                            Text(model.cleanId, fontWeight = FontWeight.Bold)
                                            if (!model.displayName.isNullOrBlank()) {
                                                Text(model.displayName, fontSize = 11.sp, color = TextSecondary)
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
            }

            // Selector 3: Test Console Model
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = AccentAmber)
                        Text(
                            text = "3. Test Console Model",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Model used specifically when executing prompts in the in-app Test Console",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    ExposedDropdownMenuBox(
                        expanded = testDropdownExpanded,
                        onExpandedChange = { testDropdownExpanded = !testDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedTestModel.removePrefix("models/"),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = testDropdownExpanded) },
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
                            expanded = testDropdownExpanded,
                            onDismissRequest = { testDropdownExpanded = false }
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(model.cleanId, fontWeight = FontWeight.Bold)
                                            if (!model.displayName.isNullOrBlank()) {
                                                Text(model.displayName, fontSize = 11.sp, color = TextSecondary)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedTestModel = model.name
                                        prefs.selectedTestModel = model.name
                                        testDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Test Model Connection Button
            OutlinedButton(
                onClick = {
                    isTestingConnection = true
                    scope.launch {
                        val testResult = app.geminiClient.planAction(
                            apiKey = prefs.apiKey,
                            modelName = selectedControllerModel,
                            userPrompt = "Hello Komal, test connection!",
                            screenContext = "",
                            languageMode = prefs.appLanguage
                        )
                        isTestingConnection = false
                        testResult.onSuccess {
                            Toast.makeText(context, "Success! Model connected perfectly", Toast.LENGTH_LONG).show()
                            app.voiceSpeaker.speak("Connection successful! Komal is ready.")
                        }.onFailure { e ->
                            Toast.makeText(context, "Test Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isTestingConnection,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing Connection...")
                } else {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Connection")
                }
            }
        }
    }
}
