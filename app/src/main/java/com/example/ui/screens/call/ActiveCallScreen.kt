package com.example.ui.screens.call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AdtAvatar
import com.example.ui.theme.AdtPrimary
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import com.example.viewmodel.AdtViewModel

@Composable
fun ActiveCallScreen(viewModel: AdtViewModel) {
    val callState by viewModel.activeCall.collectAsState()
    if (callState == null) return
    val call = callState!!

    var showE2eeDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val incomingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "incomingScale"
    )

    val durationText = remember(call.durationSeconds) {
        val minutes = call.durationSeconds / 60
        val seconds = call.durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A1128),
                        Color(0xFF101F42),
                        Color(0xFF1C2541)
                    )
                )
            )
    ) {
        // =====================================================================
        // VIDEO CALL CANVAS / PIP PREVIEWS
        // =====================================================================
        if (call.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B132B))
            ) {
                // Main full-screen Video Stream (Remote by default, or swapped Local)
                if (call.isLocalPreviewMain) {
                    // Local user preview is full-screen
                    LocalCameraStreamView(
                        isFrontCamera = call.isFrontCamera,
                        isVideoOff = call.isVideoOff,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Remote participant feed is full-screen
                    RemoteCameraStreamView(
                        contactName = call.contactName,
                        isConnected = call.isConnected,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // PiP Floating Thumbnail (Swap view on tap)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = CardDefaults.outlinedCardBorder(),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 70.dp, end = 16.dp)
                        .size(width = 110.dp, height = 155.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.toggleVideoPreviewSwap() }
                        .testTag("pip_video_preview")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (call.isLocalPreviewMain) {
                            RemoteCameraStreamView(
                                contactName = call.contactName,
                                isConnected = call.isConnected,
                                isThumbnail = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LocalCameraStreamView(
                                isFrontCamera = call.isFrontCamera,
                                isVideoOff = call.isVideoOff,
                                isThumbnail = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // PiP switch camera quick button
                        IconButton(
                            onClick = { viewModel.switchCallCamera() },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("btn_switch_camera_pip")
                        ) {
                            Icon(
                                Icons.Default.FlipCameraAndroid,
                                contentDescription = "Changer de caméra",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TOP APP BAR: MINIMIZE BUTTON & E2EE BADGE
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minimize Call to Floating Bar
            IconButton(
                onClick = { viewModel.minimizeCall() },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .testTag("btn_minimize_call")
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Réduire l'appel",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // E2EE Security Badge
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .clickable { showE2eeDialog = true }
                    .testTag("badge_e2ee_security")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        if (call.e2eeInfo.isVerified) Icons.Default.VerifiedUser else Icons.Default.Shield,
                        contentDescription = "Statut E2EE",
                        tint = if (call.e2eeInfo.isVerified) Color(0xFF25D366) else Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (call.e2eeInfo.isVerified) "E2EE ADT • Vérifié" else "E2EE ADT • Protégé",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Security Details Button
            IconButton(
                onClick = { showE2eeDialog = true },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .testTag("btn_security_info")
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = "Détails de sécurité",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // =====================================================================
        // CENTER SECTION: CONTACT INFO, AUDIO WAVEFORM & AVATAR
        // =====================================================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = call.contactName,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = call.contactPhone,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            val connectionStatusText = when (call.connectionState) {
                com.example.data.webrtc.WebRtcConnectionState.PREPARING -> "Initialisation du flux E2EE..."
                com.example.data.webrtc.WebRtcConnectionState.CONNECTING_SIGNALING -> "Signalisation SDP chiffrée..."
                com.example.data.webrtc.WebRtcConnectionState.RINGING -> if (call.isIncoming) "Appel entrant..." else "Sonnerie..."
                com.example.data.webrtc.WebRtcConnectionState.EXCHANGING_ICE -> "Négociation DTLS-SRTP (ICE)..."
                com.example.data.webrtc.WebRtcConnectionState.CONNECTED -> {
                    if (call.isVideo) "Appel vidéo HD E2EE • $durationText"
                    else "Appel audio WebRTC E2EE • $durationText"
                }
                com.example.data.webrtc.WebRtcConnectionState.RECONNECTING -> "Reconnexion réseau sécurisée..."
                com.example.data.webrtc.WebRtcConnectionState.DISCONNECTED -> "Déconnecté"
                com.example.data.webrtc.WebRtcConnectionState.FAILED -> "Échec de connexion"
                com.example.data.webrtc.WebRtcConnectionState.ENDED -> "Appel terminé"
                else -> "Connexion..."
            }

            Surface(
                color = if (call.isConnected) Color(0xFF25D366).copy(alpha = 0.2f) else Color(0xFFFFB703).copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = connectionStatusText,
                    color = if (call.isConnected) Color(0xFF25D366) else Color(0xFFFFB703),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Audio Call Central Glowing Avatar and Sound Waves
            if (!call.isVideo) {
                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Multi-layer glowing pulsing rings
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .border(2.dp, AdtPrimary.copy(alpha = pulseAlpha * 0.4f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(165.dp)
                            .border(3.dp, Color(0xFF25D366).copy(alpha = pulseAlpha), CircleShape)
                    )
                    AdtAvatar(name = call.contactName, sizeDp = 136)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Real-time Audio Waveform Reactive Bars
                if (call.isConnected) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(48.dp)
                    ) {
                        call.audioWaveLevels.forEachIndexed { index, level ->
                            val targetHeight = (10.dp + (level * 36).dp)
                            val animatedBarHeight by animateDpAsState(
                                targetValue = targetHeight,
                                animationSpec = tween(150, easing = LinearEasing),
                                label = "waveBar$index"
                            )
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(animatedBarHeight)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF25D366),
                                                AdtPrimary
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                // WebRTC Codec & Latency Chip
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${call.quality.codec} • ${call.quality.latencyMs}ms • Perte: 0%",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // =====================================================================
        // BOTTOM ACTIONS BAR
        // =====================================================================
        Surface(
            color = Color(0xFF0F172A).copy(alpha = 0.92f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // If call is incoming and not yet connected: show Answer & Decline buttons
                if (call.isIncoming && !call.isConnected) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (call.isVideo) "Appel vidéo E2EE entrant" else "Appel audio E2EE entrant",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decline Call
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.endCall() },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .background(CallRed, CircleShape)
                                        .testTag("btn_decline_incoming_call")
                                ) {
                                    Icon(
                                        Icons.Default.CallEnd,
                                        contentDescription = "Refuser",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text("Refuser", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }

                            // Accept Call (Glowing pulsing)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.acceptIncomingCall() },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .scale(incomingScale)
                                        .background(CallGreen, CircleShape)
                                        .testTag("btn_accept_incoming_call")
                                ) {
                                    Icon(
                                        if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                        contentDescription = "Répondre",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text("Répondre", color = CallGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // ACTIVE CALL IN-CALL CONTROLS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speakerphone Toggle
                        IconButton(
                            onClick = { viewModel.toggleCallSpeaker() },
                            modifier = Modifier
                                .size(54.dp)
                                .background(
                                    if (call.isSpeakerOn) AdtPrimary else Color.White.copy(alpha = 0.15f),
                                    CircleShape
                                )
                                .testTag("btn_toggle_speaker")
                        ) {
                            Icon(
                                if (call.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Haut-parleur",
                                tint = Color.White
                            )
                        }

                        // Video Mode / Camera Toggle / Upgrade
                        if (call.isVideo) {
                            IconButton(
                                onClick = { viewModel.toggleCallVideo() },
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        if (call.isVideoOff) CallRed else Color.White.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .testTag("btn_toggle_video")
                            ) {
                                Icon(
                                    if (call.isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                    contentDescription = "Caméra vidéo",
                                    tint = Color.White
                                )
                            }

                            // Switch Camera (Front/Back)
                            IconButton(
                                onClick = { viewModel.switchCallCamera() },
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                    .testTag("btn_switch_camera")
                            ) {
                                Icon(
                                    Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Basculer caméra",
                                    tint = Color.White
                                )
                            }
                        } else {
                            // Upgrade to Video Button
                            IconButton(
                                onClick = { viewModel.upgradeToVideoCall() },
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                    .testTag("btn_upgrade_video")
                            ) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = "Passer en vidéo",
                                    tint = Color.White
                                )
                            }
                        }

                        // Mute Microphone Toggle
                        IconButton(
                            onClick = { viewModel.toggleCallMute() },
                            modifier = Modifier
                                .size(54.dp)
                                .background(
                                    if (call.isMuted) CallRed else Color.White.copy(alpha = 0.15f),
                                    CircleShape
                                )
                                .testTag("btn_toggle_mic")
                        ) {
                            Icon(
                                if (call.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Microphone",
                                tint = Color.White
                            )
                        }

                        // End Call Button
                        IconButton(
                            onClick = { viewModel.endCall() },
                            modifier = Modifier
                                .size(62.dp)
                                .background(CallRed, CircleShape)
                                .testTag("btn_end_call")
                        ) {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = "Raccrocher",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // E2EE SECURITY VERIFICATION MODAL DIALOG
    // =========================================================================
    if (showE2eeDialog) {
        val clipboardManager: ClipboardManager = LocalClipboardManager.current
        var copiedToClipboard by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showE2eeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Chiffrement E2EE ADT", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cet appel WebRTC est chiffré de bout en bout. Les flux audio et vidéo sont protégés par la suite cryptographique standard Signal & WebRTC SRTP-AES-256.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Safety Symbols (4 verification emojis)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Symboles de vérification directe",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                call.e2eeInfo.safetyEmojis.forEach { emoji ->
                                    Text(text = emoji, fontSize = 26.sp)
                                }
                            }
                        }
                    }

                    // Safety Number Blocks
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Numéro de sécurité (Safety Number)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = call.e2eeInfo.safetyNumber,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Technical specs
                    Text(
                        text = "Protocole : ${call.e2eeInfo.cipherSuite}\nEmpreinte DTLS : ${call.e2eeInfo.fingerprint.take(23)}...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(call.e2eeInfo.safetyNumber))
                        copiedToClipboard = true
                    }
                ) {
                    Text(if (copiedToClipboard) "Copié !" else "Copier le code")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.verifyCallSafetyNumber()
                        showE2eeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (call.e2eeInfo.isVerified) Color(0xFF25D366) else AdtPrimary)
                ) {
                    Text(if (call.e2eeInfo.isVerified) "Certifié conforme ✓" else "Marquer comme vérifié")
                }
            }
        )
    }
}

@Composable
private fun RemoteCameraStreamView(
    contactName: String,
    isConnected: Boolean,
    isThumbnail: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF132238),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isThumbnail) 4.dp else 12.dp)
        ) {
            AdtAvatar(name = contactName, sizeDp = if (isThumbnail) 44 else 110)
            if (!isThumbnail) {
                Text(
                    text = if (isConnected) "Flux distant HD sécurisé" else "En attente du flux distant...",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
            }
        }

        // Live HD badge
        if (!isThumbnail && isConnected) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 90.dp, start = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF25D366), CircleShape)
                    )
                    Text("VP9 1080p FHD", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LocalCameraStreamView(
    isFrontCamera: Boolean,
    isVideoOff: Boolean,
    isThumbnail: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isVideoOff) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(if (isThumbnail) 24.dp else 40.dp)
                )
                if (!isThumbnail) {
                    Text("Votre caméra est désactivée", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (isFrontCamera) Icons.Default.Face else Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = AdtPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(if (isThumbnail) 28.dp else 56.dp)
                )
                Text(
                    text = if (isThumbnail) (if (isFrontCamera) "Avant" else "Arrière") else (if (isFrontCamera) "Caméra frontale (Vous)" else "Caméra arrière (Vous)"),
                    color = Color.White,
                    fontSize = if (isThumbnail) 10.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
