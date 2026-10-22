package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface

@Composable
fun JarvisFeaturesBar(
    isSentryArmed: Boolean,
    isSentryAlarmTriggered: Boolean,
    sentryCountdown: Int,
    isWakeWordActive: Boolean,
    isBubbleActive: Boolean,
    isNotificationAnnouncerEnabled: Boolean,
    isVisionAnalyzing: Boolean,
    onToggleSentry: () -> Unit,
    onDisarmSentry: () -> Unit,
    onToggleWakeWord: () -> Unit,
    onToggleBubble: () -> Unit,
    onToggleAnnouncer: () -> Unit,
    onOpenVision: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("jarvis_features_bar")
    ) {
        // Sentry alarm banner if triggered
        AnimatedVisibility(
            visible = isSentryAlarmTriggered,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(2.dp, Color.Red), RoundedCornerShape(12.dp)),
                color = Color.Red.copy(alpha = 0.25f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "আলামিন স্যারের সেন্ট্রি অ্যালার্ম বাজছে!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Button(
                        onClick = onDisarmSentry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("থামাও", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Horizontal scrolling feature pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sentry Guard Pill
            val sentryLabel = when {
                sentryCountdown > 0 -> "আর্মিং ($sentryCountdown)..."
                isSentryArmed -> "সেন্ট্রি চালু"
                else -> "সেন্ট্রি গার্ড"
            }
            JarvisFeaturePill(
                icon = Icons.Default.Security,
                label = sentryLabel,
                isActive = isSentryArmed || sentryCountdown > 0,
                activeColor = if (isSentryAlarmTriggered) Color.Red else NeonGreen,
                inactiveColor = NeonCyan,
                onClick = onToggleSentry,
                testTag = "sentry_pill"
            )

            // Camera Vision Scanner Pill
            JarvisFeaturePill(
                icon = Icons.Default.CameraAlt,
                label = if (isVisionAnalyzing) "স্ক্যানিং..." else "ক্যামেরা ছবি",
                isActive = isVisionAnalyzing,
                activeColor = NeonMagenta,
                inactiveColor = NeonCyan,
                onClick = onOpenVision,
                testTag = "vision_pill"
            )

            // Wake Word Pill
            JarvisFeaturePill(
                icon = Icons.Default.Mic,
                label = if (isWakeWordActive) "'Hey Alina' চালু" else "ওয়েক ওয়ার্ড",
                isActive = isWakeWordActive,
                activeColor = NeonAmber,
                inactiveColor = NeonCyan,
                onClick = onToggleWakeWord,
                testTag = "wake_word_pill"
            )

            // Floating Bubble Pill
            JarvisFeaturePill(
                icon = Icons.Default.Visibility,
                label = if (isBubbleActive) "বাবল চালু" else "ফ্লোটিং বাবল",
                isActive = isBubbleActive,
                activeColor = NeonPurple,
                inactiveColor = NeonCyan,
                onClick = onToggleBubble,
                testTag = "bubble_pill"
            )

            // Notification Announcer Pill
            JarvisFeaturePill(
                icon = Icons.Default.NotificationsActive,
                label = if (isNotificationAnnouncerEnabled) "অ্যানাউন্সার চালু" else "মেসেজ স্পিকার",
                isActive = isNotificationAnnouncerEnabled,
                activeColor = NeonGreen,
                inactiveColor = NeonCyan,
                onClick = onToggleAnnouncer,
                testTag = "announcer_pill"
            )
        }
    }
}

@Composable
fun JarvisFeaturePill(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = if (isActive) activeColor else ObsidianBorder
    val bgColor = if (isActive) activeColor.copy(alpha = 0.18f) else ObsidianSurface.copy(alpha = 0.7f)
    val contentColor = if (isActive) activeColor else Color.White.copy(alpha = 0.85f)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(BorderStroke(1.2.dp, borderColor), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag(testTag),
        color = bgColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.5.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
            if (isActive) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
            }
        }
    }
}