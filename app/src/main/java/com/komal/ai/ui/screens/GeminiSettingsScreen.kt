package com.komal.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
            Toast.makeText(context, "कृपया पहले Gemini API Key दर्ज करें", Toast.LENGTH_SHORT).show()
            return
        }

        isLoadingModels = true
        statusMessage = "Gemini API से मॉडल लोड हो रहे हैं..."
        focusManager.clearFocus()

        scope.launch {
            val result = app.geminiClient.fetchModels(apiKeyInput)
            isLoadingModels = false
            result.onSuccess { models ->
                availableModels = models
                prefs.apiKey = apiKeyInput
                prefs.cachedModels = models

                if (models.isNotEmpty()) {
                    // Set default if not set
                    if (!models.any { it.name == selectedMainModel }) {
                        selectedMainModel = models.first().name
                        prefs.selectedMainModel = selectedMainModel
                    }
                    if (!models.any { it.name == selectedVoiceModel }) {
                        selectedVoiceModel = models.first().name
                        prefs.selectedVoiceModel = selectedVoiceModel
                    }
                    statusMessage = "सफल! ${models.size} मॉडल लोड हो गए हैं।"
                    Toast.makeText(context, "${models.size} Gemini Models लोड हो गए", Toast.LENGTH_SHORT).show()
                } else {
                    statusMessage = "कोई उपयुक्त मॉडल नहीं मिला।"
                }
            }.onFailure { e ->
                statusMessage = "त्रुटि: ${e.localizedMessage}"
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
                    text = "Google Gemini API & मॉडल हब",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "अपनी Gemini API Key जोड़ें। ऐप स्वचालित रूप से Google सर्वर से सभी उपलब्ध मॉडल्स लोड कर लेगा।",
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
                        Text("मॉडल लोड हो रहे हैं...")
                    } else {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("API से सभी मॉडल्स लोड करें (Load Models)")
                    }
                }

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        fontSize = 12.sp,
                        color = if (statusMessage!!.startsWith("त्रुटि")) AccentRose else AccentEmerald
                    )
                }
            }
        }

        // Model Selection Section (Shown when models are available)
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
                            text = "उपलब्ध मॉडल्स (${availableModels.size})",
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
                            text = "1. फोन कंट्रोल व मुख्य एजेंट मॉडल (Phone Controller Brain)",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "WhatsApp, YouTube और स्क्रीन क्लिकिंग के फैसलों के लिए उच्च सटीकता वाला मॉडल चुनें",
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
                            text = "2. तेज़ वॉयस और त्वरित प्रतिक्रिया मॉडल (Fast Voice Model)",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "तुरंत बोलकर उत्तर देने के लिए तेज़ और कम लेटेंसी वाला मॉडल (जैसे gemini-1.5-flash / gemini-2.0-flash)",
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
                                    screenContext = ""
                                )
                                isTestingModel = false
                                testResult.onSuccess {
                                    Toast.makeText(context, "सत्यापित! मॉडल सफलतापूर्वक कनेक्ट हो गया", Toast.LENGTH_LONG).show()
                                    app.ttsManager.speak("नमस्ते! मॉडल सफलतापूर्वक कनेक्ट हो चुका है")
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
                            Text("Testing connection...")
                        } else {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("चयनित मॉडल का परीक्षण करें (Test Model)")
                        }
                    }
                }
            }
        }
    }
}
