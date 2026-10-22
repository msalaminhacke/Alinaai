package com.example.ui.components

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import com.example.R
import com.example.data.model.AvailableVoices
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    sheetState: SheetState,
    currentApiKey: String,
    selectedVoice: String,
    selectedModel: String,
    selectedLanguage: String = "bn",
    selectedCharacter: com.example.data.model.AssistantCharacter = com.example.data.model.AssistantCharacter.ALINA,
    developerAvatarPath: String? = null,
    isAccessibilityEnabled: Boolean = false,
    isDarkMode: Boolean = true,
    onToggleDarkMode: () -> Unit = {},
    onOpenDeveloperProfile: () -> Unit = {},
    onCharacterChange: (com.example.data.model.AssistantCharacter) -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    onApiKeyChange: (String) -> Unit,
    onVoiceChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit = {},
    customPrompt: String = "",
    onCustomPromptSave: (String) -> Unit = {},
    onCustomPromptReset: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var showPassword by remember { mutableStateOf(false) }

    val languages = listOf(
        "bn" to ("বাংলা 🇧🇩" to "বন্ধুত্বপূর্ণ ও আন্তরিক কথোপকথন"),
        "en" to ("English 🌐" to "Friendly and smart conversational AI")
    )

    val models = listOf(
        "gemini-2.0-flash-exp" to "Gemini 2.0 Flash Live (Fastest)",
        "gemini-2.5-flash-native-audio-preview-12-2025" to "Gemini 2.5 Native Audio",
        "gemini-3.1-flash-live-preview" to "Gemini 3.1 Flash Live Preview"
    )

    val surfaceBg = if (isDarkMode) ObsidianSurface else Color.White
    val cardBg = if (isDarkMode) ObsidianBg else Color(0xFFF1F5F9)
    val cardBorder = if (isDarkMode) ObsidianBorder else Color(0xFFCBD5E1)
    val mainTextColor = if (isDarkMode) TextPrimary else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) TextSecondary else Color(0xFF475569)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceBg,
        scrimColor = Color.Black.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
                .testTag("settings_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "System Settings & Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainTextColor
                    )
                    Text(
                        text = "Gemini Live Audio & Jarvis OS",
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = subTextColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DEVELOPER PROFILE CARD BANNER
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onDismiss()
                        onOpenDeveloperProfile()
                    }
                    .testTag("settings_developer_profile_card"),
                color = if (isDarkMode) Color(0xFF111827) else Color(0xFFEEF2F6),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val devBitmap = remember(developerAvatarPath) {
                        if (!developerAvatarPath.isNullOrEmpty()) {
                            try {
                                val file = File(developerAvatarPath)
                                if (file.exists() && file.length() > 0) {
                                    BitmapFactory.decodeFile(developerAvatarPath)?.asImageBitmap()
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                    }

                    if (devBitmap != null) {
                        Image(
                            bitmap = devBitmap,
                            contentDescription = "Developer Md. Alamin",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(1.5.dp, NeonCyan), CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_developer_portrait),
                            contentDescription = "Developer Md. Alamin",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(1.5.dp, NeonCyan), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "👨‍💻 মোঃ আলামিন (স্যার)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = mainTextColor
                            )
                        }
                        Text(
                            text = "Ethical Hacker • Mini Programmer • Faridpur",
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                        Text(
                            text = "১৬ বছর • দশম শ্রেণি • সাইবার সিকিউরিটি স্পেশালিস্ট",
                            fontSize = 10.5.sp,
                            color = subTextColor
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonCyan.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "প্রোফাইল ↗",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // THEME MODE TOGGLE CARD (DARK / LIGHT)
            Text(
                text = "DISPLAY THEME / থিম নির্বাচন",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = subTextColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dark Mode Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { if (!isDarkMode) onToggleDarkMode() }
                        .testTag("dark_mode_option"),
                    color = if (isDarkMode) Color(0xFF1E293B) else cardBg,
                    border = BorderStroke(if (isDarkMode) 2.dp else 1.dp, if (isDarkMode) NeonCyan else cardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Dark",
                            tint = if (isDarkMode) NeonCyan else subTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ডার্ক মোড 🌙",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) NeonCyan else mainTextColor
                            )
                            Text(
                                text = "Cyber HUD",
                                fontSize = 10.sp,
                                color = subTextColor
                            )
                        }
                    }
                }

                // Light Mode Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { if (isDarkMode) onToggleDarkMode() }
                        .testTag("light_mode_option"),
                    color = if (!isDarkMode) Color(0xFFE2E8F0) else cardBg,
                    border = BorderStroke(if (!isDarkMode) 2.dp else 1.dp, if (!isDarkMode) NeonMagenta else cardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = "Light",
                            tint = if (!isDarkMode) NeonMagenta else subTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "লাইট মোড ☀️",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isDarkMode) NeonMagenta else mainTextColor
                            )
                            Text(
                                text = "Cyber Frost",
                                fontSize = 10.sp,
                                color = subTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. CHARACTER SELECTION (ALINA VS BLEVER)
            Text(
                text = "AI CHARACTER / কারেক্টার পছন্দ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ALINA CARD
                val isAlina = selectedCharacter == com.example.data.model.AssistantCharacter.ALINA
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCharacterChange(com.example.data.model.AssistantCharacter.ALINA) }
                        .testTag("character_alina_button"),
                    color = if (isAlina) NeonMagenta.copy(alpha = 0.15f) else cardBg,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isAlina) 2.dp else 1.dp,
                        if (isAlina) NeonMagenta else cardBorder
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_alina_portrait),
                            contentDescription = "Alina Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(1.5.dp, if (isAlina) NeonMagenta else cardBorder), CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ALINA (আলীনা)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAlina) NeonMagenta else mainTextColor
                        )
                        Text(
                            text = "স্মার্ট কিউট বন্ধু",
                            fontSize = 11.sp,
                            color = if (isAlina) mainTextColor else subTextColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (isAlina) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeonMagenta.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "✓ সক্রিয়",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonMagenta,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // BLEVER CARD
                val isBlever = selectedCharacter == com.example.data.model.AssistantCharacter.BLEVER
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCharacterChange(com.example.data.model.AssistantCharacter.BLEVER) }
                        .testTag("character_blever_button"),
                    color = if (isBlever) NeonCyan.copy(alpha = 0.15f) else cardBg,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isBlever) 2.dp else 1.dp,
                        if (isBlever) NeonCyan else cardBorder
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_blever_portrait),
                            contentDescription = "Blever Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(1.5.dp, if (isBlever) NeonCyan else cardBorder), CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "BLEVER (ব্লেভার)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBlever) NeonCyan else mainTextColor
                        )
                        Text(
                            text = "প্রফেশনাল ইন্টেলিজেন্ট",
                            fontSize = 11.sp,
                            color = if (isBlever) mainTextColor else subTextColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (isBlever) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeonCyan.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "✓ সক্রিয়",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1B. CUSTOM SYSTEM PROMPT PER CHARACTER
            var promptInput by remember(selectedCharacter, customPrompt) { mutableStateOf(customPrompt) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SYSTEM PROMPT / সিস্টেম প্রম্পট (${selectedCharacter.nameEn})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = subTextColor
                )
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 12,
                    placeholder = { Text("খালি রাখলে ডিফল্ট প্রম্পট ব্যবহার হবে। এখানে নিজের মতো করে লিখতে পারো...") }
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeonCyan.copy(alpha = 0.2f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCustomPromptSave(promptInput) }
                ) {
                    Text(
                        text = "সংরক্ষণ করুন / Save",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            promptInput = ""
                            onCustomPromptReset()
                        }
                ) {
                    Text(
                        text = "ডিফল্টে ফিরুন / Reset to default",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        color = mainTextColor
                    )
                }
                Text(
                    text = "পরিবর্তন পরের কথোপকথন (সেশন) শুরু হলে কার্যকর হবে।",
                    fontSize = 11.sp,
                    color = subTextColor
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 2. JARVIS ACCESSIBILITY SERVICE STATUS
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ObsidianBg,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🦾 Jarvis System Control",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isAccessibilityEnabled) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFFFF9100).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isAccessibilityEnabled) "চালু আছে" else "অনুমতি দিন",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAccessibilityEnabled) Color(0xFF00E676) else Color(0xFFFF9100),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "স্ক্রিন পড়া, ক্লিক, স্ক্রল, অ্যাপ খোলা ও টাইপ করার জন্য পারমিশন প্রয়োজন",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPurple.copy(alpha = 0.4f),
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("সেটিংস", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // API Key Section
            Text(
                text = "GEMINI API KEY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    onApiKeyChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input"),
                placeholder = { Text("Paste your Gemini API key", color = TextMuted) },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle key visibility",
                            tint = TextSecondary
                        )
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null, tint = NeonCyan)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianBg,
                    unfocusedContainerColor = ObsidianBg
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Configured via Secrets panel or direct key. Audio is transmitted via secure WebSocket (WSS).",
                fontSize = 11.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Language Selection
            Text(
                text = "LANGUAGE / ভাষা পছন্দ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { (code, info) ->
                    val (title, subtitle) = info
                    val isSelected = code == selectedLanguage
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLanguageChange(code) },
                        color = if (isSelected) ObsidianSurfaceVariant else ObsidianBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NeonMagenta else ObsidianBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) NeonMagenta else TextPrimary
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = NeonMagenta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Voice Selection
            Text(
                text = "ASSISTANT VOICE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AvailableVoices.forEach { voice ->
                    val isSelected = voice.id == selectedVoice
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onVoiceChange(voice.id) },
                        color = if (isSelected) ObsidianSurfaceVariant else ObsidianBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NeonMagenta else ObsidianBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = if (isSelected) NeonMagenta else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = voice.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) NeonMagenta else TextPrimary
                                )
                                Text(
                                    text = voice.description,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = NeonMagenta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Model Selection
            Text(
                text = "LIVE MODEL SELECTION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                models.forEach { (modelId, label) ->
                    val isSelected = modelId == selectedModel
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onModelChange(modelId) },
                        color = if (isSelected) ObsidianSurfaceVariant else ObsidianBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NeonCyan else ObsidianBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = if (isSelected) NeonCyan else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) NeonCyan else TextPrimary
                                )
                                Text(
                                    text = modelId,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Done Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonMagenta,
                    contentColor = ObsidianBg
                )
            ) {
                Text(
                    text = "SAVE & CLOSE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
