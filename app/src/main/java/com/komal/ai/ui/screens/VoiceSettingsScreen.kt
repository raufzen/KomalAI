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

    var wakeWordInput by remember { mutableStateOf(prefs.wakeWord) }
    var speechPitch by remember { mutableFloatStateOf(prefs.ttsPitch) }
    var speechSpeed by remember { mutableFloatStateOf(prefs.ttsSpeed) }
    var voiceLanguage by remember { mutableStateOf(prefs.voiceLanguage) }

    val presetWakePhrases = listOf("hey komal", "सुनो कोमल", "komal", "कोमल जी")

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
                    text = "वॉयस और वेक-अप सेटिंग्स",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "वेक-अप शब्द सेट करें जिससे कोमल बैकग्राउंड में एक्टिव होगी, और कोमल की आवाज़ अनुकूलित करें।",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        // Wake Word Settings
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
                    text = "वेक-अप शब्द (Wake Phrase)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccentCyanLight
                )

                Text(
                    text = "जब भी आप यह बोलेंगे, कोमल तुरंत जाग जाएगी:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Quick presets
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
                                Toast.makeText(context, "वेक-अप शब्द सेट: '$phrase'", Toast.LENGTH_SHORT).show()
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
                    label = { Text("कस्टम वेक शब्द") },
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
                    text = "कोमल की आवाज़ (Text to Speech Voice)",
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
                        Text(text = "बोलने की गति (Speech Speed)", fontSize = 13.sp, color = TextPrimary)
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
                        Text(text = "आवाज़ का पिच (Voice Pitch)", fontSize = 13.sp, color = TextPrimary)
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
                        app.ttsManager.speak(
                            text = "नमस्ते! मैं कोमल हूँ, आपकी निजी AI फ़ोन सहायक। मैं आपके आदेश पर YouTube, WhatsApp और पूरा फोन चला सकती हूँ।",
                            pitch = speechPitch,
                            speed = speechSpeed
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoDark)
                ) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("कोमल की आवाज़ का परीक्षण करें (Test Voice)")
                }
            }
        }
    }
}
