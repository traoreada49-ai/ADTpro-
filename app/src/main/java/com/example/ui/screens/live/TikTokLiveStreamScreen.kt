package com.example.ui.screens.live

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.live.*
import com.example.data.local.entity.UserEntity
import com.example.ui.components.AdtAvatar
import com.example.ui.theme.*
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

val TikTokRed = Color(0xFFFE2C55)
val TikTokCyan = Color(0xFF25F4EE)
val TikTokDark = Color(0xFF121212)
val GoldYellow = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TikTokLiveStreamScreen(
    viewModel: AdtViewModel
) {
    val activeSession by viewModel.activeLiveSession.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val creatorAuth by viewModel.liveCreatorAuthorization.collectAsState()

    var showGiftSheet by remember { mutableStateOf(false) }
    var showHostToolsSheet by remember { mutableStateOf(false) }
    var showAuthBadgeDialog by remember { mutableStateOf(false) }
    var showShareLiveDialog by remember { mutableStateOf(false) }
    var showCommentInputBar by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var selectedGift by remember { mutableStateOf(DEFAULT_TIKTOK_GIFTS.first()) }

    val coroutineScope = rememberCoroutineScope()
    val chatListState = rememberLazyListState()

    // Auto-scroll chat when new comment arrives
    LaunchedEffect(activeSession?.comments?.size) {
        val size = activeSession?.comments?.size ?: 0
        if (size > 0) {
            chatListState.animateScrollToItem(size - 1)
        }
    }

    if (activeSession == null) {
        // Fallback if accessed without session
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = TikTokRed)
                Text("Connexion au direct...", color = Color.White, fontSize = 14.sp)
                Button(
                    onClick = { viewModel.navigateTo(AppScreen.MAIN_TABS) },
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokRed)
                ) {
                    Text("Retour")
                }
            }
        }
        return
    }

    val session = activeSession!!
    val stream = session.stream

    BackHandler {
        if (session.isHost) {
            viewModel.showEndSummaryDialog()
        } else {
            viewModel.leaveLiveStream()
        }
    }

    // Format duration
    val durationFormatted = remember(session.durationSeconds) {
        val min = session.durationSeconds / 60
        val sec = session.durationSeconds % 60
        String.format("%02d:%02d", min, sec)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Tap anywhere on screen to send TikTok flying hearts / likes
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val xPercent = (offset.x / size.width).coerceIn(0.6f, 0.95f)
                        viewModel.sendLiveLike(xPercent)
                    },
                    onDoubleTap = { offset ->
                        val xPercent = (offset.x / size.width).coerceIn(0.6f, 0.95f)
                        repeat(3) {
                            viewModel.sendLiveLike(xPercent)
                        }
                    }
                )
            }
    ) {
        // 1. LIVE VIDEO BACKGROUND / CAMERA STREAM
        LiveStreamBackgroundCanvas(
            isHost = session.isHost,
            isFrontCamera = session.isFrontCamera,
            activeFilter = session.activeFilter,
            hostName = stream.hostName,
            category = stream.category,
            likesCount = session.likesCount
        )

        // 2. PK BATTLE BAR OVERLAY (If battle active)
        if (session.isPkBattleActive) {
            PkBattleScoreBanner(
                hostScore = session.pkHostScore,
                opponentScore = session.pkOpponentScore,
                opponentName = session.pkOpponentName,
                remainingSeconds = session.pkRemainingSeconds,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 96.dp)
                    .align(Alignment.TopCenter)
            )
        }

        // 3. TOP HEADER BAR
        LiveTopHeaderBar(
            session = session,
            stream = stream,
            durationText = durationFormatted,
            onAuthBadgeClick = { showAuthBadgeDialog = true },
            onFollowClick = { viewModel.toggleFollowLiveHost() },
            onCloseClick = {
                if (session.isHost) {
                    viewModel.showEndSummaryDialog()
                } else {
                    viewModel.leaveLiveStream()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .align(Alignment.TopCenter)
        )

        // 3.5 MUSIC TICKER & SOUNDTRACK
        LiveMusicTicker(
            musicTrack = stream.musicTrack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, top = if (session.isPkBattleActive) 140.dp else 56.dp)
                .align(Alignment.TopStart)
        )

        // 3.6 VERTICAL STREAM SWITCHER (For viewers to switch between lives like TikTok)
        if (!session.isHost) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .background(Color(0x77000000), RoundedCornerShape(20.dp))
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { viewModel.switchToPreviousLiveStream() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_live_prev_stream")
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Direct précédent",
                        tint = Color.White
                    )
                }
                Text(
                    text = "LIVE",
                    color = TikTokRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(
                    onClick = { viewModel.switchToNextLiveStream() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_live_next_stream")
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Direct suivant",
                        tint = Color.White
                    )
                }
            }
        }

        // 4. ANIMATED GIFT BANNER (When someone sends a gift)
        AnimatedVisibility(
            visible = session.activeGiftEvent != null,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp, bottom = 120.dp)
        ) {
            session.activeGiftEvent?.let { giftEvent ->
                LiveGiftFlyBanner(giftEvent = giftEvent)
            }
        }

        // 5. FLOATING HEARTS & LIKES ANIMATION CANVAS
        FloatingHeartsOverlay(
            hearts = session.floatingHearts,
            modifier = Modifier.fillMaxSize()
        )

        // 6. BOTTOM INTERACTIVE OVERLAY: CHAT & CONTROLS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            // Live Chat Messages Column
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .heightIn(max = 240.dp)
                    .padding(horizontal = 12.dp)
            ) {
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Pinned Community & Guidelines Notice
                    item {
                        Surface(
                            color = Color(0x99000000),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f)),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Direct Autorisé ADT • Sécurisé, crypté & modéré en temps réel",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Dynamic Comments list
                    items(session.comments, key = { it.id }) { comment ->
                        LiveCommentBubble(comment = comment)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions Row
            LiveBottomControlsBar(
                session = session,
                onCommentClick = { showCommentInputBar = true },
                onGiftClick = { showGiftSheet = true },
                onShareClick = { showShareLiveDialog = true },
                onHostToolsClick = { showHostToolsSheet = true },
                onQuickLikeClick = { viewModel.sendLiveLike(0.85f) }
            )
        }

        // 7. INLINE COMMENT TEXT FIELD (When user taps to comment)
        if (showCommentInputBar) {
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Dites quelque chose de sympa...", fontSize = 13.sp, color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_live_comment"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            val userName = currentUser?.name ?: "Moi"
                            val userAvatar = currentUser?.name ?: "Moi"
                            viewModel.sendLiveComment(commentText)
                            commentText = ""
                            showCommentInputBar = false
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(TikTokRed, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { showCommentInputBar = false }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }
            }
        }

        // 8. TIKTOK LIVE GIFTS BOTTOM SHEET
        if (showGiftSheet) {
            TikTokGiftBottomSheet(
                coinsBalance = session.userCoinsBalance,
                gifts = DEFAULT_TIKTOK_GIFTS,
                selectedGift = selectedGift,
                onSelectGift = { selectedGift = it },
                onSendGift = {
                    val userName = currentUser?.name ?: "Moi"
                    val userAvatar = currentUser?.name ?: "Moi"
                    viewModel.sendLiveGift(selectedGift)
                },
                onRechargeCoins = { viewModel.rechargeLiveCoins(500) },
                onDismiss = { showGiftSheet = false }
            )
        }

        // 9. HOST TOOLS BOTTOM SHEET
        if (showHostToolsSheet) {
            HostToolsBottomSheet(
                session = session,
                onToggleCamera = { viewModel.toggleLiveCamera() },
                onToggleMic = { viewModel.toggleLiveMic() },
                onSelectFilter = { viewModel.setLiveFilter(it) },
                onBeautyChange = { viewModel.setLiveBeautyLevel(it) },
                onTogglePk = { viewModel.toggleLivePkBattle() },
                onEndLive = {
                    showHostToolsSheet = false
                    viewModel.showEndSummaryDialog()
                },
                onDismiss = { showHostToolsSheet = false }
            )
        }

        // 10. DIRECT AUTORISÉ BADGE INFO DIALOG
        if (showAuthBadgeDialog) {
            LiveAuthorizationBadgeDialog(
                creatorAuth = creatorAuth,
                stream = stream,
                onDismiss = { showAuthBadgeDialog = false }
            )
        }

        // 11. SHARE LIVE DIALOG
        if (showShareLiveDialog) {
            ShareLiveStreamDialog(
                stream = stream,
                onDismiss = { showShareLiveDialog = false }
            )
        }

        // 12. LIVE ENDED SUMMARY DIALOG
        if (session.showEndSummary) {
            LiveEndedSummaryDialog(
                session = session,
                durationFormatted = durationFormatted,
                onFinish = {
                    viewModel.leaveLiveStream()
                }
            )
        }
    }
}

