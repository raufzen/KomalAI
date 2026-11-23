package com.komal.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.komal.ai.ui.screens.DashboardScreen
import com.komal.ai.ui.screens.GeminiSettingsScreen
import com.komal.ai.ui.screens.TestConsoleScreen
import com.komal.ai.ui.screens.VoiceSettingsScreen
import com.komal.ai.ui.theme.DarkBackground
import com.komal.ai.ui.theme.DarkSurface
import com.komal.ai.ui.theme.KomalAITheme
import com.komal.ai.ui.theme.PrimaryIndigo

enum class KomalTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    GEMINI("Gemini AI", Icons.Default.AutoAwesome),
    VOICE("Voice", Icons.Default.RecordVoiceOver),
    CONSOLE("Console", Icons.Default.Terminal)
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            KomalAITheme {
                MainAppContainer()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needed.isNotEmpty()) {
            requestPermissionLauncher.launch(needed.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    var selectedTab by remember { mutableStateOf(KomalTab.DASHBOARD) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedTab.title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface
            ) {
                KomalTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryIndigo,
                            selectedTextColor = PrimaryIndigo,
                            indicatorColor = DarkBackground
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.padding(paddingValues),
            color = DarkBackground
        ) {
            when (selectedTab) {
                KomalTab.DASHBOARD -> DashboardScreen(onNavigateToGemini = { selectedTab = KomalTab.GEMINI })
                KomalTab.GEMINI -> GeminiSettingsScreen()
                KomalTab.VOICE -> VoiceSettingsScreen()
                KomalTab.CONSOLE -> TestConsoleScreen()
            }
        }
    }
}
