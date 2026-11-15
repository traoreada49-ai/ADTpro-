package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.MessageEntity
import com.example.data.model.LiveLocationPayload
import com.example.ui.theme.AdtPrimary
import kotlinx.coroutines.delay

@Composable
fun LiveLocationMapBubble(
    message: MessageEntity,
    onStopLiveSharing: (String) -> Unit,
    onOpenLiveTracking: ((LiveLocationPayload) -> Unit)? = null
) {
    val context = LocalContext.current
    val payload = remember(message.mediaUrl) {
        LiveLocationPayload.deserialize(message.mediaUrl) ?: LiveLocationPayload(
            latitude = 14.6928,
            longitude = -17.4467,
            address = message.text.removePrefix("📍").trim().ifBlank { "Position partagée" },
            durationMinutes = 60,
            startedAt = message.timestamp,
            expiresAt = message.timestamp + 60 * 60 * 1000L,
            isActive = true
        )
    }

    // Dynamic countdown timer ticking every 10 seconds
    var remainingFormatted by remember(payload) { mutableStateOf(payload.remainingTimeFormatted()) }
    var isExpired by remember(payload) { mutableStateOf(payload.isExpired()) }
    var showFullscreenMap by remember { mutableStateOf(false) }

    LaunchedEffect(payload) {
        while (!isExpired && payload.isActive) {
            remainingFormatted = payload.remainingTimeFormatted()
            isExpired = payload.isExpired()
            delay(10000L)
        }
    }

    val isLiveActive = payload.isActive && !isExpired

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isLiveActive) AdtPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("bubble_live_location_${message.id}")
    ) {
        Column {
            // Map Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .clickable { if (onOpenLiveTracking != null) onOpenLiveTracking(payload) else showFullscreenMap = true }
            ) {
                // Vector Rendered Map Canvas
                MapGraphicCanvas(
                    latitude = payload.latitude,
                    longitude = payload.longitude,
                    isLive = isLiveActive,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Badge: Status Pill (EN DIRECT / TERMINÉ)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isLiveActive) Color(0xFF1B5E20).copy(alpha = 0.9f) else Color(0xFF424242).copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        if (isLiveActive) {
                            PulsingDot()
                            Text(
                                text = "EN DIRECT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "PARTAGE TERMINÉ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                // Top Right Countdown Chip
                if (isLiveActive) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = remainingFormatted,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Center Pin Marker
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLiveActive) {
                        RadarPulseRings()
                    }
                    LocationMarkerPin(
                        senderName = message.senderName,
                        isLive = isLiveActive
                    )
                }

                // Tap to expand overlay hint
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Agrandir",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text("Carte", color = Color.White, fontSize = 9.sp)
                    }
                }
            }

            // Location Info & Actions
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = AdtPrimary,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = payload.address,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Lat: %.4f, Lng: %.4f • Précision ±%.0fm".format(payload.latitude, payload.longitude, payload.accuracyMeters),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (payload.comment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${payload.comment}\"",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLiveActive && onOpenLiveTracking != null) {
                            Button(
                                onClick = { onOpenLiveTracking(payload) },
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("btn_track_live_movement")
                            ) {
                                Icon(Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Suivre", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Open in Google Maps
                        TextButton(
                            onClick = {
                                openInGoogleMaps(context, payload.latitude, payload.longitude, payload.address)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp), tint = AdtPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Itinéraire", fontSize = 12.sp, color = AdtPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Stop live location (if user sent it and it's active)
                    if (message.isOutgoing && isLiveActive) {
                        OutlinedButton(
                            onClick = { onStopLiveSharing(message.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("btn_stop_live_location")
                        ) {
                            Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Arrêter le partage", fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }

    // Fullscreen interactive map view dialog
    if (showFullscreenMap) {
        FullscreenMapDialog(
            payload = payload,
            senderName = message.senderName,
            isLive = isLiveActive,
            remainingFormatted = remainingFormatted,
            onDismiss = { showFullscreenMap = false },
            onOpenMaps = { openInGoogleMaps(context, payload.latitude, payload.longitude, payload.address) }
        )
    }
}

@Composable
fun MapGraphicCanvas(
    latitude: Double,
    longitude: Double,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Terrain Base (Subtle urban beige / soft grey)
        drawRect(color = Color(0xFFECE5DD))

        // 2. Waterways / Ocean / Coastal bay in soft cyan-blue
        val waterPath = Path().apply {
            moveTo(0f, h * 0.15f)
            quadraticTo(w * 0.25f, h * 0.05f, w * 0.55f, h * 0.2f)
            quadraticTo(w * 0.85f, h * 0.35f, w, h * 0.25f)
            lineTo(w, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(waterPath, color = Color(0xFFA5D6A7).copy(alpha = 0.45f)) // Green coastal zone
        val coastalWater = Path().apply {
            moveTo(0f, h * 0.1f)
            quadraticTo(w * 0.3f, 0f, w * 0.7f, h * 0.12f)
            lineTo(w, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(coastalWater, color = Color(0xFF90CAF9).copy(alpha = 0.7f)) // Water body

        // 3. City Blocks (Soft rounded rectangles)
        val blockColor = Color(0xFFE0D7CD)
        drawRoundRect(blockColor, Offset(w * 0.05f, h * 0.32f), Size(w * 0.22f, h * 0.25f), CornerRadius(6f, 6f))
        drawRoundRect(blockColor, Offset(w * 0.32f, h * 0.30f), Size(w * 0.28f, h * 0.20f), CornerRadius(6f, 6f))
        drawRoundRect(blockColor, Offset(w * 0.65f, h * 0.35f), Size(w * 0.28f, h * 0.25f), CornerRadius(6f, 6f))
        drawRoundRect(blockColor, Offset(w * 0.08f, h * 0.65f), Size(w * 0.35f, h * 0.28f), CornerRadius(6f, 6f))
        drawRoundRect(blockColor, Offset(w * 0.50f, h * 0.68f), Size(w * 0.42f, h * 0.26f), CornerRadius(6f, 6f))

        // Park greenery
        drawRoundRect(Color(0xFFC8E6C9), Offset(w * 0.65f, h * 0.68f), Size(w * 0.25f, h * 0.22f), CornerRadius(8f, 8f))

        // 4. Secondary Road Network (White clean lines)
        val secondaryRoadColor = Color.White
        val roadStroke = Stroke(width = 10f)
        // Horizontal secondary streets
        drawLine(secondaryRoadColor, Offset(0f, h * 0.28f), Offset(w, h * 0.28f), strokeWidth = 8f)
        drawLine(secondaryRoadColor, Offset(0f, h * 0.58f), Offset(w, h * 0.58f), strokeWidth = 9f)
        drawLine(secondaryRoadColor, Offset(0f, h * 0.88f), Offset(w, h * 0.88f), strokeWidth = 7f)
        // Vertical secondary streets
        drawLine(secondaryRoadColor, Offset(w * 0.28f, 0f), Offset(w * 0.28f, h), strokeWidth = 8f)
        drawLine(secondaryRoadColor, Offset(w * 0.62f, 0f), Offset(w * 0.62f, h), strokeWidth = 8f)

        // 5. Main Arterial Highway / Avenue (Yellow/Orange Boulevard with border)
        val highwayBorder = Stroke(width = 16f)
        val highwayFill = Stroke(width = 12f)
        val highwayPath = Path().apply {
            moveTo(0f, h * 0.8f)
            quadraticTo(w * 0.4f, h * 0.6f, w * 0.5f, h * 0.5f)
            quadraticTo(w * 0.6f, h * 0.4f, w, h * 0.15f)
        }
        drawPath(highwayPath, color = Color(0xFFD7CCC8), style = highwayBorder)
        drawPath(highwayPath, color = Color(0xFFFFE082), style = highwayFill)

        // Roundabout in the middle
        val centerPoint = Offset(w * 0.5f, h * 0.5f)
        drawCircle(color = Color(0xFFD7CCC8), radius = 22f, center = centerPoint)
        drawCircle(color = Color(0xFFFFE082), radius = 18f, center = centerPoint)
        drawCircle(color = Color(0xFFC8E6C9), radius = 10f, center = centerPoint) // Green island in roundabout
    }
}

@Composable
fun RadarPulseRings() {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxRadius = size.minDimension / 2f
        val currentRadius = maxRadius * pulseProgress
        val alpha = (1f - pulseProgress).coerceIn(0f, 0.7f)

        // Radar wave circle
        drawCircle(
            color = Color(0xFF00A884).copy(alpha = alpha),
            radius = currentRadius
        )
        drawCircle(
            color = Color(0xFF00A884).copy(alpha = alpha * 0.8f),
            radius = currentRadius,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "BlinkDot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Box(
        modifier = Modifier
            .size(7.dp)
            .background(Color(0xFF4CAF50).copy(alpha = alpha), CircleShape)
    )
}

@Composable
fun LocationMarkerPin(
    senderName: String,
    isLive: Boolean
) {
    val pinColor = if (isLive) Color(0xFF00A884) else Color(0xFF616161)
    Box(
        modifier = Modifier.size(38.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.size(34.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .background(pinColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = senderName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        // Small active beacon badge on pin
        if (isLive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .background(Color(0xFF00E676), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenMapDialog(
    payload: LiveLocationPayload,
    senderName: String,
    isLive: Boolean,
    remainingFormatted: String,
    onDismiss: () -> Unit,
    onOpenMaps: () -> Unit
) {
    var zoomLevel by remember { mutableStateOf(1.0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Localisation de $senderName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isLive) "En direct • Reste $remainingFormatted" else "Partage terminé",
                                fontSize = 12.sp,
                                color = if (isLive) Color(0xFF00A884) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Fermer")
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenMaps) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Ouvrir dans Google Maps")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Large Vector Map Canvas
                MapGraphicCanvas(
                    latitude = payload.latitude,
                    longitude = payload.longitude,
                    isLive = isLive,
                    modifier = Modifier.fillMaxSize()
                )

                // Big Pin Marker in center
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(70.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLive) {
                        RadarPulseRings()
                    }
                    LocationMarkerPin(senderName = senderName, isLive = isLive)
                }

                // Floating Zoom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { zoomLevel = (zoomLevel + 0.2f).coerceAtMost(2.5f) },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom avant")
                    }
                    SmallFloatingActionButton(
                        onClick = { zoomLevel = (zoomLevel - 0.2f).coerceAtLeast(0.6f) },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom arrière")
                    }
                    SmallFloatingActionButton(
                        onClick = { zoomLevel = 1.0f },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Centrer", tint = AdtPrimary)
                    }
                }

                // Bottom Info Card
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isLive) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = null,
                                        tint = if (isLive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = payload.address,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Coordonnées : %.5f° N, %.5f° W".format(payload.latitude, payload.longitude),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (payload.comment.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Note : ${payload.comment}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenMaps,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ouvrir Navigation")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openInGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String) {
    try {
        val encodedLabel = Uri.encode(label)
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        try {
            // Fallback to browser Google Maps
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Coordonnées : $latitude, $longitude", Toast.LENGTH_LONG).show()
        }
    }
}
