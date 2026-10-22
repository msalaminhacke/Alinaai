package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple

@Composable
fun FuturisticBackground(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "bg_anim")
    val scanlineY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Base Wallpaper Image
        Image(
            painter = painterResource(id = R.drawable.img_futuristic_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Dynamic Tint / Fog / Frosted Gradient Overlay based on Mode
        if (isDarkMode) {
            // Dark Cyber HUD Tint
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xF2060912), // Deep obsidian
                                Color(0xCC0A0F1D),
                                Color(0xE6080614)
                            )
                        )
                    )
            )

            // Subtle neon glow spots
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(200f, 300f),
                            radius = 600f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonMagenta.copy(alpha = 0.06f), Color.Transparent),
                            center = Offset(800f, 1400f),
                            radius = 700f
                        )
                    )
            )
        } else {
            // Light Cyber Frost Tint (Clean, luminous, frosted holographic)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xF5F1F5F9), // Cyber Frost White
                                Color(0xEBF8FAFC),
                                Color(0xF2E2E8F0)
                            )
                        )
                    )
            )

            // Subtle cyan daylight radiance
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(300f, 200f),
                            radius = 800f
                        )
                    )
            )
        }

        // 3. Futuristic HUD Scanlines & Cyber Grid Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Animated subtle scanline
            val lineY = scanlineY * h
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        if (isDarkMode) NeonCyan.copy(alpha = 0.18f) else NeonPurple.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, lineY),
                end = Offset(w, lineY),
                strokeWidth = 2.dp.toPx()
            )

            // Corner Cyber Reticles (+)
            val reticleColor = if (isDarkMode) NeonCyan.copy(alpha = 0.35f) else Color(0xFF64748B).copy(alpha = 0.35f)
            val arm = 14.dp.toPx()
            val inset = 18.dp.toPx()

            // Top-Left
            drawLine(reticleColor, Offset(inset, inset - arm), Offset(inset, inset + arm), 1.5.dp.toPx())
            drawLine(reticleColor, Offset(inset - arm, inset), Offset(inset + arm, inset), 1.5.dp.toPx())

            // Top-Right
            drawLine(reticleColor, Offset(w - inset, inset - arm), Offset(w - inset, inset + arm), 1.5.dp.toPx())
            drawLine(reticleColor, Offset(w - inset - arm, inset), Offset(w - inset + arm, inset), 1.5.dp.toPx())

            // Bottom-Left
            drawLine(reticleColor, Offset(inset, h - inset - arm), Offset(inset, h - inset + arm), 1.5.dp.toPx())
            drawLine(reticleColor, Offset(inset - arm, h - inset), Offset(inset + arm, h - inset), 1.5.dp.toPx())

            // Bottom-Right
            drawLine(reticleColor, Offset(w - inset, h - inset - arm), Offset(w - inset, h - inset + arm), 1.5.dp.toPx())
            drawLine(reticleColor, Offset(w - inset - arm, h - inset), Offset(w - inset + arm, h - inset), 1.5.dp.toPx())
        }

        // Foreground Content
        content()
    }
}