/**
 * Background canvas rendering dynamic video broadcast or camera stream
 */
@Composable
fun LiveStreamBackgroundCanvas(
    isHost: Boolean,
    isFrontCamera: Boolean,
    activeFilter: String,
    hostName: String,
    category: LiveCategory,
    likesCount: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_bg")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val lightSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "light_sweep"
    )

    val filterOverlayColor = when (activeFilter) {
        "Éclat" -> Color(0x33FFD700)
        "Chaud" -> Color(0x33FF7A00)
        "Cinéma" -> Color(0x441E1B4B)
        "Studio" -> Color(0x3300E5FF)
        "Cyber" -> Color(0x44D946EF)
        else -> Color.Transparent
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient animated backdrop simulating high-definition stage lighting
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Base ambient radial gradient
            val baseBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1E293B),
                    Color(0xFF0F172A),
                    Color(0xFF020617)
                ),
                center = Offset(canvasWidth * 0.5f, canvasHeight * 0.4f),
                radius = canvasHeight * 0.8f
            )
            drawRect(brush = baseBrush)

            // Dynamic moving concert/studio beam lights
            val beamX = canvasWidth * lightSweep
            val beamBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0x00FE2C55),
                    Color(0x22FE2C55),
                    Color(0x3325F4EE),
                    Color(0x0025F4EE)
                ),
                start = Offset(beamX - 250f, 0f),
                end = Offset(beamX + 250f, canvasHeight)
            )
            drawRect(brush = beamBrush)
        }

        // Host Live Presence Center Visual
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Pulsing camera/creator avatar with live neon ring
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseAlpha),
                        shape = CircleShape,
                        color = TikTokRed.copy(alpha = 0.2f),
                        border = BorderStroke(2.dp, Brush.sweepGradient(listOf(TikTokRed, TikTokCyan, TikTokRed)))
                    ) {}
                    Surface(
                        modifier = Modifier
                            .size(118.dp),
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        shadowElevation = 12.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AdtAvatar(name = hostName, sizeDp = 110)
                            if (isHost) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Surface(
                                        color = TikTokRed,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isFrontCamera) "Caméra Avant" else "Caméra Arrière",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Stage info
                Surface(
                    color = Color(0x88000000),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(category.icon, fontSize = 14.sp)
                            Text(
                                text = "${category.label} • 1080p 60fps",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (activeFilter != "Naturel") {
                            Text(
                                text = "Filtre actif : $activeFilter",
                                color = TikTokCyan,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Optional Color Grading Shader Filter overlay
        if (filterOverlayColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(filterOverlayColor)
            )
        }
    }
}

/**
 * Top Header bar styled with TikTok Live aesthetics
 */
@Composable
fun LiveTopHeaderBar(
    session: LiveStreamSessionState,
    stream: LiveStreamItem,
    durationText: String,
    onAuthBadgeClick: () -> Unit,
    onFollowClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Host Profile Capsule
        Surface(
            color = Color(0x99000000),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier.clickable { onAuthBadgeClick() }
        ) {
            Row(
                modifier = Modifier.padding(start = 3.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Host avatar with LIVE red ring
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = TikTokRed
                    ) {}
                    AdtAvatar(name = stream.hostName, sizeDp = 32)
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = stream.hostName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Certified / Direct Autorisé shield
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Direct Autorisé",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "❤️ ${formatCount(session.likesCount)}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "• $durationText",
                            color = TikTokCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Follow button if viewer and not yet following
                if (!session.isHost) {
                    Button(
                        onClick = onFollowClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (session.isFollowing) Color.White.copy(alpha = 0.2f) else TikTokRed,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(26.dp)
                            .testTag("btn_follow_host")
                    ) {
                        Text(
                            text = if (session.isFollowing) "Abonné ✓" else "+ Suivre",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Right side: Viewer Counter, Gifter Avatars & Close
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Viewers count capsule
            Surface(
                color = Color(0x99000000),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Pulsing red live dot
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(TikTokRed, CircleShape)
                    )
                    Text(
                        text = "${session.viewerCount}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top gifters preview avatars
            Row(
                modifier = Modifier.padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy((-6).dp)
            ) {
                AdtAvatar(name = "Fatou", sizeDp = 24)
                AdtAvatar(name = "Mamadou", sizeDp = 24)
                AdtAvatar(name = "Awa", sizeDp = 24)
            }

            // Close button
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0x88000000), CircleShape)
                    .testTag("btn_close_live")
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Quitter le direct",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * PK Battle score comparison bar
 */
@Composable
fun PkBattleScoreBanner(
    hostScore: Int,
    opponentScore: Int,
    opponentName: String,
    remainingSeconds: Int,
    modifier: Modifier = Modifier
) {
    val total = (hostScore + opponentScore).coerceAtLeast(1)
    val hostRatio = hostScore.toFloat() / total

    val timeFormatted = remember(remainingSeconds) {
        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        String.format("%02d:%02d", m, s)
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Battle badge with countdown
        Surface(
            color = Color(0xCC000000),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, GoldYellow)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("⚔️ BATTLE PK", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GoldYellow)
                Text(timeFormatted, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress duel bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.DarkGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(hostRatio)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF38BDF8)))),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "  $hostScore",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(Brush.horizontalGradient(listOf(TikTokRed, Color(0xFFFF7A00)))),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "$opponentScore  ",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "VS $opponentName",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * Animated Gift Banner that flies across the screen
 */
@Composable
fun LiveGiftFlyBanner(giftEvent: LiveGiftEvent) {
    Surface(
        color = Color(0xDD0F172A),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(GoldYellow, TikTokRed, TikTokCyan))),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdtAvatar(name = giftEvent.senderName, sizeDp = 38)

            Column {
                Text(
                    text = giftEvent.senderName,
                    color = GoldYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "a envoyé ${giftEvent.gift.name}",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }

            Text(
                text = giftEvent.gift.icon,
                fontSize = 32.sp
            )

            // Combo multiplier badge
            Surface(
                color = TikTokRed,
                shape = CircleShape,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "x${giftEvent.combo}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * Single live chat comment bubble
 */
@Composable
fun LiveCommentBubble(comment: LiveComment) {
    Surface(
        color = if (comment.isSystem) Color(0x6600A884) else Color(0x88000000),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Badge (Modérateur, VIP, Top Fan)
            if (comment.badge != null) {
                Surface(
                    color = Color(comment.badgeColorHex),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = comment.badge,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            // Sender name in distinctive TikTok color
            Text(
                text = comment.senderName,
                color = if (comment.isSystem) Color(0xFF25D366) else Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // Comment text
            Text(
                text = comment.text,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Bottom controls bar for TikTok live (Comment input, Gift, Reactions, Host Tools, Share)
 */
@Composable
fun LiveBottomControlsBar(
    session: LiveStreamSessionState,
    onCommentClick: () -> Unit,
    onGiftClick: () -> Unit,
    onShareClick: () -> Unit,
    onHostToolsClick: () -> Unit,
    onQuickLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Comment Input Pill Trigger
        Surface(
            color = Color(0x88000000),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clickable { onCommentClick() }
                .testTag("btn_open_comment_bar")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Ajouter un commentaire...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        // Host Tools if Host
        if (session.isHost) {
            IconButton(
                onClick = onHostToolsClick,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0x88000000), CircleShape)
                    .border(1.dp, TikTokCyan.copy(alpha = 0.5f), CircleShape)
                    .testTag("btn_host_tools")
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Outils du direct",
                    tint = TikTokCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Share button
        IconButton(
            onClick = onShareClick,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0x88000000), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .testTag("btn_share_live")
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = "Partager",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Quick Like button
        IconButton(
            onClick = onQuickLikeClick,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0x88000000), CircleShape)
                .border(1.dp, TikTokRed.copy(alpha = 0.5f), CircleShape)
                .testTag("btn_quick_like")
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Like",
                tint = TikTokRed,
                modifier = Modifier.size(22.dp)
            )
        }

        // TikTok Gifts Box Button
        IconButton(
            onClick = onGiftClick,
            modifier = Modifier
                .size(42.dp)
                .background(
                    Brush.linearGradient(listOf(TikTokRed, Color(0xFFFFD700))),
                    CircleShape
                )
                .testTag("btn_open_gifts")
        ) {
            Icon(
                Icons.Default.CardGiftcard,
                contentDescription = "Envoyer un cadeau",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Floating hearts and like emojis canvas rising smoothly from bottom-right
 */
@Composable
fun FloatingHeartsOverlay(
    hearts: List<LiveHeartBurst>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        hearts.forEach { burst ->
            key(burst.id) {
                FloatingSingleHeart(burst = burst)
            }
        }
    }
}

@Composable
fun FloatingSingleHeart(burst: LiveHeartBurst) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(burst.id) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
    }

    val progress = animProgress.value
    val yOffset = (1f - progress) * 450f + 100f
    val xWiggle = sin(progress * 8f) * 25f
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val scale = (0.5f + progress * 0.8f).coerceAtMost(1.2f)

    val safeBottom = yOffset.coerceAtLeast(0f)
    val safeEnd = ((1f - burst.xOffset) * 200f + xWiggle).coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = safeBottom.dp, end = safeEnd.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Text(
            text = burst.emoji,
            fontSize = 26.sp,
            modifier = Modifier
                .scale(scale)
                .blur(if (progress > 0.85f) 2.dp else 0.dp),
            color = Color.White.copy(alpha = alpha)
        )
    }
}

/**
 * TikTok Live Gift Drawer Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TikTokGiftBottomSheet(
    coinsBalance: Int,
    gifts: List<LiveGift>,
    selectedGift: LiveGift,
    onSelectGift: (LiveGift) -> Unit,
    onSendGift: () -> Unit,
    onRechargeCoins: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Coins Balance & Recharge Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🎁 Cadeaux en Direct",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Recharge free trial coins button
                    FilledTonalButton(
                        onClick = onRechargeCoins,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = GoldYellow.copy(alpha = 0.2f),
                            contentColor = GoldYellow
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("btn_recharge_coins")
                    ) {
                        Text("+500 🪙 Recharger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Balance Chip
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GoldYellow)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🪙", fontSize = 12.sp)
                            Text(
                                text = "$coinsBalance",
                                color = GoldYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Gifts Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(220.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(gifts) { gift ->
                    val isSelected = gift.id == selectedGift.id
                    Surface(
                        color = if (isSelected) Color(0xFF334155) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) GoldYellow else Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .clickable { onSelectGift(gift) }
                            .testTag("gift_item_${gift.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = gift.icon, fontSize = 28.sp)
                            Text(
                                text = gift.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("🪙", fontSize = 10.sp)
                                Text(
                                    text = "${gift.coins}",
                                    color = GoldYellow,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Send button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onSendGift,
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("btn_send_selected_gift")
                ) {
                    Text(
                        text = "Envoyer (${selectedGift.coins} 🪙)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Host Tools Sheet (Camera switch, mute, filters, beauty, PK battle)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostToolsBottomSheet(
    session: LiveStreamSessionState,
    onToggleCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onSelectFilter: (String) -> Unit,
    onBeautyChange: (Float) -> Unit,
    onTogglePk: () -> Unit,
    onEndLive: () -> Unit,
    onDismiss: () -> Unit
) {
    val filters = listOf("Naturel", "Éclat", "Chaud", "Cinéma", "Studio", "Cyber")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚙️ Studio & Outils du Direct",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )

            // Quick toggles row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Switch Camera
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleCamera,
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Changer caméra", tint = TikTokCyan)
                    }
                    Text(if (session.isFrontCamera) "Cam avant" else "Cam arrière", color = Color.White, fontSize = 11.sp)
                }

                // Mute Mic
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleMic,
                        modifier = Modifier
                            .size(50.dp)
                            .background(if (session.isMicMuted) CallRed else Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(
                            if (session.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Micro",
                            tint = Color.White
                        )
                    }
                    Text(if (session.isMicMuted) "Coupé" else "Micro actif", color = Color.White, fontSize = 11.sp)
                }

                // PK Battle Toggle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onTogglePk,
                        modifier = Modifier
                            .size(50.dp)
                            .background(if (session.isPkBattleActive) GoldYellow else Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.SportsKabaddi,
                            contentDescription = "PK Battle",
                            tint = if (session.isPkBattleActive) Color.Black else Color.White
                        )
                    }
                    Text(if (session.isPkBattleActive) "PK Actif" else "Lancer PK", color = Color.White, fontSize = 11.sp)
                }
            }

            // Filters selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Filtres vidéo IA", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { f ->
                        val isSel = f == session.activeFilter
                        FilterChip(
                            selected = isSel,
                            onClick = { onSelectFilter(f) },
                            label = { Text(f, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TikTokRed,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Beauty Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lissage & Beauté du visage", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Text("${(session.beautyLevel * 100).toInt()}%", color = TikTokCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = session.beautyLevel,
                    onValueChange = onBeautyChange,
                    colors = SliderDefaults.colors(
                        thumbColor = TikTokCyan,
                        activeTrackColor = TikTokCyan
                    )
                )
            }

            // End Live Broadcast Button
            Button(
                onClick = onEndLive,
                colors = ButtonDefaults.buttonColors(containerColor = CallRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_end_live_stream"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.StopCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Terminer la diffusion en direct", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

/**
 * Direct Autorisé Verification & Compliance Dialog
 */
@Composable
fun LiveAuthorizationBadgeDialog(
    creatorAuth: LiveCreatorAuthorization,
    stream: LiveStreamItem,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, Color(0xFF25D366))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Direct Autorisé ADT Pro",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Text(
                    text = "Ce direct respecte toutes les règles de certification en direct. L'hôte ${stream.hostName} possède un badge de créateur autorisé conforme aux standards de sécurité.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        creatorAuth.authorizedFeatures.forEach { feat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(14.dp))
                                Text(feat, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Compris", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Share live stream dialog
 */
@Composable
fun ShareLiveStreamDialog(
    stream: LiveStreamItem,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Partager le direct",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = "Partagez le lien du live de ${stream.hostName} avec vos contacts ADT Pro ou sur les réseaux sociaux.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF25D366), CircleShape)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "Discussions", tint = Color.White)
                        }
                        Text("Discussions", color = Color.White, fontSize = 10.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF0088CC), CircleShape)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Message", tint = Color.White)
                        }
                        Text("Contact", color = Color.White, fontSize = 10.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF475569), CircleShape)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copier lien", tint = Color.White)
                        }
                        Text("Copier lien", color = Color.White, fontSize = 10.sp)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fermer", color = Color.White)
                }
            }
        }
    }
}

