package com.komal.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.komal.ai.KomalApp
import com.komal.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as KomalApp
    val prefs = app.preferencesManager

    var appLanguage by remember { mutableStateOf(prefs.appLanguage) }
    var wakeWordInput by remember { mutableStateOf(prefs.wakeWord) }
    var useGeminiVoice by remember { mutableStateOf(prefs.useGeminiNativeVoice) }
    var selectedPersona by remember { mutableStateOf(prefs.geminiVoicePersona) }
    var offlineMode by remember { mutableStateOf(prefs.offlineModeEnabled) }
    var speechPitch by remember { mutableFloatStateOf(prefs.ttsPitch) }
    var speechSpeed by remember { mutableFloatStateOf(prefs.ttsSpeed) }

    val presetWakePhrases = listOf("hey komal", "suno komal", "komal", "ok komal")
    val geminiPersonas = listOf("Aoede", "Kore", "Puck", "Charon", "Fenrir")

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
                    text = "Voice & AI Speech Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure Gemini AI native voices, offline quick actions, response language, and background wake phrases.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        // Gemini AI Native Voice Persona Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gemini AI Native Voice",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = AccentCyanLight
                        )
                        Text(
                            text = "Use realistic Gemini API voice instead of robotic phone TTS",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = useGeminiVoice,
                        onCheckedChange = {
                            useGeminiVoice = it
                            prefs.useGeminiNativeVoice = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentCyan
                        )
                    )
                }

                if (useGeminiVoice) {
                    Divider(color = DarkCard)
                    Text(
                        text = "Choose Gemini Voice Persona:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        geminiPersonas.forEach { persona ->
                            FilterChip(
                                selected = selectedPersona == persona,
                                onClick = {
                                    selectedPersona = persona
                                    prefs.geminiVoicePersona = persona
                                    Toast.makeText(context, "Voice Persona: $persona", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(persona) }
                            )
                        }
                    }

                    Text(
                        text = when (selectedPersona) {
                            "Aoede" -> "Aoede: Warm, natural, expressive female voice (Recommended for Komal)"
                            "Kore" -> "Kore: Gentle, soothing, relaxed female voice"
                            "Puck" -> "Puck: Youthful, energetic, friendly voice"
                            "Charon" -> "Charon: Deep, calm, mature voice"
                            else -> "Fenrir: Crisp, authoritative, sharp voice"
                        },
                        fontSize = 11.sp,
                        color = AccentEmerald
                    )
                }
            }
        }

        // Offline Fast Mode Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Offline Fast Mode (Zero Latency)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Executes basic actions (Open WhatsApp, YouTube, Camera, Home, Back, Recents) instantly without Gemini API",
                        fontSize = 12.sp,
                        color = if (offlineMode) AccentEmerald else TextSecondary,
                        lineHeight = 16.sp
                    )
                }
                Switch(
                    checked = offlineMode,
                    onCheckedChange = {
                        offlineMode = it
                        prefs.offlineModeEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentEmerald
                    )
                )
            }
        }

        // Assistant Language Selection Card (Hinglish / English)
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
                    text = "Response Language",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccentCyanLight
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = appLanguage == "hinglish",
                        onClick = {
                            appLanguage = "hinglish"
                            prefs.appLanguage = "hinglish"
                            app.ttsManager.setLanguage("hinglish")
                            Toast.makeText(context, "Language set to Hinglish", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("Hinglish (Hindi + English)") },
                        leadingIcon = {
                            if (appLanguage == "hinglish") {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    )

                    FilterChip(
                        selected = appLanguage == "english",
                        onClick = {
                            appLanguage = "english"
                            prefs.appLanguage = "english"
                            app.ttsManager.setLanguage("english")
                            Toast.makeText(context, "Language set to English", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("English") },
                        leadingIcon = {
                            if (appLanguage == "english") {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                }
            }
        }

        // Wake Word Settings Card
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
                    text = "Wake-up Phrase",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccentCyanLight
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetWakePhrases.forEach { phrase ->
                        FilterChip(
                            selected = wakeWordInput.equals(phrase, ignoreCase = true),
                            onClick = {
                                wakeWordInput = phrase
                                prefs.wakeWord = phrase
                                Toast.makeText(context, "Wake phrase set: '$phrase'", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text(phrase) }
                        )
                    }
                }

                OutlinedTextField(
                    value = wakeWordInput,
                    onValueChange = {
                        wakeWordInput = it
                        prefs.wakeWord = it
                    },
                    label = { Text("Custom Wake Phrase") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = DarkCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        // Test Voice Button
        Button(
            onClick = {
                val sampleText = if (appLanguage == "english") {
                    "Hello! I am Komal, your voice-activated AI phone controller."
                } else {
                    "Hello! Main Komal hoon, aapki AI phone controller."
                }
                app.ttsManager.speak(sampleText, pitch = speechPitch, speed = speechSpeed)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoDark)
        ) {
            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test Voice Speech")
        }
    }
}
