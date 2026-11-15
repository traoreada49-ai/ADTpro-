package com.example.ui.screens.status

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.StatusEntity
import com.example.ui.components.AdtAvatar
import com.example.ui.theme.AdtAccentGreen
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

private data class FloatingReaction(
    val id: Long = System.nanoTime(),
    val emoji: String,
    val xOffsetFraction: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusViewerScreen(viewModel: AdtViewModel) {
    val activeStatusEntity by viewModel.activeStatus.collectAsState()
    val allStatusesList by viewModel.allStatuses.collectAsState()

    if (activeStatusEntity == null) return

    // Filter valid 24h statuses
    val now = remember { System.currentTimeMillis() }
    val validStatuses = remember(allStatusesList) {
        allStatusesList.filter { it.timestamp >= (now - 24L * 3600L * 1000L) }
    }

    // Group stories by author or build the continuous sequence
    val authorStatuses = remember(validStatuses, activeStatusEntity?.authorPhone) {
        val matching = validStatuses.filter { it.authorPhone == activeStatusEntity?.authorPhone }
        if (matching.isNotEmpty()) matching else listOfNotNull(activeStatusEntity)
    }

    val currentIndex = remember(authorStatuses, activeStatusEntity?.id) {
        val idx = authorStatuses.indexOfFirst { it.id == activeStatusEntity?.id }
        if (idx >= 0) idx else 0
    }

    val currentStatus = authorStatuses.getOrElse(currentIndex) { activeStatusEntity!! }

    var isNavigatingForward by remember { mutableStateOf(true) }
    var replyText by remember { mutableStateOf("") }
    var isPaused by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var floatingReactions by remember { mutableStateOf<List<FloatingReaction>>(emptyList()) }

    // Each story runs for 5500 ms
    val storyDurationMs = 5500L
    val progress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Handle back button press
    BackHandler {
        viewModel.navigateTo(AppScreen.MAIN_TABS)
    }

    // Remaining duration in seconds calculation
    val remainingSeconds = remember(progress.value) {
        val remainingRatio = (1f - progress.value).coerceIn(0f, 1f)
        (remainingRatio * (storyDurationMs / 1000f))
    }

    // Calculate remaining hours and minutes until 24-hour expiration
    val remainingHours = remember(currentStatus.timestamp) {
        val expireTime = currentStatus.timestamp + (24L * 60L * 60L * 1000L)
        val diffMs = expireTime - System.currentTimeMillis()
        if (diffMs <= 0) 0 else (diffMs / (1000 * 60 * 60)).toInt()
    }

    val remainingMinutes = remember(currentStatus.timestamp) {
        val expireTime = currentStatus.timestamp + (24L * 60L * 60L * 1000L)
        val diffMs = expireTime - System.currentTimeMillis()
        if (diffMs <= 0) 0 else ((diffMs / (1000 * 60)) % 60).toInt()
    }

    val postedTimeFormatted = remember(currentStatus.timestamp) {
        val date = Date(currentStatus.timestamp)
        val diffHours = ((System.currentTimeMillis() - currentStatus.timestamp) / (1000 * 3600)).toInt()
        when {
            diffHours < 1 -> "À l'instant"
            diffHours < 24 -> "Aujourd'hui à ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
            else -> SimpleDateFormat("dd/MM à HH:mm", Locale.getDefault()).format(date)
        }
    }

    fun goToNextStory() {
        if (currentIndex < authorStatuses.size - 1) {
            isNavigatingForward = true
            val nextStatus = authorStatuses[currentIndex + 1]
            viewModel.switchActiveStatus(nextStatus)
        } else {
            // Check if there is another contact with statuses
            val distinctAuthors = validStatuses.map { it.authorPhone }.distinct()
            val currentAuthorIdx = distinctAuthors.indexOf(currentStatus.authorPhone)
            if (currentAuthorIdx >= 0 && currentAuthorIdx < distinctAuthors.size - 1) {
                val nextAuthorPhone = distinctAuthors[currentAuthorIdx + 1]
                val nextAuthorStatus = validStatuses.firstOrNull { it.authorPhone == nextAuthorPhone }
                if (nextAuthorStatus != null) {
                    isNavigatingForward = true
                    viewModel.switchActiveStatus(nextAuthorStatus)
                    return
                }
            }
            viewModel.navigateTo(AppScreen.MAIN_TABS)
        }
    }

    fun goToPreviousStory() {
        if (progress.value > 0.25f) {
            // Restart current story progress if already advanced
            coroutineScope.launch {
                progress.snapTo(0f)
            }
        } else if (currentIndex > 0) {
            isNavigatingForward = false
            val prevStatus = authorStatuses[currentIndex - 1]
            viewModel.switchActiveStatus(prevStatus)
        } else {
            // If at the very beginning, restart
            coroutineScope.launch {
                progress.snapTo(0f)
            }
        }
    }

    // Auto-advance progress timer
    LaunchedEffect(currentStatus.id) {
        progress.snapTo(0f)
    }

    LaunchedEffect(currentStatus.id, isPaused) {
        if (!isPaused) {
            val remainingTarget = 1f
            val currentVal = progress.value
            val durationLeft = ((1f - currentVal) * storyDurationMs).toLong()
            if (durationLeft > 50) {
                progress.animateTo(
                    targetValue = remainingTarget,
                    animationSpec = tween(durationMillis = durationLeft.toInt(), easing = LinearEasing)
                )
                if (progress.value >= 0.99f) {
                    goToNextStory()
                }
            }
        }
    }

    // Subtle Ken Burns scale effect for photo statuses
    val photoScale by animateFloatAsState(
        targetValue = if (progress.value > 0.05f) 1.07f else 1.0f,
        animationSpec = tween(durationMillis = storyDurationMs.toInt(), easing = LinearEasing),
        label = "photo_zoom"
    )

    // Parse background color
    val parsedBgColor = remember(currentStatus.backgroundColorHex) {
        try {
            Color(android.graphics.Color.parseColor(currentStatus.backgroundColorHex))
        } catch (e: Exception) {
            Color(0xFF075E54)
        }
    }

    // Background gradient overlay
    val gradientBrush = remember(parsedBgColor) {
        Brush.verticalGradient(
            colors = listOf(
                parsedBgColor,
                parsedBgColor.copy(alpha = 0.85f),
                Color.Black.copy(alpha = 0.92f)
            )
        )
    }

    val quickReactions = remember { listOf("❤️", "🔥", "😂", "😮", "👏", "🎉") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Vertical swipe down to close
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 50) {
                        viewModel.navigateTo(AppScreen.MAIN_TABS)
                    }
                }
            }
    ) {
        // ANIMATED STORY CONTENT TRANSITION (Slide in / Slide out)
        AnimatedContent(
            targetState = currentStatus,
            transitionSpec = {
                if (isNavigatingForward) {
                    (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> width } +
                            fadeIn(animationSpec = tween(280))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> -width } +
                                    fadeOut(animationSpec = tween(240)))
                } else {
                    (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> -width } +
                            fadeIn(animationSpec = tween(280))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> width } +
                                    fadeOut(animationSpec = tween(240)))
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "story_slide_transition"
        ) { statusItem ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (statusItem.mediaUrl.isNullOrBlank()) parsedBgColor else Color.Black)
            ) {
                if (!statusItem.mediaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = statusItem.mediaUrl,
                        contentDescription = "Photo du statut",
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(photoScale),
                        contentScale = ContentScale.Crop
                    )
                    // Translucent scrims to enhance text and controls readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.7f),
                                    0.25f to Color.Transparent,
                                    0.65f to Color.Black.copy(alpha = 0.35f),
                                    1.0f to Color.Black.copy(alpha = 0.85f)
                                )
                            )
                    )
                } else {
                    // Decorative stylish backdrop for text stories
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradientBrush)
                    )
                }

                // Center Story Content (Text or Caption)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (statusItem.text.isNotBlank()) {
                        Surface(
                            color = if (statusItem.mediaUrl.isNullOrBlank()) Color.Transparent else Color.Black.copy(alpha = 0.62f),
                            shape = RoundedCornerShape(20.dp),
                            border = if (!statusItem.mediaUrl.isNullOrBlank()) {
                                androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(
                                text = statusItem.text,
                                color = Color.White,
                                fontSize = if (statusItem.mediaUrl.isNullOrBlank()) 26.sp else 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = if (statusItem.mediaUrl.isNullOrBlank()) 36.sp else 25.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                            )
                        }
                    }
                }
            }
        }

        // TOUCH GESTURE ZONES (Left 30% = Previous, Right 70% = Next, Long Press = Pause)
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.32f)
                    .fillMaxHeight()
                    .pointerInput(currentStatus.id) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = {
                                goToPreviousStory()
                            }
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight()
                    .pointerInput(currentStatus.id) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = {
                                goToNextStory()
                            }
                        )
                    }
            )
        }

        // FLOATING ANIMATED REACTIONS OVERLAY
        floatingReactions.forEach { reaction ->
            key(reaction.id) {
                var animStarted by remember { mutableStateOf(false) }
                LaunchedEffect(reaction.id) {
                    animStarted = true
                }
                val yOffset by animateFloatAsState(
                    targetValue = if (animStarted) -350f else 0f,
                    animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                    label = "reaction_y"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (animStarted) 0f else 1f,
                    animationSpec = tween(durationMillis = 1100, easing = LinearEasing),
                    label = "reaction_alpha"
                )
                val scale by animateFloatAsState(
                    targetValue = if (animStarted) 1.6f else 0.8f,
                    animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                    label = "reaction_scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = reaction.emoji,
                        fontSize = 32.sp,
                        modifier = Modifier
                            .offset(
                                x = ((reaction.xOffsetFraction - 0.5f) * 260).dp,
                                y = yOffset.dp
                            )
                            .alpha(alpha)
                            .scale(scale)
                    )
                }
            }
        }

        // TOP CONTROLS & DURATION PROGRESS BAR (Animated visibility during hold/press)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .alpha(if (isPaused) 0.65f else 1.0f)
                .padding(top = 34.dp, start = 12.dp, end = 12.dp)
        ) {
            // MULTI-SEGMENT PROGRESS BARS INDICATING REMAINING DURATION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                authorStatuses.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> progress.value
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AUTHOR HEADER + STATUS LIFETIME BADGE + REMAINING DURATION PILL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.MAIN_TABS) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    AdtAvatar(name = currentStatus.authorName, sizeDp = 40, hasStatusStory = true)

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentStatus.authorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (authorStatuses.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = "${currentIndex + 1}/${authorStatuses.size}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "$postedTimeFormatted • Expire dans ${remainingHours}h${remainingMinutes}m",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // RIGHT ACTION BUTTONS: DURATION CHIP + PLAY/PAUSE + DELETE + CLOSE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // DURATION REMAINING PILL BADGE
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPaused) Color(0xFFE65100).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPaused) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.Pause else Icons.Default.Timelapse,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isPaused) "Pause" else "${ceil(remainingSeconds).toInt()}s",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Play/Pause toggle
                    IconButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Reprendre" else "Mettre en pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (currentStatus.isSelf) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Supprimer mon statut",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.MAIN_TABS) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // BOTTOM INTERACTIVE SECTION (Reactions, Reply bar, or Views counter)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentStatus.isSelf) {
                // Self Status: Views counter card & viewers trigger
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showViewersSheet = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = AdtAccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${currentStatus.viewsCount} vues",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Détails des vues",
                                color = AdtAccentGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = AdtAccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Quick Floating Emoji Reactions Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickReactions.forEachIndexed { idx, emoji ->
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable {
                                    floatingReactions = floatingReactions + FloatingReaction(
                                        emoji = emoji,
                                        xOffsetFraction = (idx.toFloat() / (quickReactions.size - 1))
                                    )
                                    viewModel.reactToStatus(currentStatus, emoji)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // Reply Input Bar
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(26.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = {
                                Text(
                                    "Répondre au statut de ${currentStatus.authorName}...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    viewModel.replyToStatus(currentStatus, replyText)
                                    replyText = ""
                                }
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Envoyer",
                                tint = if (replyText.isNotBlank()) AdtAccentGreen else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    // VIEWERS BOTTOM SHEET (Self status only)
    if (showViewersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showViewersSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vues de votre statut (${currentStatus.viewsCount})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AdtAccentGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Visible 24h",
                            color = AdtAccentGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "Seuls les contacts enregistrés dans votre carnet qui ont consulté votre statut apparaissent ici.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                if (currentStatus.viewsCount == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Pas encore de vues",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(minOf(currentStatus.viewsCount, 6)) { idx ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AdtAvatar(name = "Contact ${idx + 1}", sizeDp = 36)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Contact ${idx + 1}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Vu il y a ${(idx + 1) * 8} min",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = AdtAccentGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // DELETE CONFIRMATION DIALOG
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer cette actualité ?") },
            text = { Text("Cette publication éphémère sera immédiatement et définitivement effacée pour tous vos contacts.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteStatus(currentStatus.id)
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStatusScreen(viewModel: AdtViewModel) {
    var statusText by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri.toString()
        }
    }

    val colorOptions = remember {
        listOf(
            "#E53935", // Rouge
            "#1E88E5", // Bleu
            "#FFFFFF", // Blanche
            "#121212", // Noir
            "#8E24AA", // Violet
            "#43A047", // Vert
            "#EC407A", // Rose (Ross)
            "#075E54"  // Émeraude ADT
        )
    }

    val colorNames = remember {
        listOf(
            "Rouge",
            "Bleu",
            "Blanche",
            "Noir",
            "Violet",
            "Vert",
            "Rose (Ross)",
            "Émeraude"
        )
    }

    val activeBgHex = colorOptions[selectedColorIndex]
    val isLightBg = activeBgHex.equals("#FFFFFF", ignoreCase = true)
    val activeBg = Color(android.graphics.Color.parseColor(activeBgHex))
    val statusContentColor = if (isLightBg) Color(0xFF111827) else Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (selectedImageUri != null) "Statut Photo (24h)" else "Nouveau statut texte (24h)",
                            color = statusContentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "Disparaît automatiquement après 24 heures",
                            color = statusContentColor.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.MAIN_TABS) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = statusContentColor)
                    }
                },
                actions = {
                    // Pick photo button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Ajouter une photo", tint = statusContentColor)
                    }
                    if (selectedImageUri != null) {
                        IconButton(onClick = { selectedImageUri = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "Retirer la photo", tint = statusContentColor)
                        }
                    } else {
                        IconButton(onClick = {
                            selectedColorIndex = (selectedColorIndex + 1) % colorOptions.size
                        }) {
                            Icon(Icons.Default.Palette, contentDescription = "Changer la couleur", tint = statusContentColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeBg)
            )
        },
        floatingActionButton = {
            if (statusText.isNotBlank() || selectedImageUri != null) {
                FloatingActionButton(
                    onClick = {
                        if (selectedImageUri != null) {
                            viewModel.postPhotoStatus(
                                caption = statusText,
                                photoUri = selectedImageUri!!,
                                bgHex = colorOptions[selectedColorIndex]
                            )
                        } else {
                            viewModel.postNewStatus(statusText, colorOptions[selectedColorIndex])
                        }
                    },
                    containerColor = AdtPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("btn_publish_status")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Publier")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(activeBg)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // If image is picked, show full background photo
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Photo sélectionnée",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top badge explaining 24h expiration
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Timelapse, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(
                            "Visible par vos contacts pendant 24h",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Center Input Text
                TextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    placeholder = {
                        Text(
                            if (selectedImageUri != null) "Ajouter une légende à votre photo..." else "Tapez un statut pour vos contacts...",
                            color = statusContentColor.copy(alpha = 0.65f),
                            fontSize = if (selectedImageUri != null) 20.sp else 24.sp,
                            textAlign = TextAlign.Center
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        color = statusContentColor,
                        fontSize = if (selectedImageUri != null) 22.sp else 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_status_text")
                )

                // Quick Color Swatches Palette (Rouge, Bleu, Blanche, Noir, Violet, Vert, Rose)
                if (selectedImageUri == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "Couleur : ${colorNames[selectedColorIndex]}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            items(colorOptions.indices.toList()) { index ->
                                val hex = colorOptions[index]
                                val itemColor = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedColorIndex == index
                                Surface(
                                    onClick = { selectedColorIndex = index },
                                    shape = CircleShape,
                                    color = itemColor,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 3.dp else 1.5.dp,
                                        color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.8f)
                                    ),
                                    modifier = Modifier.size(if (isSelected) 40.dp else 32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = colorNames[index],
                                                tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = statusContentColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusContentColor.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ajouter une photo")
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }
    }
}