/**
 * Live Ended Summary Dialog with statistics
 */
@Composable
fun LiveEndedSummaryDialog(
    session: LiveStreamSessionState,
    durationFormatted: String,
    onFinish: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, TikTokRed)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🔴 Direct Terminé", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)

                Text(
                    text = "Statistiques du direct de ${session.stream.hostName}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                // Stats Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("⏱️ Durée de diffusion", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text(durationFormatted, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("👁️ Spectateurs cumulés", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text("${session.viewerCount + 850}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("❤️ Total J'aime", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text(formatCount(session.likesCount), color = TikTokRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("💎 Diamants / Cadeaux", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text("${session.diamondsEarned} 💎", color = GoldYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_finish_live_summary")
                ) {
                    Text("Terminer et quitter", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Pre-Live Setup Dialog ("Lancer un direct autorisé")
 */
@Composable
fun StartLiveBroadcastDialog(
    creatorAuth: LiveCreatorAuthorization,
    onStartLive: (title: String, category: LiveCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(LiveCategory.DISCUSSION) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, TikTokRed)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔴", fontSize = 20.sp)
                    Text(
                        text = "Lancer un Direct Autorisé",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                // Authorization banner
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF25D366))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                        Column {
                            Text("Autorisation Live accordée", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Votre compte est certifié pour diffuser en direct.", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        }
                    }
                }

                // Title input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Titre de votre direct", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Ex: Discussion du soir, Show DJ, Vente flash...", fontSize = 12.sp, color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_live_title")
                    )
                }

                // Category selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Catégorie", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(LiveCategory.values()) { cat ->
                            val isSel = cat == selectedCategory
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategory = cat },
                                label = { Text("${cat.icon} ${cat.label}", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TikTokRed,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        onStartLive(
                            title.ifBlank { "Direct Autorisé ADT Pro" },
                            selectedCategory
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_start_live_confirm")
                ) {
                    Text("PASSER EN DIRECT MAINTENANT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Annuler", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun LiveMusicTicker(
    musicTrack: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "music_disk")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disk_rotation"
    )

    Surface(
        color = Color(0x88000000),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = TikTokCyan,
                modifier = Modifier
                    .size(15.dp)
                    .rotate(rotation)
            )
            Text(
                text = musicTrack,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatCount(count: Int): String {
    return if (count >= 1_000_000) {
        String.format("%.1fM", count / 1_000_000f)
    } else if (count >= 1_000) {
        String.format("%.1fK", count / 1_000f)
    } else {
        count.toString()
    }
}
