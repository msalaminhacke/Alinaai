package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.service.JarvisBackgroundService
import com.example.service.JarvisFloatingBubbleService
import com.example.ui.screens.AssistantMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AssistantViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AssistantViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 13+ needs this permission at runtime, or reminders never show.
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        registerBubbleCallbacks()
        handleIntent(intent)

        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AssistantMainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("AUTO_START_VOICE", false)) {
            viewModel.startAssistant(true)
        }
        if (intent.getBooleanExtra("AUTO_EXPLAIN_SCREEN", false)) {
            viewModel.explainCurrentScreen()
        }
    }

    private fun registerBubbleCallbacks() {
        JarvisFloatingBubbleService.onVoiceTrigger = {
            viewModel.startAssistant(true)
        }
        JarvisFloatingBubbleService.onScreenExplainTrigger = {
            viewModel.explainCurrentScreen()
        }
        JarvisBackgroundService.start(this)
    }
}