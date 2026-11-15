package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.WallpaperData
import com.example.ui.theme.*

@Composable
fun AdtAvatar(
    name: String,
    avatarKey: String = "",
    sizeDp: Int = 48,
    isOnline: Boolean = false,
    hasStatusStory: Boolean = false,
    modifier: Modifier = Modifier
) {
    val initial = name.trim().take(1).uppercase().ifBlank { "A" }
    
    // Distinct color palette based on name hash
    val colorPair = when (kotlin.math.abs(name.hashCode()) % 5) {
        0 -> Color(0xFF00A884) to Color(0xFF075E54)
        1 -> Color(0xFF128C7E) to Color(0xFF054C44)
        2 -> Color(0xFFFF7900) to Color(0xFFB35400)
        3 -> Color(0xFF0072CE) to Color(0xFF004987)
        else -> Color(0xFF7C4DFF) to Color(0xFF4A148C)
    }

    Box(modifier = modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        // Status story ring if any
        if (hasStatusStory) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.5.dp, AdtPrimary, CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size((if (hasStatusStory) sizeDp - 6 else sizeDp).dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colorPair.first, colorPair.second))),
            contentAlignment = Alignment.Center
        ) {
            if (avatarKey.isNotBlank()) {
                AsyncImage(
                    model = avatarKey,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = {
                        // Fallback text if loading fails
                    }
                )
            } else {
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = (sizeDp * 0.42).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((sizeDp * 0.3).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366))
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun OperatorBadge(operatorName: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (operatorName.lowercase()) {
        "orange" -> OperatorOrange to Color.White
        "moov africa", "moov" -> OperatorMoov to Color.White
        "telecel" -> OperatorTelecel to Color.White
        "mtn" -> OperatorMtn to Color.Black
        "vodacom" -> OperatorVodacom to Color.White
        "airtel" -> OperatorAirtel to Color.White
        "safaricom" -> OperatorSafaricom to Color.White
        "tigo" -> OperatorTigo to Color.White
        "halotel" -> OperatorHalotel to Color.White
        "africell" -> OperatorAfricell to Color.White
        "telkom", "telkom kenya" -> OperatorTelkom to Color.White
        "mauritel" -> OperatorMauritel to Color.White
        "chinguitel" -> OperatorChinguitel to Color.White
        "mattel" -> OperatorMattel to Color.White
        "camtel" -> OperatorCamtel to Color.White
        "nexttel" -> OperatorNexttel to Color.White
        "at&t", "att" -> Color(0xFF00A8E0) to Color.White
        "verizon" -> Color(0xFFCD040B) to Color.White
        "t-mobile" -> Color(0xFFE20074) to Color.White
        "rogers" -> Color(0xFFDA291C) to Color.White
        "bell" -> Color(0xFF005596) to Color.White
        "telus" -> Color(0xFF4B286D) to Color.White
        "proximus" -> Color(0xFF5F259F) to Color.White
        "base" -> Color(0xFF009639) to Color.White
        "swisscom" -> Color(0xFF0019A5) to Color.White
        "sunrise" -> Color(0xFFE2001A) to Color.White
        "salt" -> Color(0xFF222222) to Color.White
        "ee" -> Color(0xFF007B85) to Color.White
        "vodafone" -> Color(0xFFE60000) to Color.White
        "o2" -> Color(0xFF0038A8) to Color.White
        "three" -> Color(0xFF1A1A1A) to Color.White
        "telekom" -> Color(0xFFE20074) to Color.White
        "telcel" -> Color(0xFF002F6C) to Color.White
        "movistar" -> Color(0xFF00A9CE) to Color.White
        "vivo" -> Color(0xFF660099) to Color.White
        "claro" -> Color(0xFFDA291C) to Color.White
        "tim" -> Color(0xFF003399) to Color.White
        "telstra" -> Color(0xFF0064D2) to Color.White
        "optus" -> Color(0xFF00C1D5) to Color.Black
        "telkomsel" -> Color(0xFFEE2E24) to Color.White
        "indosat" -> Color(0xFFFFCC00) to Color.Black
        "xl axiata", "xl" -> Color(0xFF00529C) to Color.White
        "beeline" -> Color(0xFFFFCC00) to Color.Black
        "kcell" -> Color(0xFF7B2D82) to Color.White
        "mts" -> Color(0xFFE30611) to Color.White
        "megafon" -> Color(0xFF00B956) to Color.White
        "china mobile" -> Color(0xFF0085D0) to Color.White
        "china telecom" -> Color(0xFF003882) to Color.White
        "china unicom" -> Color(0xFFE60012) to Color.White
        "ntt docomo", "docomo" -> Color(0xFFCC0000) to Color.White
        "softbank" -> Color(0xFF707070) to Color.White
        "au" -> Color(0xFFED6D00) to Color.White
        "rakuten" -> Color(0xFFBF0000) to Color.White
        "sk telecom", "skt" -> Color(0xFFEA002C) to Color.White
        "kt" -> Color(0xFF222222) to Color.White
        "lg u+", "lg uplus" -> Color(0xFFE6007E) to Color.White
        "turkcell" -> Color(0xFF002855) to Color(0xFFFFC20E)
        "türk telekom", "turk telekom" -> Color(0xFF005696) to Color.White
        "jio" -> Color(0xFF0A2885) to Color.White
        "vi" -> Color(0xFFE31837) to Color.White
        "bsnl" -> Color(0xFF0055A5) to Color.White
        "stc" -> Color(0xFF4F008C) to Color.White
        "mobily" -> Color(0xFF00A3E0) to Color.White
        "zain" -> Color(0xFF231F20) to Color.White
        "e& (etisalat)", "etisalat" -> Color(0xFF719E19) to Color.White
        "du" -> Color(0xFF00A4E4) to Color.White
        "ooredoo" -> Color(0xFFED1C24) to Color.White
        "maroc telecom" -> Color(0xFF004F9F) to Color.White
        "inwi" -> Color(0xFF8A1E76) to Color.White
        "djezzy" -> Color(0xFFE20613) to Color.White
        "mobilis" -> Color(0xFF007A3D) to Color.White
        "tunisie telecom" -> Color(0xFF0066B3) to Color.White
        "ethio telecom" -> Color(0xFF0083CA) to Color.White
        "we" -> Color(0xFF4A1A70) to Color.White
        "telenor" -> Color(0xFF00A9E0) to Color.White
        "telia" -> Color(0xFF990099) to Color.White
        "ice" -> Color(0xFF002F6C) to Color.White
        "tele2" -> Color(0xFF001F60) to Color.White
        "tre" -> Color(0xFFFF6600) to Color.White
        "partner" -> Color(0xFF008080) to Color.White
        "pelephone" -> Color(0xFF0055A5) to Color.White
        "hot mobile" -> Color(0xFFE2001A) to Color.White
        "jazz" -> Color(0xFFED1C24) to Color.White
        "zong" -> Color(0xFF8DC63F) to Color.Black
        "ufone" -> Color(0xFFFF6600) to Color.White
        "+móvil", "+movil" -> Color(0xFF00A4E4) to Color.White
        "digicel" -> Color(0xFFED1C24) to Color.White
        else -> AdtPrimary to Color.White
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SignalCellularAlt,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = operatorName,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EncryptionNotice(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Chiffrement",
                tint = AdtPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Les messages et appels sont chiffrés de bout en bout. Aucun tiers, pas même ADT, ne peut les lire ou les écouter.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun ChatWallpaperBackground(
    wallpaperKey: String = "EMERALD",
    isDark: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val wallpaper = remember(wallpaperKey) { WallpaperData.findById(wallpaperKey) }
    val bgBrush = remember(wallpaper, isDark) {
        if (wallpaperKey == "DOODLE" && !isDark) {
            Brush.verticalGradient(listOf(Color(0xFFEFEAE2), Color(0xFFE5DDD5)))
        } else if (wallpaper.gradientColors.isNotEmpty()) {
            Brush.verticalGradient(wallpaper.gradientColors)
        } else {
            Brush.verticalGradient(
                if (isDark) listOf(Color(0xFF0B141A), Color(0xFF06231E))
                else listOf(Color(0xFFE8ECE9), Color(0xFFD9E2DC))
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        // Decorative atmospheric background canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val accent = wallpaper.accentColor.copy(alpha = 0.08f)
            val accentGlow = wallpaper.accentColor.copy(alpha = 0.04f)

            when {
                // COULEURS (Smooth radiant ambient orbs & minimalist geometric aura)
                wallpaper.category == com.example.data.model.WallpaperCategory.COLORS -> {
                    drawCircle(accentGlow, radius = width * 0.55f, center = Offset(width * 0.8f, height * 0.2f))
                    drawCircle(accentGlow, radius = width * 0.45f, center = Offset(width * 0.2f, height * 0.8f))
                    drawCircle(accent.copy(alpha = 0.05f), radius = width * 0.25f, center = Offset(width * 0.5f, height * 0.5f))
                    for (r in listOf(60f, 120f, 200f)) {
                        drawCircle(accent.copy(alpha = 0.03f), radius = r, center = Offset(width * 0.8f, height * 0.2f), style = Stroke(width = 1.2f))
                    }
                }

                // FLEURS (Flower petals & organic bloom circles)
                wallpaper.category == com.example.data.model.WallpaperCategory.FLOWERS -> {
                    // Soft floral ambient circles & petals
                    drawCircle(accentGlow, radius = width * 0.45f, center = Offset(width * 0.85f, height * 0.15f))
                    drawCircle(accentGlow, radius = width * 0.35f, center = Offset(width * 0.15f, height * 0.75f))
                    // Flower petal clusters
                    for (i in 0..5) {
                        val cx = width * (0.15f + 0.14f * i)
                        val cy = height * (0.25f + 0.10f * ((i * 3) % 7))
                        drawCircle(accent, radius = 14f, center = Offset(cx, cy))
                        drawCircle(accent, radius = 6f, center = Offset(cx, cy))
                        drawLine(accent, Offset(cx - 16f, cy), Offset(cx + 16f, cy), strokeWidth = 1.5f)
                        drawLine(accent, Offset(cx, cy - 16f), Offset(cx, cy + 16f), strokeWidth = 1.5f)
                    }
                }

                // ARBRES (Trees & foliage branches)
                wallpaper.category == com.example.data.model.WallpaperCategory.TREES -> {
                    drawCircle(accentGlow, radius = width * 0.5f, center = Offset(width * 0.5f, height * 0.2f))
                    // Soft vertical forest trunks & canopy lines
                    for (i in 0..7) {
                        val x = width * (0.08f + 0.12f * i)
                        drawLine(
                            color = accent,
                            start = Offset(x, height),
                            end = Offset(x + (i % 3 - 1) * 20f, height * 0.4f),
                            strokeWidth = 2.5f
                        )
                        drawCircle(accent, radius = 22f, center = Offset(x, height * 0.45f))
                    }
                }

                // VOITURES (Speedlines & aerodynamic contours)
                wallpaper.category == com.example.data.model.WallpaperCategory.CARS -> {
                    // Dynamic horizontal speedlines
                    drawCircle(accentGlow, radius = width * 0.4f, center = Offset(width * 0.5f, height * 0.5f))
                    for (i in 0..8) {
                        val y = height * (0.15f + 0.08f * i)
                        drawLine(
                            color = accent,
                            start = Offset(0f, y),
                            end = Offset(width * (0.3f + 0.08f * (i % 5)), y),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = accent.copy(alpha = 0.05f),
                            start = Offset(width * 0.6f, y + 10f),
                            end = Offset(width, y + 10f),
                            strokeWidth = 1.5f
                        )
                    }
                }

                // ÉTOILES (Cosmic starry night & constellations)
                wallpaper.category == com.example.data.model.WallpaperCategory.STARS -> {
                    drawCircle(accentGlow, radius = width * 0.5f, center = Offset(width * 0.7f, height * 0.3f))
                    // Twinkle stars
                    val starPositions = listOf(
                        Offset(width * 0.15f, height * 0.12f),
                        Offset(width * 0.45f, height * 0.08f),
                        Offset(width * 0.82f, height * 0.16f),
                        Offset(width * 0.28f, height * 0.26f),
                        Offset(width * 0.68f, height * 0.35f),
                        Offset(width * 0.12f, height * 0.52f),
                        Offset(width * 0.88f, height * 0.58f),
                        Offset(width * 0.35f, height * 0.72f),
                        Offset(width * 0.75f, height * 0.82f)
                    )
                    starPositions.forEach { pos ->
                        drawCircle(wallpaper.accentColor.copy(alpha = 0.22f), radius = 3.5f, center = pos)
                        drawLine(wallpaper.accentColor.copy(alpha = 0.15f), Offset(pos.x - 8f, pos.y), Offset(pos.x + 8f, pos.y), strokeWidth = 1.2f)
                        drawLine(wallpaper.accentColor.copy(alpha = 0.15f), Offset(pos.x, pos.y - 8f), Offset(pos.x, pos.y + 8f), strokeWidth = 1.2f)
                    }
                }

                // LUNE (Moon disc & celestial crescent glow)
                wallpaper.category == com.example.data.model.WallpaperCategory.MOON -> {
                    // Moon aura
                    val moonCenter = Offset(width * 0.78f, height * 0.18f)
                    drawCircle(accentGlow, radius = width * 0.4f, center = moonCenter)
                    drawCircle(wallpaper.accentColor.copy(alpha = 0.12f), radius = 45f, center = moonCenter)
                    drawCircle(wallpaper.accentColor.copy(alpha = 0.18f), radius = 38f, center = moonCenter)
                    // Horizon moonlit water reflections
                    for (i in 0..5) {
                        val y = height * (0.80f + 0.03f * i)
                        val lineLen = width * (0.2f + 0.06f * i)
                        drawLine(accent, Offset(moonCenter.x - lineLen / 2, y), Offset(moonCenter.x + lineLen / 2, y), strokeWidth = 1.5f)
                    }
                }

                // SOLEIL (Solar rays & dawn/dusk radiant burst)
                wallpaper.category == com.example.data.model.WallpaperCategory.SUN -> {
                    val sunCenter = Offset(width * 0.5f, height * 0.35f)
                    drawCircle(accentGlow, radius = width * 0.6f, center = sunCenter)
                    drawCircle(wallpaper.accentColor.copy(alpha = 0.15f), radius = 55f, center = sunCenter)
                    // Radiating solar ray spokes
                    for (deg in 0 until 360 step 30) {
                        val rad = Math.toRadians(deg.toDouble())
                        val r1 = 65f
                        val r2 = 140f
                        val p1 = Offset(sunCenter.x + (r1 * Math.cos(rad)).toFloat(), sunCenter.y + (r1 * Math.sin(rad)).toFloat())
                        val p2 = Offset(sunCenter.x + (r2 * Math.cos(rad)).toFloat(), sunCenter.y + (r2 * Math.sin(rad)).toFloat())
                        drawLine(accent, p1, p2, strokeWidth = 1.8f)
                    }
                }

                // CLASSIQUE (Subtle minimal geometric watermark)
                else -> {
                    drawCircle(accentGlow, radius = width * 0.4f, center = Offset(width * 0.5f, height * 0.3f))
                }
            }
        }

        // Inner chat content
        content()
    }
}

