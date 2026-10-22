package com.example.ui.screens
import com.example.ui.components.CameraVisionModal
import com.example.ui.components.JarvisFeaturesBar


import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.AssistantCharacter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.data.model.AssistantState
import com.example.ui.components.ActionNotificationBanner
import com.example.ui.components.CentralPowerButton
import com.example.ui.components.DeveloperProfileModal
import com.example.ui.components.DeviceQuickActionBar
import com.example.ui.components.FuturisticBackground
import com.example.ui.components.FuturisticOrbVisualizer
import com.example.ui.components.LiveWaveformBarVisualizer
import com.example.ui.components.SassyPersonaTicker
import com.example.ui.components.SettingsSheet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantMainScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val assistantState by viewModel.assistantState.collectAsState()
    val micAmp by viewModel.micAmplitude.collectAsState()
    val speakerAmp by viewModel.speakerAmplitude.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val sassyQuote by viewModel.sassyQuote.collectAsState()
    val recentActions by viewModel.recentActions.collectAsState()
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val isFlashlightOn by viewModel.isFlashlightOn.collectAsState()
    val durationSeconds by viewModel.sessionDurationSeconds.collectAsState()
    val selectedCharacter by viewModel.selectedCharacter.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val showDeveloperProfile by viewModel.showDeveloperProfile.collectAsState()
    val developerAvatarUri by viewModel.developerAvatarUri.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Check microphone permission
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startAssistant(true)
        } else {
            showPermissionDialog = true
        }
    }

    
    val isSentryArmed by viewModel.isSentryArmed.collectAsState()
    val isSentryAlarmTriggered by viewModel.isSentryAlarmTriggered.collectAsState()
    val sentryCountdown by viewModel.sentryCountdown.collectAsState()
    val isWakeWordActive by viewModel.isWakeWordActive.collectAsState()
    val isBubbleActive by viewModel.isBubbleActive.collectAsState()
    val isNotificationAnnouncerEnabled by viewModel.isNotificationAnnouncerEnabled.collectAsState()
    val showVisionScanner by viewModel.showVisionScanner.collectAsState()
    val isVisionAnalyzing by viewModel.isVisionAnalyzing.collectAsState()
    val visionResult by viewModel.visionResult.collectAsState()
    val visionQuery by viewModel.visionQuery.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    FuturisticBackground(
        isDarkMode = isDarkMode,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .testTag("assistant_main_screen")
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP HUD / HEADER BAR
                TopHudBar(
                    state = assistantState,
                    durationSeconds = durationSeconds,
                    batteryPercent = batteryInfo.percentage,
                    isCharging = batteryInfo.isCharging,
                    language = selectedLanguage,
                    character = selectedCharacter,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onOpenDeveloper = { viewModel.setShowDeveloperProfile(true) },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onToggleCharacter = {
                        viewModel.setCharacter(
                            when (selectedCharacter) {
                                com.example.data.model.AssistantCharacter.ALINA -> com.example.data.model.AssistantCharacter.BLEVER
                                com.example.data.model.AssistantCharacter.BLEVER -> com.example.data.model.AssistantCharacter.JARVIS
                                else -> com.example.data.model.AssistantCharacter.ALINA
                            }
                        )
                    },
                    onSettingsClick = { showSettings = true }
                )

                // 1.5 FUTURISTIC TELEMETRY SUB-BANNER
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkMode) ObsidianSurface.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, NeonCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "JARVIS HUD v4.2 // QUANTUM CORE",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                        Text(
                            text = "DEV: MD. ALAMIN",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) NeonAmber else Color(0xFFD97706)
                        )
                    }
                }

                // 2. ACTION NOTIFICATION (Executed Device Actions)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ActionNotificationBanner(
                        action = recentActions.firstOrNull()
                    )
                }

                // 3. CENTER VISUALIZER AREA
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        FuturisticOrbVisualizer(
                            state = assistantState,
                            micAmplitude = micAmp,
                            speakerAmplitude = speakerAmp,
                            character = selectedCharacter
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Character Switch Bar with live portrait thumbnails
                        CharacterQuickSwitchBar(
                            selectedCharacter = selectedCharacter,
                            onSelect = { character -> viewModel.setCharacter(character) },
                            isDarkMode = isDarkMode
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real-time audio waveform equalizer
                        LiveWaveformBarVisualizer(
                            state = assistantState,
                            amplitude = if (assistantState == AssistantState.SPEAKING) speakerAmp else micAmp
                        )
                    }
                }

                // 4. SASSY PERSONA TICKER & ATTITUDE DISPLAY
                SassyPersonaTicker(
                    state = assistantState,
                    quote = sassyQuote,
                    language = selectedLanguage
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 5. QUICK DEVICE ACTIONS & DEVELOPER SHORTCUT
                DeviceQuickActionBar(
                    isFlashlightOn = isFlashlightOn,
                    language = selectedLanguage,
                    isDarkMode = isDarkMode,
                    onTrigger = { actionName ->
                        viewModel.triggerDeviceActionDirectly(actionName)
                    },
                    onOpenDeveloper = {
                        viewModel.setShowDeveloperProfile(true)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // JARVIS ADVANCED FEATURES (SENTRY, VISION, WAKE WORD, BUBBLE, ANNOUNCER)
                JarvisFeaturesBar(
                    isSentryArmed = isSentryArmed,
                    isSentryAlarmTriggered = isSentryAlarmTriggered,
                    sentryCountdown = sentryCountdown,
                    isWakeWordActive = isWakeWordActive,
                    isBubbleActive = isBubbleActive,
                    isNotificationAnnouncerEnabled = isNotificationAnnouncerEnabled,
                    isVisionAnalyzing = isVisionAnalyzing,
                    onToggleSentry = { viewModel.toggleSentryMode() },
                    onDisarmSentry = { viewModel.disarmSentry() },
                    onToggleWakeWord = { viewModel.toggleWakeWord() },
                    onToggleBubble = { viewModel.toggleFloatingBubble() },
                    onToggleAnnouncer = { viewModel.toggleNotificationAnnouncer() },
                    onOpenVision = { viewModel.openCameraVision() }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 6. BOTTOM CONTROL DOCK
                BottomControlDock(
                    state = assistantState,
                    isMuted = isMuted,
                    isDarkMode = isDarkMode,
                    onMuteToggle = { viewModel.toggleMute() },
                    onPowerClick = {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleSession(true)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }


            if (showVisionScanner) {
                CameraVisionModal(
                    isAnalyzing = isVisionAnalyzing,
                    visionResult = visionResult,
                    visionQuery = visionQuery,
                    onQueryChange = { viewModel.openCameraVision(it) },
                    onCaptureAndAnalyze = { bmp, q -> viewModel.analyzeCapturedBitmap(bmp, q) },
                    onDismiss = { viewModel.closeCameraVision() }
                )
            }

            // Settings Sheet
            if (showSettings) {
                SettingsSheet(
                    sheetState = sheetState,
                    currentApiKey = apiKey,
                    selectedVoice = selectedVoice,
                    selectedModel = selectedModel,
                    selectedLanguage = selectedLanguage,
                    selectedCharacter = selectedCharacter,
                    developerAvatarPath = developerAvatarUri,
                    isAccessibilityEnabled = viewModel.isAccessibilityEnabled(),
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onOpenDeveloperProfile = { viewModel.setShowDeveloperProfile(true) },
                    onCharacterChange = { viewModel.setCharacter(it) },
                    onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                    onApiKeyChange = { viewModel.setApiKey(it) },
                    onVoiceChange = { viewModel.setVoice(it) },
                    onModelChange = { viewModel.setModel(it) },
                    onLanguageChange = { viewModel.setLanguage(it) },
                    customPrompt = viewModel.getCustomPrompt(selectedCharacter),
                    onCustomPromptSave = { viewModel.setCustomPrompt(selectedCharacter, it) },
                    onCustomPromptReset = { viewModel.resetCustomPrompt(selectedCharacter) },
                    onDismiss = { showSettings = false }
                )
            }

            // Confirmation for sensitive actions (call, SMS, WhatsApp)
            val pendingAction by viewModel.pendingAction.collectAsState()
            pendingAction?.let { action ->
                AlertDialog(
                    onDismissRequest = { viewModel.cancelPendingAction() },
                    title = { Text("নিশ্চিত করুন / Confirm") },
                    text = { Text(action.summary + "\n\n🎙 বলুন: \"হ্যাঁ\" বা \"না\"") },
                    confirmButton = { },
                    dismissButton = null
                )
            }

            // Developer Profile Modal
            if (showDeveloperProfile) {
                DeveloperProfileModal(
                    isDarkMode = isDarkMode,
                    customAvatarPath = developerAvatarUri,
                    onUploadAvatar = { uri -> viewModel.saveCustomDeveloperAvatar(uri) },
                    onSaveCustomAvatarPath = { path -> viewModel.saveCustomDeveloperAvatarPath(path) },
                    onResetAvatar = { viewModel.resetDeveloperAvatarToDefault() },
                    onDismiss = { viewModel.setShowDeveloperProfile(false) },
                    onOpenUrl = { viewModel.openDeveloperUrl(it) },
                    onCopyText = { text, label -> viewModel.copyDeveloperText(text, label) }
                )
            }

            // Permission Dialog
            if (showPermissionDialog) {
            val isBn = (selectedLanguage == "bn")
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDialog = false
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta)
                    ) {
                        Text(
                            text = if (isBn) "অনুমতি দিন" else "Grant Permission",
                            color = ObsidianBg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text(
                            text = if (isBn) "পরে" else "Dismiss",
                            color = TextSecondary
                        )
                    }
                },
                title = {
                    Text(
                        text = if (isBn) "মাইক্রোফোন অনুমতি প্রয়োজন" else "Microphone Access Required",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isBn) {
                            "আলীনা লাইভ একটি রিয়েল-টাইম অডিও অ্যাসিস্ট্যান্ট ও বন্ধু। সরাসরি ভয়েসে কথা বলার জন্য মাইক্রোফোন পারমিশন দিন।"
                        } else {
                            "Alina Live is a real-time voice-to-voice AI friend and companion. Please grant microphone permission to converse."
                        }
                    )
                },
                containerColor = ObsidianSurface,
                titleContentColor = NeonMagenta,
                textContentColor = TextPrimary
            )
        }

        // Error Banner
        AnimatedVisibility(
            visible = errorMessage != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp)
        ) {
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF3B0B18),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF3366)),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clickable { viewModel.clearError() }
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFF99AA),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
}

