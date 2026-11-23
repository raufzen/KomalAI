package com.komal.ai.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komal.ai.KomalApp
import com.komal.ai.accessibility.KomalAccessibilityService
import com.komal.ai.service.VoiceWakeService
import com.komal.ai.ui.theme.*

@Composable
fun DashboardScreen(onNavigateToGemini: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as KomalApp
    val prefs = app.preferencesManager

    var isAccessibilityActive by remember { mutableStateOf(KomalAccessibilityService.isServiceRunning) }
    var isVoiceActive by remember { mutableStateOf(prefs.isVoiceWakeEnabled) }
    var isOverlayGranted by remember {
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true)
    }

    LaunchedEffect(Unit) {
        isAccessibilityActive = KomalAccessibilityService.isServiceRunning
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isOverlayGranted = Settings.canDrawOverlays(context)
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
        // Hero Header Card
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(PrimaryIndigoDark, Color(0xFF7C3AED))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Komal AI Assistant",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Phone Controller & Voice Automation",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "बोलिए: \"सुनो कोमल, WhatsApp पर Rahul को मैसेज करो\" या \"YouTube पर गाने चलाओ\"",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Master Service Toggle Card
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
                        text = "पृष्ठभूमि वॉयस सेवा (Background Wake)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isVoiceActive) "सक्रिय है - 'सुनो कोमल' सुन रहा है" else "निष्क्रिय - चालू करने के लिए स्विच दबाएं",
                        fontSize = 12.sp,
                        color = if (isVoiceActive) AccentEmerald else TextSecondary
                    )
                }

                Switch(
                    checked = isVoiceActive,
                    onCheckedChange = { enable ->
                        isVoiceActive = enable
                        prefs.isVoiceWakeEnabled = enable
                        if (enable) {
                            VoiceWakeService.start(context)
                        } else {
                            VoiceWakeService.stop(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentEmerald
                    )
                )
            }
        }

        Text(
            text = "सिस्टम अनुमतियां (System Permissions)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AccentCyanLight
        )

        // Permission Card 1: Accessibility Service
        PermissionStatusCard(
            title = "Phone Controller Accessibility Service",
            subtitle = "फ़ोन स्क्रीन पढ़ने, WhatsApp में टाइपिंग और क्लिक करने के लिए आवश्यक है",
            icon = Icons.Default.Accessibility,
            isGranted = isAccessibilityActive,
            actionLabel = "Enable in Settings",
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )

        // Permission Card 2: Draw Over Other Apps (Overlay)
        PermissionStatusCard(
            title = "Floating Assistant Bubble (Overlay)",
            subtitle = "फ़ोन पर किसी भी ऐप के ऊपर कोमल का फ्लोटिंग इंटरफ़ेस दिखाने के लिए",
            icon = Icons.Default.Layers,
            isGranted = isOverlayGranted,
            actionLabel = "Grant Overlay",
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        )

        // Gemini API Configuration Quick Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = AccentCyan
                    )
                    Text(
                        text = "Google Gemini AI मॉडल स्थिति",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val hasKey = prefs.apiKey.isNotBlank()
                Text(
                    text = if (hasKey) "सक्रिय मॉडल: ${prefs.selectedMainModel}" else "Gemini API Key अभी सेट नहीं है!",
                    fontSize = 13.sp,
                    color = if (hasKey) AccentEmerald else AccentRose
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNavigateToGemini,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                ) {
                    Text("API Key और मॉडल कॉन्फ़िगर करें")
                }
            }
        }
    }
}

@Composable
fun PermissionStatusCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) AccentEmerald else AccentAmber
                    )
                    Text(
                        text = title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }

                Badge(
                    containerColor = if (isGranted) AccentEmerald.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f),
                    contentColor = if (isGranted) AccentEmerald else AccentAmber
                ) {
                    Text(
                        text = if (isGranted) "Active" else "Needed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PrimaryIndigoDark,
                        contentColor = Color.White
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
