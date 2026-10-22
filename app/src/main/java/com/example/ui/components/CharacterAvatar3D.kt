package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.AssistantCharacter
import com.example.data.model.AssistantState
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CharacterAvatar3D(
    character: AssistantCharacter,
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    var showPreviewModal by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "avatar_anim")

    // Breathing / floating vertical motion
    val floatOffset by transition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_float"
    )

    // Holographic Cyber HUD rotation
    val hudRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_rotation"
    )

    // Breathing pulse
    val breathingPulse by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val isAlina = (character == AssistantCharacter.ALINA)
    val brandColor = when (character) {
        AssistantCharacter.ALINA -> NeonMagenta
        AssistantCharacter.JARVIS -> Color(0xFFFFB300)
        else -> NeonCyan
    }
    val accentColor = if (isAlina) NeonPurple else NeonAmber

    // Speaking reactive scale
    val voiceScale = if (state == AssistantState.SPEAKING) {
        1f + (amplitude * 0.12f).coerceIn(0f, 0.15f)
    } else if (state == AssistantState.LISTENING) {
        1f + (amplitude * 0.06f).coerceIn(0f, 0.08f)
    } else {
        breathingPulse
    }

    val portraitRes = if (isAlina) R.drawable.img_alina_portrait else R.drawable.img_blever_portrait

    Box(
        modifier = modifier
            .size(190.dp)
            .testTag("character_avatar_container"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Rotating Cyber Rings around Avatar
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f + floatOffset)
            val ringRadius = size.minDimension / 2f - 6.dp.toPx()

            // Outer segmented cyber HUD ring
            val segments = 8
            for (i in 0 until segments) {
                val start = hudRotation + (i * (360f / segments))
                drawArc(
                    color = if (i % 2 == 0) brandColor.copy(alpha = 0.85f) else accentColor.copy(alpha = 0.5f),
                    startAngle = start,
                    sweepAngle = 26f,
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Speaking audio wave rings
            if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) {
                val activeRadius = ringRadius + (amplitude * 18.dp.toPx()).coerceIn(2.dp.toPx(), 14.dp.toPx())
                drawCircle(
                    color = brandColor.copy(alpha = (amplitude * 0.7f).coerceIn(0.2f, 0.8f)),
                    radius = activeRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // 2. High Resolution Circular Portrait Image
        Box(
            modifier = Modifier
                .size(165.dp)
                .offset(y = floatOffset.dp)
                .scale(voiceScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            brandColor.copy(alpha = 0.35f),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        width = if (state == AssistantState.SPEAKING) 3.dp else 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                brandColor,
                                accentColor,
                                NeonCyan,
                                brandColor
                            )
                        )
                    ),
                    shape = CircleShape
                )
                .clickable {
                    showPreviewModal = true
                }
                .testTag("avatar_portrait_circle"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = portraitRes),
                contentDescription = if (isAlina) "Alina AI Portrait" else "Blever AI Portrait",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Holographic ambient rim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                brandColor.copy(alpha = if (state == AssistantState.SPEAKING) 0.3f else 0.15f)
                            )
                        )
                    )
            )
        }

        // 3. Floating HUD Badge (bottom center of orb)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianSurface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, brandColor.copy(alpha = 0.8f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-4).dp)
                .clickable { showPreviewModal = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when (state) {
                                AssistantState.IDLE -> NeonAmber
                                AssistantState.CONNECTING -> NeonCyan
                                AssistantState.LISTENING -> NeonCyan
                                AssistantState.SPEAKING -> NeonGreen
                                AssistantState.ERROR -> Color(0xFFEF4444)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = when (character) {
                    AssistantCharacter.ALINA -> "ALINA • AI"
                    AssistantCharacter.JARVIS -> "JARVIS • AI"
                    else -> "BLEVER • AI"
                },
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = brandColor,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }

    // High-Resolution Character Profile Modal when tapped
    if (showPreviewModal) {
        Dialog(onDismissRequest = { showPreviewModal = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.5.dp, brandColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (character) {
                                AssistantCharacter.ALINA -> "ALINA AI PROFILE"
                                AssistantCharacter.JARVIS -> "JARVIS AI PROFILE"
                                else -> "BLEVER AI PROFILE"
                            },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = { showPreviewModal = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Large HD Portrait
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(BorderStroke(2.dp, brandColor), RoundedCornerShape(20.dp))
                    ) {
                        Image(
                            painter = painterResource(id = portraitRes),
                            contentDescription = "Full Portrait",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (character) {
                            AssistantCharacter.ALINA -> "আলীনা (Alina AI)"
                            AssistantCharacter.JARVIS -> "জার্ভিস (Jarvis AI)"
                            else -> "ব্লেভার (Blever AI)"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
                    )
                    Text(
                        text = if (isAlina) "এক্সিকিউটিভ এআই অ্যাসিস্ট্যান্ট • কিউট ও কেয়ারিং বন্ধু" else "সাইবারপাঙ্ক এআই অ্যাসিস্ট্যান্ট • কোডিং ও সিকিউরিটি স্পেশালিস্ট",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianBg,
                        border = BorderStroke(0.8.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "আলামিন স্যারের জন্য ডেডিকেটেড এআই",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAlina)
                                    "আলীনা স্যারের কথা মন দিয়ে শোনে, টর্চ ও ব্যাটারি কন্ট্রোল করে এবং স্যারের সকল প্রশ্নের মিষ্টি ভাষায় উত্তর দেয়।"
                                else
                                    "ব্লেভার সাইবারপাঙ্ক স্টাইলের হাই-টেক এআই। আলামিন স্যারের সাইবার সিকিউরিটি রিসার্চ, এথিক্যাল হ্যাকিং ও ডিভাইজ কমান্ডের সার্বক্ষণিক সহকারী।",
                                fontSize = 11.5.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAlina3D(
    cx: Float,
    cy: Float,
    scale: Float,
    isBlinking: Boolean,
    mouthOpen: Float,
    state: AssistantState
) {
    // 1. Back Long Hair Flow
    val backHairPath = Path().apply {
        moveTo(cx - 45f * scale, cy - 20f * scale)
        cubicTo(cx - 65f * scale, cy + 20f * scale, cx - 60f * scale, cy + 80f * scale, cx - 35f * scale, cy + 85f * scale)
        lineTo(cx + 35f * scale, cy + 85f * scale)
        cubicTo(cx + 60f * scale, cy + 80f * scale, cx + 65f * scale, cy + 20f * scale, cx + 45f * scale, cy - 20f * scale)
        close()
    }
    drawPath(
        path = backHairPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF2B1B36), Color(0xFF160E1D), Color(0xFF0F0814)),
            startY = cy - 40f * scale,
            endY = cy + 90f * scale
        )
    )

    // 2. Formal Dark Navy/Charcoal Suit Blazer Shoulders
    val suitPath = Path().apply {
        moveTo(cx - 65f * scale, cy + 85f * scale)
        cubicTo(cx - 50f * scale, cy + 42f * scale, cx - 22f * scale, cy + 34f * scale, cx - 14f * scale, cy + 34f * scale)
        lineTo(cx + 14f * scale, cy + 34f * scale)
        cubicTo(cx + 22f * scale, cy + 34f * scale, cx + 50f * scale, cy + 42f * scale, cx + 65f * scale, cy + 85f * scale)
        lineTo(cx - 65f * scale, cy + 85f * scale)
        close()
    }
    drawPath(
        path = suitPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E243A), Color(0xFF121624), Color(0xFF0B0E18)),
            startY = cy + 30f * scale,
            endY = cy + 90f * scale
        )
    )

    // 3. Crisp White Collared Dress Shirt
    val shirtPath = Path().apply {
        moveTo(cx - 16f * scale, cy + 34f * scale)
        lineTo(cx, cy + 62f * scale)
        lineTo(cx + 16f * scale, cy + 34f * scale)
        lineTo(cx + 10f * scale, cy + 28f * scale)
        lineTo(cx - 10f * scale, cy + 28f * scale)
        close()
    }
    drawPath(
        path = shirtPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFD6DBE8)),
            startY = cy + 28f * scale,
            endY = cy + 62f * scale
        )
    )

    // 4. Elegant Lapels & Ribbon Tie
    val leftLapel = Path().apply {
        moveTo(cx - 22f * scale, cy + 34f * scale)
        lineTo(cx - 6f * scale, cy + 64f * scale)
        lineTo(cx - 16f * scale, cy + 70f * scale)
        lineTo(cx - 32f * scale, cy + 42f * scale)
        close()
    }
    val rightLapel = Path().apply {
        moveTo(cx + 22f * scale, cy + 34f * scale)
        lineTo(cx + 6f * scale, cy + 64f * scale)
        lineTo(cx + 16f * scale, cy + 70f * scale)
        lineTo(cx + 32f * scale, cy + 42f * scale)
        close()
    }
    drawPath(leftLapel, Brush.horizontalGradient(listOf(Color(0xFF181C2B), Color(0xFF262D45))))
    drawPath(rightLapel, Brush.horizontalGradient(listOf(Color(0xFF262D45), Color(0xFF181C2B))))

    // Silk Magenta/Cyan Ribbon Brooch at Collar
    drawCircle(
        color = Color(0xFFFF2A85),
        radius = 4f * scale,
        center = Offset(cx, cy + 40f * scale)
    )
    drawCircle(
        color = Color(0xFF00E5FF),
        radius = 2f * scale,
        center = Offset(cx, cy + 40f * scale)
    )

    // 5. Neck
    val neckPath = Path().apply {
        moveTo(cx - 11f * scale, cy + 14f * scale)
        lineTo(cx - 11f * scale, cy + 32f * scale)
        lineTo(cx + 11f * scale, cy + 32f * scale)
        lineTo(cx + 11f * scale, cy + 14f * scale)
        close()
    }
    drawPath(
        path = neckPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFD1BD), Color(0xFFE8B29C)),
            startY = cy + 14f * scale,
            endY = cy + 32f * scale
        )
    )

    // 6. Cute Head & Face with 3D Depth
    val headCenter = Offset(cx, cy - 8f * scale)
    val headRadiusX = 33f * scale
    val headRadiusY = 37f * scale
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF0EA), Color(0xFFFFDEC7), Color(0xFFF3C4AA)),
            center = Offset(cx - 4f * scale, cy - 12f * scale),
            radius = 45f * scale
        ),
        topLeft = Offset(headCenter.x - headRadiusX, headCenter.y - headRadiusY),
        size = Size(headRadiusX * 2, headRadiusY * 2)
    )

    // Soft Cheek Blush
    drawOval(
        color = Color(0xFFFF4081).copy(alpha = 0.35f),
        topLeft = Offset(cx - 26f * scale, cy - 2f * scale),
        size = Size(10f * scale, 6f * scale)
    )
    drawOval(
        color = Color(0xFFFF4081).copy(alpha = 0.35f),
        topLeft = Offset(cx + 16f * scale, cy - 2f * scale),
        size = Size(10f * scale, 6f * scale)
    )

    // 7. Expressive 3D Eyes with Specular Highlights
    val leftEyeCenter = Offset(cx - 14f * scale, cy - 8f * scale)
    val rightEyeCenter = Offset(cx + 14f * scale, cy - 8f * scale)

    if (isBlinking) {
        // Cute arched blink lines
        drawArc(
            color = Color(0xFF381E2B),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(leftEyeCenter.x - 7f * scale, leftEyeCenter.y - 3f * scale),
            size = Size(14f * scale, 6f * scale),
            style = Stroke(width = 2.5f * scale, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF381E2B),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(rightEyeCenter.x - 7f * scale, rightEyeCenter.y - 3f * scale),
            size = Size(14f * scale, 6f * scale),
            style = Stroke(width = 2.5f * scale, cap = StrokeCap.Round)
        )
    } else {
        // Left Eye (Anime/3D style gradient iris with double catchlights)
        drawOval(
            color = Color.White,
            topLeft = Offset(leftEyeCenter.x - 7f * scale, leftEyeCenter.y - 9f * scale),
            size = Size(14f * scale, 17f * scale)
        )
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF6B1D4B), Color(0xFFC2185B), Color(0xFFFF4081)),
                startY = leftEyeCenter.y - 8f * scale,
                endY = leftEyeCenter.y + 7f * scale
            ),
            topLeft = Offset(leftEyeCenter.x - 5.5f * scale, leftEyeCenter.y - 7f * scale),
            size = Size(11f * scale, 14f * scale)
        )
        drawCircle(Color(0xFF1E0A16), 3.5f * scale, leftEyeCenter)
        // Specular reflections
        drawCircle(Color.White, 2.2f * scale, Offset(leftEyeCenter.x - 2f * scale, leftEyeCenter.y - 3f * scale))
        drawCircle(Color.White, 1.2f * scale, Offset(leftEyeCenter.x + 2f * scale, leftEyeCenter.y + 2f * scale))

        // Right Eye
        drawOval(
            color = Color.White,
            topLeft = Offset(rightEyeCenter.x - 7f * scale, rightEyeCenter.y - 9f * scale),
            size = Size(14f * scale, 17f * scale)
        )
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF6B1D4B), Color(0xFFC2185B), Color(0xFFFF4081)),
                startY = rightEyeCenter.y - 8f * scale,
                endY = rightEyeCenter.y + 7f * scale
            ),
            topLeft = Offset(rightEyeCenter.x - 5.5f * scale, rightEyeCenter.y - 7f * scale),
            size = Size(11f * scale, 14f * scale)
        )
        drawCircle(Color(0xFF1E0A16), 3.5f * scale, rightEyeCenter)
        // Specular reflections
        drawCircle(Color.White, 2.2f * scale, Offset(rightEyeCenter.x - 2f * scale, rightEyeCenter.y - 3f * scale))
        drawCircle(Color.White, 1.2f * scale, Offset(rightEyeCenter.x + 2f * scale, rightEyeCenter.y + 2f * scale))

        // Upper Eyelashes
        drawArc(
            color = Color(0xFF2C1623),
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(leftEyeCenter.x - 7f * scale, leftEyeCenter.y - 11f * scale),
            size = Size(14f * scale, 6f * scale),
            style = Stroke(width = 2f * scale, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF2C1623),
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(rightEyeCenter.x - 7f * scale, rightEyeCenter.y - 11f * scale),
            size = Size(14f * scale, 6f * scale),
            style = Stroke(width = 2f * scale, cap = StrokeCap.Round)
        )
    }

    // Eyebrows
    drawArc(
        color = Color(0xFF4A2A3E),
        startAngle = 200f,
        sweepAngle = 135f,
        useCenter = false,
        topLeft = Offset(cx - 20f * scale, cy - 20f * scale),
        size = Size(13f * scale, 5f * scale),
        style = Stroke(width = 2f * scale, cap = StrokeCap.Round)
    )
    drawArc(
        color = Color(0xFF4A2A3E),
        startAngle = 205f,
        sweepAngle = 135f,
        useCenter = false,
        topLeft = Offset(cx + 7f * scale, cy - 20f * scale),
        size = Size(13f * scale, 5f * scale),
        style = Stroke(width = 2f * scale, cap = StrokeCap.Round)
    )

    // 8. Cute Animated Mouth (Opens with Speech Amplitude)
    val mouthY = cy + 12f * scale
    if (mouthOpen > 0.08f) {
        val mouthHeight = (4f + mouthOpen * 14f) * scale
        val mouthWidth = (10f + mouthOpen * 8f) * scale
        // Open mouth oval
        drawOval(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF880E4F), Color(0xFFD81B60), Color(0xFFFF80AB)),
                startY = mouthY - mouthHeight / 2f,
                endY = mouthY + mouthHeight / 2f
            ),
            topLeft = Offset(cx - mouthWidth / 2f, mouthY - mouthHeight / 2f),
            size = Size(mouthWidth, mouthHeight)
        )
        // Little white tooth bar
        if (mouthOpen > 0.35f) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cx - mouthWidth * 0.35f, mouthY - mouthHeight * 0.45f),
                size = Size(mouthWidth * 0.7f, 3.5f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
        }
    } else {
        // Cute closed friendly smile curve
        drawArc(
            color = Color(0xFFC2185B),
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(cx - 7f * scale, mouthY - 4f * scale),
            size = Size(14f * scale, 7f * scale),
            style = Stroke(width = 2.2f * scale, cap = StrokeCap.Round)
        )
    }

    // 9. Front Styled Hair Bangs with Highlights
    val bangsPath = Path().apply {
        moveTo(cx - 38f * scale, cy - 24f * scale)
        cubicTo(cx - 28f * scale, cy - 42f * scale, cx + 28f * scale, cy - 42f * scale, cx + 38f * scale, cy - 24f * scale)
        cubicTo(cx + 34f * scale, cy - 6f * scale, cx + 25f * scale, cy + 6f * scale, cx + 22f * scale, cy - 8f * scale)
        cubicTo(cx + 14f * scale, cy - 4f * scale, cx + 6f * scale, cy - 6f * scale, cx + 2f * scale, cy - 14f * scale)
        cubicTo(cx - 8f * scale, cy - 4f * scale, cx - 18f * scale, cy - 6f * scale, cx - 24f * scale, cy - 12f * scale)
        cubicTo(cx - 32f * scale, cy + 4f * scale, cx - 36f * scale, cy - 8f * scale, cx - 38f * scale, cy - 24f * scale)
        close()
    }
    drawPath(
        path = bangsPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF3F274E), Color(0xFF281733), Color(0xFF1A0F21)),
            startY = cy - 44f * scale,
            endY = cy - 4f * scale
        )
    )

    // Hair gloss highlight arc
    drawArc(
        color = Color(0xFFBA68C8).copy(alpha = 0.5f),
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - 24f * scale, cy - 36f * scale),
        size = Size(48f * scale, 12f * scale),
        style = Stroke(width = 3f * scale, cap = StrokeCap.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlever3D(
    cx: Float,
    cy: Float,
    scale: Float,
    isBlinking: Boolean,
    mouthOpen: Float,
    state: AssistantState
) {
    // 1. Back Cool Hair Strands
    val backHairPath = Path().apply {
        moveTo(cx - 44f * scale, cy - 20f * scale)
        cubicTo(cx - 52f * scale, cy - 10f * scale, cx - 48f * scale, cy + 22f * scale, cx - 32f * scale, cy + 30f * scale)
        lineTo(cx + 32f * scale, cy + 30f * scale)
        cubicTo(cx + 48f * scale, cy + 22f * scale, cx + 52f * scale, cy - 10f * scale, cx + 44f * scale, cy - 20f * scale)
        close()
    }
    drawPath(
        path = backHairPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1B2A38), Color(0xFF0F1A24)),
            startY = cy - 30f * scale,
            endY = cy + 30f * scale
        )
    )

    // 2. Formal Tech Charcoal Suit Blazer Shoulders
    val suitPath = Path().apply {
        moveTo(cx - 68f * scale, cy + 85f * scale)
        cubicTo(cx - 52f * scale, cy + 40f * scale, cx - 24f * scale, cy + 34f * scale, cx - 14f * scale, cy + 34f * scale)
        lineTo(cx + 14f * scale, cy + 34f * scale)
        cubicTo(cx + 24f * scale, cy + 34f * scale, cx + 52f * scale, cy + 40f * scale, cx + 68f * scale, cy + 85f * scale)
        lineTo(cx - 68f * scale, cy + 85f * scale)
        close()
    }
    drawPath(
        path = suitPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF242A36), Color(0xFF151921), Color(0xFF0D0F14)),
            startY = cy + 30f * scale,
            endY = cy + 90f * scale
        )
    )

    // 3. Crisp White Dress Shirt
    val shirtPath = Path().apply {
        moveTo(cx - 16f * scale, cy + 34f * scale)
        lineTo(cx, cy + 62f * scale)
        lineTo(cx + 16f * scale, cy + 34f * scale)
        lineTo(cx + 11f * scale, cy + 28f * scale)
        lineTo(cx - 11f * scale, cy + 28f * scale)
        close()
    }
    drawPath(
        path = shirtPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFCFD8DC)),
            startY = cy + 28f * scale,
            endY = cy + 62f * scale
        )
    )

    // 4. Cool Tech Tie (Cyan/Blue Formal Tie)
    val tiePath = Path().apply {
        moveTo(cx - 5f * scale, cy + 38f * scale)
        lineTo(cx + 5f * scale, cy + 38f * scale)
        lineTo(cx + 7f * scale, cy + 70f * scale)
        lineTo(cx, cy + 80f * scale)
        lineTo(cx - 7f * scale, cy + 70f * scale)
        close()
    }
    drawPath(
        path = tiePath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF00B0FF), Color(0xFF0077C2), Color(0xFF004D73)),
            startY = cy + 38f * scale,
            endY = cy + 80f * scale
        )
    )
    // Metallic tie clip
    drawLine(
        color = Color(0xFFE0E0E0),
        start = Offset(cx - 6f * scale, cy + 54f * scale),
        end = Offset(cx + 4f * scale, cy + 54f * scale),
        strokeWidth = 2.5f * scale,
        cap = StrokeCap.Round
    )

    // Sharp Suit Lapels
    val leftLapel = Path().apply {
        moveTo(cx - 24f * scale, cy + 34f * scale)
        lineTo(cx - 7f * scale, cy + 65f * scale)
        lineTo(cx - 18f * scale, cy + 72f * scale)
        lineTo(cx - 36f * scale, cy + 42f * scale)
        close()
    }
    val rightLapel = Path().apply {
        moveTo(cx + 24f * scale, cy + 34f * scale)
        lineTo(cx + 7f * scale, cy + 65f * scale)
        lineTo(cx + 18f * scale, cy + 72f * scale)
        lineTo(cx + 36f * scale, cy + 42f * scale)
        close()
    }
    drawPath(leftLapel, Brush.horizontalGradient(listOf(Color(0xFF1B202A), Color(0xFF2C3445))))
    drawPath(rightLapel, Brush.horizontalGradient(listOf(Color(0xFF2C3445), Color(0xFF1B202A))))

    // 5. Boy Neck
    val neckPath = Path().apply {
        moveTo(cx - 12f * scale, cy + 14f * scale)
        lineTo(cx - 12f * scale, cy + 32f * scale)
        lineTo(cx + 12f * scale, cy + 32f * scale)
        lineTo(cx + 12f * scale, cy + 14f * scale)
        close()
    }
    drawPath(
        path = neckPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFD4C2), Color(0xFFE8B29C)),
            startY = cy + 14f * scale,
            endY = cy + 32f * scale
        )
    )

    // 6. Boy Head & Jawline with 3D Depth
    val headCenter = Offset(cx, cy - 8f * scale)
    val headRadiusX = 33f * scale
    val headRadiusY = 37f * scale
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF0EA), Color(0xFFFFDFC8), Color(0xFFEEBFA5)),
            center = Offset(cx - 3f * scale, cy - 10f * scale),
            radius = 45f * scale
        ),
        topLeft = Offset(headCenter.x - headRadiusX, headCenter.y - headRadiusY),
        size = Size(headRadiusX * 2, headRadiusY * 2)
    )

    // 7. Confident Smart Eyes
    val leftEyeCenter = Offset(cx - 14f * scale, cy - 7f * scale)
    val rightEyeCenter = Offset(cx + 14f * scale, cy - 7f * scale)

    if (isBlinking) {
        drawLine(
            color = Color(0xFF1C2733),
            start = Offset(leftEyeCenter.x - 7f * scale, leftEyeCenter.y),
            end = Offset(leftEyeCenter.x + 7f * scale, leftEyeCenter.y),
            strokeWidth = 2.5f * scale,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF1C2733),
            start = Offset(rightEyeCenter.x - 7f * scale, rightEyeCenter.y),
            end = Offset(rightEyeCenter.x + 7f * scale, rightEyeCenter.y),
            strokeWidth = 2.5f * scale,
            cap = StrokeCap.Round
        )
    } else {
        // Left Eye (Cyan / Tech Blue iris)
        drawOval(
            color = Color.White,
            topLeft = Offset(leftEyeCenter.x - 7f * scale, leftEyeCenter.y - 7f * scale),
            size = Size(14f * scale, 14f * scale)
        )
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0D47A1), Color(0xFF0091EA), Color(0xFF00E5FF)),
                startY = leftEyeCenter.y - 6f * scale,
                endY = leftEyeCenter.y + 6f * scale
            ),
            topLeft = Offset(leftEyeCenter.x - 5f * scale, leftEyeCenter.y - 5.5f * scale),
            size = Size(10f * scale, 11f * scale)
        )
        drawCircle(Color(0xFF0A192F), 3.2f * scale, leftEyeCenter)
        drawCircle(Color.White, 2f * scale, Offset(leftEyeCenter.x - 1.8f * scale, leftEyeCenter.y - 2f * scale))

        // Right Eye
        drawOval(
            color = Color.White,
            topLeft = Offset(rightEyeCenter.x - 7f * scale, rightEyeCenter.y - 7f * scale),
            size = Size(14f * scale, 14f * scale)
        )
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0D47A1), Color(0xFF0091EA), Color(0xFF00E5FF)),
                startY = rightEyeCenter.y - 6f * scale,
                endY = rightEyeCenter.y + 6f * scale
            ),
            topLeft = Offset(rightEyeCenter.x - 5f * scale, rightEyeCenter.y - 5.5f * scale),
            size = Size(10f * scale, 11f * scale)
        )
        drawCircle(Color(0xFF0A192F), 3.2f * scale, rightEyeCenter)
        drawCircle(Color.White, 2f * scale, Offset(rightEyeCenter.x - 1.8f * scale, rightEyeCenter.y - 2f * scale))

        // Confident upper eyelid line
        drawLine(
            color = Color(0xFF1A2634),
            start = Offset(leftEyeCenter.x - 8f * scale, leftEyeCenter.y - 8f * scale),
            end = Offset(leftEyeCenter.x + 7f * scale, leftEyeCenter.y - 6f * scale),
            strokeWidth = 2.4f * scale,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF1A2634),
            start = Offset(rightEyeCenter.x - 7f * scale, rightEyeCenter.y - 6f * scale),
            end = Offset(rightEyeCenter.x + 8f * scale, rightEyeCenter.y - 8f * scale),
            strokeWidth = 2.4f * scale,
            cap = StrokeCap.Round
        )
    }

    // Bold Sharp Eyebrows
    drawLine(
        color = Color(0xFF263238),
        start = Offset(cx - 22f * scale, cy - 18f * scale),
        end = Offset(cx - 6f * scale, cy - 15f * scale),
        strokeWidth = 2.8f * scale,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF263238),
        start = Offset(cx + 6f * scale, cy - 15f * scale),
        end = Offset(cx + 22f * scale, cy - 18f * scale),
        strokeWidth = 2.8f * scale,
        cap = StrokeCap.Round
    )

    // 8. Animated Boy Mouth (Opens with Speech Amplitude)
    val mouthY = cy + 12f * scale
    if (mouthOpen > 0.08f) {
        val mouthHeight = (4f + mouthOpen * 14f) * scale
        val mouthWidth = (11f + mouthOpen * 9f) * scale
        drawOval(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF78281F), Color(0xFFC0392B), Color(0xFFE74C3C)),
                startY = mouthY - mouthHeight / 2f,
                endY = mouthY + mouthHeight / 2f
            ),
            topLeft = Offset(cx - mouthWidth / 2f, mouthY - mouthHeight / 2f),
            size = Size(mouthWidth, mouthHeight)
        )
        if (mouthOpen > 0.35f) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cx - mouthWidth * 0.35f, mouthY - mouthHeight * 0.45f),
                size = Size(mouthWidth * 0.7f, 3.5f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
        }
    } else {
        // Confident smirk / smile
        drawArc(
            color = Color(0xFF9E2A2B),
            startAngle = 10f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(cx - 8f * scale, mouthY - 4f * scale),
            size = Size(16f * scale, 7f * scale),
            style = Stroke(width = 2.4f * scale, cap = StrokeCap.Round)
        )
    }

    // 9. Cool Boy Hairstyle (Side-Swept Bangs with Cyan Cyber Streaks)
    val boyHairPath = Path().apply {
        moveTo(cx - 40f * scale, cy - 22f * scale)
        cubicTo(cx - 36f * scale, cy - 48f * scale, cx + 22f * scale, cy - 48f * scale, cx + 40f * scale, cy - 26f * scale)
        cubicTo(cx + 34f * scale, cy - 12f * scale, cx + 26f * scale, cy + 2f * scale, cx + 22f * scale, cy - 10f * scale)
        // Textured side swept locks
        lineTo(cx + 8f * scale, cy - 14f * scale)
        lineTo(cx + 4f * scale, cy - 6f * scale)
        lineTo(cx - 8f * scale, cy - 14f * scale)
        lineTo(cx - 14f * scale, cy - 8f * scale)
        lineTo(cx - 26f * scale, cy - 14f * scale)
        cubicTo(cx - 35f * scale, cy - 4f * scale, cx - 38f * scale, cy - 12f * scale, cx - 40f * scale, cy - 22f * scale)
        close()
    }
    drawPath(
        path = boyHairPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF263238), Color(0xFF1E272C), Color(0xFF10171B)),
            startY = cy - 50f * scale,
            endY = cy - 6f * scale
        )
    )

    // Cyber Cyan Highlight Locks
    val highlightPath = Path().apply {
        moveTo(cx - 24f * scale, cy - 40f * scale)
        cubicTo(cx - 10f * scale, cy - 45f * scale, cx + 18f * scale, cy - 42f * scale, cx + 28f * scale, cy - 30f * scale)
    }
    drawPath(
        path = highlightPath,
        color = Color(0xFF00E5FF).copy(alpha = 0.6f),
        style = Stroke(width = 2.8f * scale, cap = StrokeCap.Round)
    )
}