@Composable
private fun TopHudBar(
    state: AssistantState,
    durationSeconds: Long,
    batteryPercent: Int,
    isCharging: Boolean,
    language: String = "bn",
    character: com.example.data.model.AssistantCharacter = com.example.data.model.AssistantCharacter.ALINA,
    isDarkMode: Boolean = true,
    onToggleDarkMode: () -> Unit = {},
    onOpenDeveloper: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    onToggleCharacter: () -> Unit = {},
    onSettingsClick: () -> Unit
) {
    val isBlever = character == com.example.data.model.AssistantCharacter.BLEVER
    val brandColor = when (character) {
        com.example.data.model.AssistantCharacter.BLEVER -> NeonCyan
        com.example.data.model.AssistantCharacter.JARVIS -> Color(0xFFFFB300)
        else -> NeonMagenta
    }
    val pillBg = if (isDarkMode) ObsidianSurface else Color.White
    val pillBorder = if (isDarkMode) ObsidianBorder else Color(0xFFCBD5E1)
    val textColor = if (isDarkMode) TextPrimary else Color(0xFF0F172A)
    val textSub = if (isDarkMode) TextSecondary else Color(0xFF475569)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App title & live indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onOpenDeveloper() }
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (state) {
                            AssistantState.IDLE -> pillBorder
                            AssistantState.CONNECTING -> NeonCyan
                            AssistantState.LISTENING -> NeonCyan
                            AssistantState.SPEAKING -> brandColor
                            AssistantState.ERROR -> Color(0xFFFF3366)
                        }
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = when (character) {
                        com.example.data.model.AssistantCharacter.BLEVER -> "BLEVER"
                        com.example.data.model.AssistantCharacter.JARVIS -> "JARVIS"
                        else -> "ALINA"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = textColor
                )
                Text(
                    text = if (state != AssistantState.IDLE) formatDuration(durationSeconds) else "JARVIS AI",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = brandColor
                )
            }
        }

        // Right side: Character Switch + Dark/Light Toggle + Dev Profile + Battery + Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Quick 1-Tap Character Switch Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = brandColor.copy(alpha = if (isDarkMode) 0.18f else 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, brandColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleCharacter() }
                    .testTag("character_quick_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (character) {
                            com.example.data.model.AssistantCharacter.BLEVER -> "👦 ব্লিভার"
                            com.example.data.model.AssistantCharacter.JARVIS -> "🤖 জার্ভিস"
                            else -> "👧 আলিনা"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
                    )
                }
            }

            // Dark / Light Mode Toggle Button
            IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(pillBg)
                    .border(1.dp, pillBorder, CircleShape)
                    .testTag("theme_mode_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = if (isDarkMode) NeonAmber else Color(0xFF6366F1),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Developer Profile Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NeonCyan.copy(alpha = if (isDarkMode) 0.18f else 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenDeveloper() }
                    .testTag("top_bar_dev_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👨‍💻 স্যার",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }

            // Quick Language Switch Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (language == "bn") NeonMagenta.copy(alpha = 0.2f) else pillBg,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (language == "bn") NeonMagenta else pillBorder
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleLanguage() }
                    .testTag("language_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "🇧🇩" else "🌐",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (language == "bn") NeonMagenta else textSub
                    )
                }
            }

            // Battery Indicator
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = pillBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, pillBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = "Battery",
                        tint = if (isCharging) NeonCyan else textSub,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$batteryPercent%",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSub
                    )
                }
            }

            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(pillBg)
                    .border(1.dp, pillBorder, CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = textColor,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomControlDock(
    state: AssistantState,
    isMuted: Boolean,
    isDarkMode: Boolean = true,
    onMuteToggle: () -> Unit,
    onPowerClick: () -> Unit
) {
    val dockSurfaceBg = if (isDarkMode) ObsidianSurface else Color.White
    val dockBorder = if (isDarkMode) ObsidianBorder else Color(0xFFCBD5E1)
    val textSub = if (isDarkMode) TextSecondary else Color(0xFF475569)
    val textColor = if (isDarkMode) TextPrimary else Color(0xFF0F172A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute Button (48dp touch target)
        IconButton(
            onClick = onMuteToggle,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (isMuted) Color(0xFF38101E) else dockSurfaceBg)
                .border(
                    1.dp,
                    if (isMuted) NeonMagenta else dockBorder,
                    CircleShape
                )
                .testTag("mute_button")
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = if (isMuted) NeonMagenta else textColor,
                modifier = Modifier.size(24.dp)
            )
        }

        // Central Mic / Power Activator
        CentralPowerButton(
            state = state,
            isMuted = isMuted,
            onClick = onPowerClick
        )

        // Volume Info Indicator
        Surface(
            shape = CircleShape,
            color = dockSurfaceBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, dockBorder),
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Audio Level",
                    tint = if (state == AssistantState.SPEAKING) NeonMagenta else textSub,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d LIVE", mins, secs)
}

