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
import com.komal.ai.KomalApp
import com.komal.ai.ui.theme.*

@Composable
fun VoiceSettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as KomalApp
    val prefs = app.preferencesManager

    var appLanguage by remember { mutableStateOf(prefs.appLanguage) }
    var wakeWordInput by remember { mutableStateOf(prefs.wakeWord) }
    var speechPitch by remember { mutableFloatStateOf(prefs.ttsPitch) }
    var speechSpeed by remember { mutableFloatStateOf(prefs.ttsSpeed) }

    val presetWakePhrases = listOf("hey komal", "suno komal", "komal", "ok komal")

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
                    text = "Voice & Language Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select your preferred assistant language (Hinglish or English), wake phrase, and customize voice pitch and speed.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
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
                    text = "Assistant Response Language",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccentCyanLight
                )

                Text(
                    text = "Choose how Komal talks and replies to your commands:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Hinglish
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

                    // Option 2: English
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

                Text(
                    text = "Say this phrase anytime in the background to activate Komal:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Presets
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

        // TTS Voice Customization
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Text-to-Speech Voice Controls",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccentCyanLight
                )

                // Speed Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Speech Speed", fontSize = 13.sp, color = TextPrimary)
                        Text(text = String.format("%.2fx", speechSpeed), fontSize = 13.sp, color = AccentEmerald)
                    }
                    Slider(
                        value = speechSpeed,
                        onValueChange = {
                            speechSpeed = it
                            prefs.ttsSpeed = it
                        },
                        valueRange = 0.6f..1.6f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentEmerald,
                            activeTrackColor = AccentEmerald
                        )
                    )
                }

                // Pitch Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Voice Pitch", fontSize = 13.sp, color = TextPrimary)
                        Text(text = String.format("%.2fx", speechPitch), fontSize = 13.sp, color = AccentCyan)
                    }
                    Slider(
                        value = speechPitch,
                        onValueChange = {
                            speechPitch = it
                            prefs.ttsPitch = it
                        },
                        valueRange = 0.7f..1.4f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentCyan,
                            activeTrackColor = AccentCyan
                        )
                    )
                }

                // Test Voice Button
                Button(
                    onClick = {
                        val sampleText = if (appLanguage == "english") {
                            "Hello! I am Komal, your autonomous AI phone controller. I can open WhatsApp, play YouTube, and control your device on your voice command."
                        } else {
                            "Hello! Main Komal hoon, aapki AI phone controller. Main aapke voice command pe WhatsApp, YouTube aur pura phone chala sakti hoon."
                        }
                        app.ttsManager.speak(sampleText, pitch = speechPitch, speed = speechSpeed)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoDark)
                ) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Komal Voice")
                }
            }
        }
    }
}
