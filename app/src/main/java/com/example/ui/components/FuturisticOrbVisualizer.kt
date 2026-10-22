package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FuturisticOrbVisualizer(
    state: AssistantState,
    micAmplitude: Float,
    speakerAmplitude: Float,
    modifier: Modifier = Modifier,
    character: com.example.data.model.AssistantCharacter = com.example.data.model.AssistantCharacter.ALINA
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    // Continuous rotation angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse for idle state
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Secondary counter-rotation
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // Choose active amplitude based on state
    val activeAmp = when (state) {
        AssistantState.SPEAKING -> (speakerAmplitude * 1.6f).coerceIn(0.1f, 1f)
        AssistantState.LISTENING -> (micAmplitude * 1.5f).coerceIn(0.05f, 1f)
        AssistantState.CONNECTING -> 0.35f
        AssistantState.IDLE -> 0.05f
        AssistantState.ERROR -> 0.2f
    }

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2.8f)

            // Dynamic colors according to state
            val (primaryColor, secondaryColor, coreColor) = when (state) {
                AssistantState.IDLE -> Triple(
                    NeonPurple.copy(alpha = 0.6f),
                    NeonMagenta.copy(alpha = 0.3f),
                    Color(0xFF28124D)
                )
                AssistantState.CONNECTING -> Triple(
                    NeonCyan.copy(alpha = 0.8f),
                    NeonPurple.copy(alpha = 0.7f),
                    Color(0xFF0F2B48)
                )
                AssistantState.LISTENING -> Triple(
                    NeonCyan,
                    NeonMagenta.copy(alpha = 0.8f),
                    Color(0xFF0A344A)
                )
                AssistantState.SPEAKING -> Triple(
                    NeonMagenta,
                    NeonPurple,
                    Color(0xFF4A0E32)
                )
                AssistantState.ERROR -> Triple(
                    Color(0xFFFF3366),
                    Color(0xFFFF9900),
                    Color(0xFF420B19)
                )
            }

            // 1. Ambient Outer Halo Glow
            val glowRadius = baseRadius * (1.1f + activeAmp * 0.45f) * (if (state == AssistantState.IDLE) breathingPulse else 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.45f * (0.6f + activeAmp)),
                        secondaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius * 1.4f
                ),
                radius = glowRadius * 1.4f,
                center = center
            )

            // 2. Outer Concentric Soundwave Ripples (3 rings)
            for (ring in 1..3) {
                val ringOffset = (ring * 18f) * (0.6f + activeAmp)
                val ringRadius = baseRadius + ringOffset
                val ringAlpha = (0.7f - (ring * 0.18f)).coerceIn(0.1f, 0.8f)

                drawCircle(
                    color = if (ring % 2 == 0) primaryColor.copy(alpha = ringAlpha) else secondaryColor.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(
                        width = (2f + (3 - ring) + activeAmp * 4f).dp.toPx()
                    )
                )
            }

            // 3. Rotating Cyber Arcs / Orbiting Particle Segments
            val numSegments = 16
            for (i in 0 until numSegments) {
                val startAngle = rotationAngle + (i * (360f / numSegments))
                val sweep = 12f + (activeAmp * 8f)
                val arcRadius = baseRadius + 14f + (if (i % 2 == 0) activeAmp * 15f else -activeAmp * 10f)

                drawArc(
                    color = if (i % 2 == 0) primaryColor else secondaryColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Counter-rotating inner particle rings
            val innerSegments = 12
            for (i in 0 until innerSegments) {
                val startAngle = counterRotation + (i * (360f / innerSegments))
                val arcRadius = baseRadius * 0.82f

                drawArc(
                    color = primaryColor.copy(alpha = 0.6f),
                    startAngle = startAngle,
                    sweepAngle = 16f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 5. Dense Radiant Wave Spikes (Equalizer rays shooting outwards when active)
            val numSpikes = 32
            val spikeBase = baseRadius * 0.95f
            for (i in 0 until numSpikes) {
                val angleRad = Math.toRadians((i * (360.0 / numSpikes) + rotationAngle).toDouble())
                val modulation = sin(angleRad * 3.0 + (rotationAngle * 0.05)).toFloat()
                val spikeLength = (15f + (activeAmp * 45f * (0.5f + (modulation * 0.5f))))

                val startX = center.x + (spikeBase * cos(angleRad)).toFloat()
                val startY = center.y + (spikeBase * sin(angleRad)).toFloat()
                val endX = center.x + ((spikeBase + spikeLength) * cos(angleRad)).toFloat()
                val endY = center.y + ((spikeBase + spikeLength) * sin(angleRad)).toFloat()

                drawLine(
                    brush = Brush.linearGradient(
                        listOf(primaryColor, secondaryColor.copy(alpha = 0.2f))
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 6. Glowing Energy Core backdrop
            val coreRadius = baseRadius * 0.75f * (if (state == AssistantState.IDLE) breathingPulse else (0.95f + activeAmp * 0.2f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        coreColor.copy(alpha = 0.65f),
                        Color(0xFF090D18).copy(alpha = 0.85f)
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )
        }

        // Center 3D Character Avatar (Alina in formal dress / Blever in formal suit) with dynamic lip sync
        CharacterAvatar3D(
            character = character,
            state = state,
            amplitude = if (state == AssistantState.SPEAKING) speakerAmplitude else micAmplitude
        )
    }
}