@Composable
fun CharacterQuickSwitchBar(
    selectedCharacter: AssistantCharacter,
    onSelect: (AssistantCharacter) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val isAlina = selectedCharacter == AssistantCharacter.ALINA
    val isBlever = selectedCharacter == AssistantCharacter.BLEVER
    val isJarvis = selectedCharacter == AssistantCharacter.JARVIS

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isDarkMode) ObsidianSurface.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else Color(0xFFCBD5E1)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Alina Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isAlina) NeonMagenta.copy(alpha = 0.2f) else Color.Transparent,
                border = if (isAlina) BorderStroke(1.5.dp, NeonMagenta) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelect(AssistantCharacter.ALINA) }
                    .testTag("quick_switch_alina")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_alina_portrait),
                        contentDescription = "Alina Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(1.dp, NeonMagenta), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "আলীনা (ALINA)",
                        fontSize = 11.5.sp,
                        fontWeight = if (isAlina) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAlina) NeonMagenta else if (isDarkMode) TextSecondary else Color(0xFF64748B)
                    )
                }
            }

            // Blever Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isBlever) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                border = if (isBlever) BorderStroke(1.5.dp, NeonCyan) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelect(AssistantCharacter.BLEVER) }
                    .testTag("quick_switch_blever")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_blever_portrait),
                        contentDescription = "Blever Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(1.dp, NeonCyan), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ব্লেভার (BLEVER)",
                        fontSize = 11.5.sp,
                        fontWeight = if (isBlever) FontWeight.Bold else FontWeight.Medium,
                        color = if (isBlever) NeonCyan else if (isDarkMode) TextSecondary else Color(0xFF64748B)
                    )
                }
            }

            // Jarvis Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isJarvis) Color(0xFFFFB300).copy(alpha = 0.2f) else Color.Transparent,
                border = if (isJarvis) BorderStroke(1.5.dp, Color(0xFFFFB300)) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelect(AssistantCharacter.JARVIS) }
                    .testTag("quick_switch_jarvis")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🤖", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "জার্ভিস (JARVIS)",
                        fontSize = 11.5.sp,
                        fontWeight = if (isJarvis) FontWeight.Bold else FontWeight.Medium,
                        color = if (isJarvis) Color(0xFFFFB300) else if (isDarkMode) TextSecondary else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
