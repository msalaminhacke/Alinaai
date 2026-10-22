package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.data.model.DeviceActionResult
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

/**
 * Large central glowing futuristic mic/power button with animated pulses.
 */
@Composable
fun CentralPowerButton(
    state: AssistantState,
    isMuted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "power_button_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) 1.25f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val buttonColor by animateColorAsState(
        targetValue = when (state) {
            AssistantState.IDLE -> NeonPurple
            AssistantState.CONNECTING -> NeonCyan
            AssistantState.LISTENING -> NeonCyan
            AssistantState.SPEAKING -> NeonMagenta
            AssistantState.ERROR -> Color(0xFFFF3366)
        },
        animationSpec = tween(400),
        label = "button_color"
    )

    val buttonBgBrush = Brush.radialGradient(
        colors = listOf(
            buttonColor.copy(alpha = 0.95f),
            buttonColor.copy(alpha = 0.6f),
            ObsidianSurface
        )
    )

    Box(
        modifier = modifier
            .size(110.dp)
            .testTag("mic_power_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        if (state != AssistantState.IDLE) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .border(2.dp, buttonColor.copy(alpha = 0.45f), CircleShape)
            )
        }

        // Main tactile button
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(
                    elevation = if (state != AssistantState.IDLE) 24.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = buttonColor,
                    spotColor = buttonColor
                )
                .clip(CircleShape)
                .background(buttonBgBrush)
                .border(2.dp, buttonColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val icon = when {
                isMuted -> Icons.Default.MicOff
                state == AssistantState.IDLE -> Icons.Default.PowerSettingsNew
                state == AssistantState.SPEAKING -> Icons.Default.GraphicEq
                else -> Icons.Default.Mic
            }

            Icon(
                imageVector = icon,
                contentDescription = "Voice Activation Button",
                tint = if (state == AssistantState.IDLE) TextPrimary else Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

/**
 * 20-bar futuristic real-time frequency equalizer.
 */
@Composable
fun LiveWaveformBarVisualizer(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val barCount = 20

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val normalizedIdx = (i.toFloat() / barCount.toFloat())
            val waveMod = (sin(normalizedIdx * 9.0 + phase).toFloat() * 0.5f + 0.5f)
            val baseHeight = when (state) {
                AssistantState.IDLE -> 4.dp
                AssistantState.CONNECTING -> (6f + waveMod * 14f).dp
                AssistantState.LISTENING -> (4f + (amplitude * 32f * waveMod)).coerceIn(4f, 40f).dp
                AssistantState.SPEAKING -> (6f + (amplitude * 36f * waveMod)).coerceIn(6f, 42f).dp
                AssistantState.ERROR -> 6.dp
            }

            val barColor = when (state) {
                AssistantState.SPEAKING -> if (i % 2 == 0) NeonMagenta else NeonPurple
                AssistantState.LISTENING -> if (i % 2 == 0) NeonCyan else NeonMagenta
                AssistantState.CONNECTING -> NeonCyan.copy(alpha = 0.7f)
                AssistantState.IDLE -> ObsidianBorder
                AssistantState.ERROR -> Color(0xFFFF3366)
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(baseHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * Sassy persona HUD ticker displaying current status and snappy attitude comments.
 */
@Composable
fun SassyPersonaTicker(
    state: AssistantState,
    quote: String,
    language: String = "bn",
    modifier: Modifier = Modifier
) {
    val isBn = (language == "bn")
    val stateText = when (state) {
        AssistantState.IDLE -> if (isBn) "অপেক্ষারত (STANDBY)" else "STANDBY"
        AssistantState.CONNECTING -> if (isBn) "লাইভ সংযোগ স্থাপন হচ্ছে..." else "LINKING NEURAL BIDI STREAM..."
        AssistantState.LISTENING -> if (isBn) "আলীনা শুনছে... বলুন!" else "ALINA LISTENING..."
        AssistantState.SPEAKING -> if (isBn) "আলীনা কথা বলছে (24kHz)" else "ALINA TRANSMITTING 24kHz"
        AssistantState.ERROR -> if (isBn) "সিস্টেম নোটিশ" else "SYSTEM NOTICE"
    }

    val stateColor = when (state) {
        AssistantState.IDLE -> TextSecondary
        AssistantState.CONNECTING -> NeonCyan
        AssistantState.LISTENING -> NeonCyan
        AssistantState.SPEAKING -> NeonMagenta
        AssistantState.ERROR -> Color(0xFFFF3366)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("persona_ticker"),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // State pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(stateColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stateText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = stateColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = quote,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Animated banner for executed device actions.
 */
@Composable
fun ActionNotificationBanner(
    action: DeviceActionResult?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = action != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        if (action != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .testTag("action_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (action.toolName) {
                            "toggleFlashlight", "Flashlight" -> Icons.Default.FlashlightOn
                            "getBatteryStatus", "Battery" -> Icons.Default.BatteryChargingFull
                            "vibrateDevice", "Vibrate" -> Icons.Default.Vibration
                            else -> Icons.Default.AutoAwesome
                        },
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = action.message,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Direct shortcut action chips for testing device actions and developer profile.
 */
@Composable
fun DeviceQuickActionBar(
    isFlashlightOn: Boolean,
    language: String = "bn",
    isDarkMode: Boolean = true,
    onTrigger: (String) -> Unit,
    onOpenDeveloper: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isBn = (language == "bn")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        ActionChip(
            icon = Icons.Default.FlashlightOn,
            label = if (isFlashlightOn) (if (isBn) "টর্চ চালু" else "Torch ON") else (if (isBn) "টর্চ" else "Torch"),
            isActive = isFlashlightOn,
            isDark = isDarkMode,
            onClick = { onTrigger("toggleFlashlight") }
        )
        ActionChip(
            icon = Icons.Default.BatteryChargingFull,
            label = if (isBn) "ব্যাটারি" else "Battery",
            isDark = isDarkMode,
            onClick = { onTrigger("getBattery") }
        )
        ActionChip(
            icon = Icons.Default.AutoAwesome,
            label = if (isBn) "বন্ধুত্ব" else "Chat",
            isDark = isDarkMode,
            onClick = { onTrigger("sassyJoke") }
        )
        // Developer Profile Quick Chip
        ActionChip(
            icon = Icons.Default.Vibration,
            label = if (isBn) "👨‍💻 আলামিন স্যার" else "👨‍💻 Dev Profile",
            isActive = true,
            isDark = isDarkMode,
            activeColor = NeonGreen,
            onClick = onOpenDeveloper
        )
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    isDark: Boolean = true,
    activeColor: Color = NeonMagenta,
    onClick: () -> Unit
) {
    val chipBg = if (isActive) {
        activeColor.copy(alpha = if (isDark) 0.22f else 0.18f)
    } else {
        if (isDark) ObsidianSurface else Color.White
    }
    val chipBorder = if (isActive) activeColor else (if (isDark) ObsidianBorder else Color(0xFFCBD5E1))
    val textColor = if (isDark) TextPrimary else Color(0xFF0F172A)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = chipBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else NeonCyan,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}
