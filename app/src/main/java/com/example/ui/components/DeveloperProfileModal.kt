package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import com.example.R
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeveloperProfileModal(
    isDarkMode: Boolean,
    customAvatarPath: String? = null,
    onUploadAvatar: (Uri) -> Boolean = { false },
    onSaveCustomAvatarPath: (String) -> Unit = {},
    onResetAvatar: () -> Unit = {},
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopyText: (String, String) -> Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // Security & Photo Picker State
    var showSecurityDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                val success = onUploadAvatar(uri)
                if (success) {
                    Toast.makeText(context, "✅ মোঃ আলামিন স্যারের ছবি সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "❌ ছবি সংরক্ষণ করতে ব্যর্থ হয়েছে!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val customBitmap = remember(customAvatarPath) {
        if (!customAvatarPath.isNullOrEmpty()) {
            try {
                val file = File(customAvatarPath)
                if (file.exists() && file.length() > 0) {
                    BitmapFactory.decodeFile(customAvatarPath)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // Cyber styling based on theme
    val bgColor = if (isDarkMode) Color(0xFF070B14) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0E1424) else Color(0xFFFFFFFF)
    val borderColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val textColor = if (isDarkMode) TextPrimary else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) TextSecondary else Color(0xFF475569)

    val transition = rememberInfiniteTransition(label = "cyber_hud")
    val hudRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_rot"
    )
    val pulseGlow by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgColor,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // 1. Futuristic Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                NeonCyan.copy(alpha = if (isDarkMode) 0.15f else 0.1f),
                                NeonPurple.copy(alpha = if (isDarkMode) 0.18f else 0.12f),
                                NeonMagenta.copy(alpha = if (isDarkMode) 0.15f else 0.1f)
                            )
                        )
                    )
                    .border(
                        BorderStroke(1.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonPurple, NeonMagenta))),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NeonGreen.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, NeonGreen)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .padding(2.dp)
                                    .background(NeonGreen, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SYSTEM ARCHITECT DOSSIER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                color = NeonCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "মোঃ আলামিন (MD. ALAMIN)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isDarkMode) Color(0x33FFFFFF) else Color(0x1A000000), CircleShape)
                            .testTag("close_dev_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Hero Developer Identity Card with Rotating Holographic HUD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar Box with Rotating Cyber Ring
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Rotating Outer HUD Ring
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .rotate(hudRotation)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                NeonCyan,
                                                Color.Transparent,
                                                NeonMagenta,
                                                Color.Transparent,
                                                NeonGreen,
                                                NeonCyan
                                            )
                                        ),
                                        style = Stroke(
                                            width = 3.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    )
                                }
                        )

                        // Real Portrait Image or Custom Uploaded Image
                        if (customBitmap != null) {
                            Image(
                                bitmap = customBitmap,
                                contentDescription = "Md. Alamin Developer Portrait",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(CircleShape)
                                    .border(BorderStroke(2.5.dp, NeonCyan), CircleShape)
                                    .clickable { showSecurityDialog = true }
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_developer_portrait),
                                contentDescription = "Md. Alamin Developer Portrait",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(CircleShape)
                                    .border(BorderStroke(2.5.dp, NeonCyan), CircleShape)
                                    .clickable { showSecurityDialog = true }
                            )
                        }

                        // Upload Camera Badge Icon at bottom right of avatar
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan,
                            border = BorderStroke(2.dp, bgColor),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { showSecurityDialog = true }
                                .testTag("upload_dev_avatar_badge")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Upload Photo",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "মোঃ আলামিন (স্যার)",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                    Text(
                        text = "Young Ethical Hacker • Mini Programmer • AI Developer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Photo Action & Password Security Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NeonCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showSecurityDialog = true }
                                .testTag("btn_upload_developer_photo")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Security",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (customAvatarPath != null) "ছবি পরিবর্তন করুন 📷" else "ছবি আপলোড করুন 📷",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }

                        if (!customAvatarPath.isNullOrEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Red.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        onResetAvatar()
                                        Toast.makeText(context, "ডিফল্ট ছবিতে রিসেট হয়েছে ↺", Toast.LENGTH_SHORT).show()
                                    }
                                    .testTag("btn_reset_developer_photo")
                            ) {
                                Text(
                                    text = "ডিফল্টে রিসেট ↺",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFF5252),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Holographic Tagline Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        DevTag(text = "🎂 ১৬ বছর বয়স", color = NeonAmber, isDark = isDarkMode)
                        Spacer(modifier = Modifier.width(6.dp))
                        DevTag(text = "🎓 দশম শ্রেণি", color = NeonPurple, isDark = isDarkMode)
                        Spacer(modifier = Modifier.width(6.dp))
                        DevTag(text = "📍 ফরিদপুর, বাংলাদেশ", color = NeonGreen, isDark = isDarkMode)
                        Spacer(modifier = Modifier.width(6.dp))
                        DevTag(text = "🛡️ White-Hat Researcher", color = NeonCyan, isDark = isDarkMode)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Bio & Ambition Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonMagenta,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "মিশন ও পরিচিতি // MISSION STATEMENT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonMagenta,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "আসসালামু আলাইকুম! আমি মোঃ আলামিন। বাখুন্ডা, গেরদা ইউনিয়ন পরিষদ, ফরিদপুর-এর একজন শিক্ষার্থী ও প্রযুক্তিপ্রেমী। আমার প্রধান স্বপ্ন হলো বিশ্বমানের একজন সাইবার সিকিউরিটি স্পেশালিস্ট হওয়া। প্রযুক্তি ও এআই দিয়ে মানুষের জীবন সহজ ও নিরাপদ করাই আমার লক্ষ্য।",
                        fontSize = 13.5.sp,
                        lineHeight = 22.sp,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location detail row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "গ্রাম: বাখুন্ডা | ইউনিয়ন: গেরদা | জেলা: ফরিদপুর | বাংলাদেশ 🇧🇩",
                            fontSize = 11.5.sp,
                            color = subTextColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Skills & Technical Prowess Matrix
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "⚡ TECHNICAL SKILLS & PROFICIENCY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SkillBar("Ethical Hacking & Cyber Security", 0.92f, NeonMagenta, isDarkMode)
                    SkillBar("Android App Dev & Kotlin Compose", 0.95f, NeonCyan, isDarkMode)
                    SkillBar("Gemini Live Voice AI & System Automation", 0.98f, NeonGreen, isDarkMode)
                    SkillBar("Linux Shell & Reverse Engineering", 0.88f, NeonAmber, isDarkMode)
                    SkillBar("Python & Cybersecurity Scripting", 0.90f, NeonPurple, isDarkMode)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Social & Direct Connection Links
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🌐 CONNECT & SOCIAL CHANNELS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // GitHub Card
                    SocialContactItem(
                        iconEmoji = "🐙",
                        title = "GitHub",
                        handle = "@Alaminvaihero",
                        desc = "গিটহাব রিপোজিটরি ও ওপেন সোর্স প্রজেক্ট",
                        accentColor = Color(0xFF6E5494),
                        isDark = isDarkMode,
                        onClick = { onOpenUrl("https://github.com/Alaminvaihero") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Telegram Card
                    SocialContactItem(
                        iconEmoji = "✈️",
                        title = "Telegram",
                        handle = "@mrvirus460",
                        desc = "সরাসরি টেলিগ্রামে যোগাযোগ করুন",
                        accentColor = Color(0xFF29B6F6),
                        isDark = isDarkMode,
                        onClick = { onOpenUrl("https://t.me/mrvirus460") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // TikTok Card
                    SocialContactItem(
                        iconEmoji = "🎵",
                        title = "TikTok",
                        handle = "@mr_virus_apk",
                        desc = "টেক ও কোডিং ভিডিও প্রোফাইল",
                        accentColor = Color(0xFFFF0050),
                        isDark = isDarkMode,
                        onClick = { onOpenUrl("https://www.tiktok.com/@mr_virus_apk") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // WhatsApp Card
                    SocialContactItem(
                        iconEmoji = "💬",
                        title = "WhatsApp",
                        handle = "+880 1336-419701",
                        desc = "অফিসিয়াল বার্তা ও মেসেজ",
                        accentColor = Color(0xFF25D366),
                        isDark = isDarkMode,
                        onClick = { onOpenUrl("https://wa.me/8801336419701") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5.5 Developer's AI Creations: Alina & Blever
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "🤖 CREATED AI COMPANIONS / আলামিনের এআই সৃষ্টি",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Alina Preview Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkMode) ObsidianBg else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_alina_portrait),
                                contentDescription = "Alina AI",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(BorderStroke(1.5.dp, NeonMagenta), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("আলীনা (ALINA)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonMagenta)
                                Text("কিউট এক্সিকিউটিভ", fontSize = 10.sp, color = subTextColor)
                            }
                        }
                    }

                    // Blever Preview Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkMode) ObsidianBg else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_blever_portrait),
                                contentDescription = "Blever AI",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(BorderStroke(1.5.dp, NeonCyan), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("ব্লেভার (BLEVER)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                Text("সাইবার ইন্টেলিজেন্ট", fontSize = 10.sp, color = subTextColor)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Action Buttons Bar (Copy Info / WhatsApp / GitHub)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val fullBio = """
                            Developer: মোঃ আলামিন (Md. Alamin Sir)
                            Role: Ethical Hacker & AI Developer
                            Age: 16 Years (Class 10)
                            Location: বাখুন্ডা, গেরদা, ফরিদপুর, বাংলাদেশ
                            GitHub: https://github.com/Alaminvaihero
                            Telegram: https://t.me/mrvirus460
                            TikTok: https://www.tiktok.com/@mr_virus_apk
                            WhatsApp: +8801336419701
                        """.trimIndent()
                        val success = onCopyText(fullBio, "Md. Alamin Developer Profile")
                        if (success) {
                            Toast.makeText(context, "ডেভেলপার তথ্য কপি করা হয়েছে! 📋", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_dev_info_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("কপি তথ্য", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }

                Button(
                    onClick = { onOpenUrl("https://wa.me/8801336419701") },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("whatsapp_dev_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp বার্তা", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Security Ethics Disclaimer Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(
                        color = if (isDarkMode) Color(0x1A00F0FF) else Color(0x0D00F0FF),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        BorderStroke(0.8.dp, NeonCyan.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛡️ White-Hat Ethics: সমস্ত গবেষণা ও ডেভেলপমেন্ট ১০০% ইতিবাচক, শিক্ষামূলক ও নিরাপত্তার স্বার্থে তৈরি।",
                    fontSize = 10.5.sp,
                    color = subTextColor,
                    lineHeight = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // 8. Cyber Security Developer Authentication Dialog
        if (showSecurityDialog) {
            AlertDialog(
                onDismissRequest = {
                    showSecurityDialog = false
                    passwordInput = ""
                    passwordError = null
                },
                containerColor = if (isDarkMode) Color(0xFF0E1424) else Color.White,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter password to upload",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                passwordError = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("••••••••") },
                            singleLine = true,
                            isError = passwordError != null,
                            supportingText = if (passwordError != null) {
                                { Text(passwordError!!, color = Color(0xFFFF5252), fontSize = 11.sp) }
                            } else null,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordVisible) "Hide" else "Show",
                                        tint = NeonCyan
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = borderColor,
                                focusedLabelColor = NeonCyan,
                                cursorColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("developer_password_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (passwordInput.trim() == "@alaminnote8#") {
                                passwordError = null
                                showSecurityDialog = false
                                passwordInput = ""
                                Toast.makeText(context, "গ্যালারি ওপেন হচ্ছে...", Toast.LENGTH_SHORT).show()
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                passwordError = "ভুল পাসওয়ার্ড!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_verify_password_upload")
                    ) {
                        Text("Upload", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSecurityDialog = false
                            passwordInput = ""
                            passwordError = null
                        }
                    ) {
                        Text("Cancel", color = subTextColor)
                    }
                }
            )
        }
    }
}

@Composable
private fun DevTag(text: String, color: Color, isDark: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = if (isDark) 0.15f else 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SkillBar(skillName: String, progress: Float, color: Color, isDark: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = skillName,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) TextPrimary else Color(0xFF1E293B)
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        )
    }
}

@Composable
private fun SocialContactItem(
    iconEmoji: String,
    title: String,
    handle: String,
    desc: String,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF131A2E) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, accentColor),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = iconEmoji, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextPrimary else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = handle,
                        fontSize = 11.sp,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = desc,
                    fontSize = 10.5.sp,
                    color = if (isDark) TextSecondary else Color(0xFF64748B)
                )
            }

            Text(
                text = "খুলুন ↗",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
