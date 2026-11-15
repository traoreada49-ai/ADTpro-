package com.example.ui.screens.media

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CameraEffect
import com.example.data.model.CameraEffectsData
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CaptureMode {
    PHOTO,
    VIDEO
}

enum class FlashState(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    AUTO(Icons.Default.FlashAuto, "Auto"),
    ON(Icons.Default.FlashOn, "Activé"),
    OFF(Icons.Default.FlashOff, "Désactivé"),
    TORCH(Icons.Default.Highlight, "Torche")
}

data class CapturedMediaData(
    val isVideo: Boolean,
    val effect: CameraEffect,
    val durationSeconds: Int = 0,
    val previewBitmap: Bitmap? = null,
    val mockSceneTitle: String = "Scène Studio ADT Pro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraStudioScreen(viewModel: AdtViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val cameraSourceChatId by viewModel.cameraSourceChatId.collectAsState()
    val cameraForStatus by viewModel.cameraForStatus.collectAsState()

    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var selectedEffect by remember { mutableStateOf(CameraEffectsData.CITRON) }
    var flashState by remember { mutableStateOf(FlashState.AUTO) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var showGrid by remember { mutableStateOf(true) }

    // Video recording states
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    // Focus indicator animation
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusAnimAlpha = remember { Animatable(0f) }

    // Flash shutter animation
    val shutterFlashAlpha = remember { Animatable(0f) }

    // Captured media review state
    var capturedMedia by remember { mutableStateOf<CapturedMediaData?>(null) }
    var mediaCaption by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            toastMessage = null
        }
    }

    // Video timer loop
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording && recordingSeconds < 60) {
                delay(1000)
                recordingSeconds++
            }
            if (isRecording && recordingSeconds >= 60) {
                // Auto stop recording at 60s
                isRecording = false
                capturedMedia = CapturedMediaData(
                    isVideo = true,
                    effect = selectedEffect,
                    durationSeconds = 60
                )
            }
        }
    }

    // Native Camera Launcher
    val nativeCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedMedia = CapturedMediaData(
                isVideo = false,
                effect = selectedEffect,
                previewBitmap = bitmap,
                mockSceneTitle = "Photo Appareil Système"
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // VIEWPORT / VIEWFINDER
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            focusPoint = offset
                            coroutineScope.launch {
                                focusAnimAlpha.snapTo(1f)
                                focusAnimAlpha.animateTo(0f, animationSpec = tween(1200))
                                focusPoint = null
                            }
                        }
                    }
            ) {
                // Base viewfinder background simulating camera lens input
                CameraPreviewCanvas(
                    isFrontCamera = isFrontCamera,
                    zoomLevel = zoomLevel,
                    effect = selectedEffect,
                    isRecording = isRecording
                )

                // Live Color Filter Matrix overlay applied to the viewfinder
                Surface(
                    color = selectedEffect.overlayColor,
                    modifier = Modifier.fillMaxSize()
                ) {}

                // Rule-of-thirds grid
                if (showGrid) {
                    CameraGridView()
                }

                // Focus reticle animation
                focusPoint?.let { pt ->
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = selectedEffect.accentColor.copy(alpha = focusAnimAlpha.value),
                            radius = 42f,
                            center = pt,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                        )
                        drawCircle(
                            color = selectedEffect.accentColor.copy(alpha = focusAnimAlpha.value * 0.5f),
                            radius = 6f,
                            center = pt
                        )
                    }
                }

                // Shutter white flash
                if (shutterFlashAlpha.value > 0.01f) {
                    Surface(
                        color = Color.White.copy(alpha = shutterFlashAlpha.value),
                        modifier = Modifier.fillMaxSize()
                    ) {}
                }
            }

            // TOP BAR CONTROLS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = {
                            viewModel.navigateTo(
                                if (cameraSourceChatId != null) AppScreen.CHAT_ROOM else AppScreen.MAIN_TABS
                            )
                        },
                        modifier = Modifier.testTag("btn_camera_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }

                    // Recording indicator or Effect pill
                    if (isRecording) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE53935),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    text = String.format("REC %02d:%02d", recordingSeconds / 60, recordingSeconds % 60),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, selectedEffect.accentColor.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(selectedEffect.emoji, fontSize = 14.sp)
                                Text(
                                    text = "Effet : ${selectedEffect.name}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Quick settings action row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Flash button
                        IconButton(
                            onClick = {
                                flashState = when (flashState) {
                                    FlashState.AUTO -> FlashState.ON
                                    FlashState.ON -> FlashState.OFF
                                    FlashState.OFF -> FlashState.TORCH
                                    FlashState.TORCH -> FlashState.AUTO
                                }
                            }
                        ) {
                            Icon(flashState.icon, contentDescription = flashState.label, tint = Color.White)
                        }

                        // Grid toggle
                        IconButton(onClick = { showGrid = !showGrid }) {
                            Icon(
                                if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                                contentDescription = "Grille",
                                tint = if (showGrid) AdtPrimary else Color.White
                            )
                        }

                        // System Camera Hardware Bridge
                        IconButton(onClick = { nativeCameraLauncher.launch(null) }) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Appareil photo système", tint = Color.White)
                        }
                    }
                }
            }

            // BOTTOM CONTROLS & EFFECTS CAROUSEL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                        )
                    )
                    .padding(bottom = 20.dp, top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom buttons (0.5x, 1x, 2x)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    listOf(0.5f to "0.5x", 1.0f to "1x", 2.0f to "2x").forEach { (level, label) ->
                        Surface(
                            shape = CircleShape,
                            color = if (zoomLevel == level) selectedEffect.accentColor else Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { zoomLevel = level }
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (zoomLevel == level) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                // Live Filter Carousel Strip
                Text(
                    "EFFETS COULEUR : CITRON • FRAISIER • ORANGE • WHITE • NOIR & BLANC • BLEU",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    items(CameraEffectsData.allEffects, key = { it.id }) { effect ->
                        val isSelected = selectedEffect.id == effect.id
                        FilterChipSwatch(
                            effect = effect,
                            isSelected = isSelected,
                            onClick = { selectedEffect = effect }
                        )
                    }
                }

                // Shutter & Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Wallpapers quick shortcut button
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { viewModel.openWallpaperPicker() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Wallpaper, contentDescription = "Fonds d'écran", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }

                    // SHUTTER BUTTON
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(3.dp, Color.White, CircleShape)
                            .clickable {
                                if (captureMode == CaptureMode.PHOTO) {
                                    // Take Photo
                                    coroutineScope.launch {
                                        shutterFlashAlpha.snapTo(1f)
                                        shutterFlashAlpha.animateTo(0f, animationSpec = tween(250))
                                    }
                                    capturedMedia = CapturedMediaData(
                                        isVideo = false,
                                        effect = selectedEffect,
                                        mockSceneTitle = "Photo Effet « ${selectedEffect.name} »"
                                    )
                                } else {
                                    // Toggle Video Recording
                                    if (isRecording) {
                                        isRecording = false
                                        capturedMedia = CapturedMediaData(
                                            isVideo = true,
                                            effect = selectedEffect,
                                            durationSeconds = recordingSeconds
                                        )
                                    } else {
                                        isRecording = true
                                    }
                                }
                            }
                            .testTag("btn_camera_shutter"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (captureMode == CaptureMode.PHOTO) {
                            Surface(
                                shape = CircleShape,
                                color = selectedEffect.accentColor,
                                modifier = Modifier.size(62.dp)
                            ) {}
                        } else {
                            if (isRecording) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE53935),
                                    modifier = Modifier.size(28.dp)
                                ) {}
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE53935),
                                    modifier = Modifier.size(62.dp)
                                ) {}
                            }
                        }
                    }

                    // Flip Camera (Front / Rear)
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { isFrontCamera = !isFrontCamera }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Changer de caméra", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PHOTO / VIDEO MODE SELECTOR
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PHOTO",
                        fontWeight = if (captureMode == CaptureMode.PHOTO) FontWeight.Bold else FontWeight.Normal,
                        color = if (captureMode == CaptureMode.PHOTO) selectedEffect.accentColor else Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                if (!isRecording) captureMode = CaptureMode.PHOTO
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Text(
                        text = "VIDÉO",
                        fontWeight = if (captureMode == CaptureMode.VIDEO) FontWeight.Bold else FontWeight.Normal,
                        color = if (captureMode == CaptureMode.VIDEO) Color(0xFFE53935) else Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                if (!isRecording) captureMode = CaptureMode.VIDEO
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    // CAPTURED MEDIA PREVIEW & SHARING DIALOG
    capturedMedia?.let { media ->
        CapturedMediaReviewDialog(
            media = media,
            initialCaption = mediaCaption,
            onCaptionChange = { mediaCaption = it },
            onDismiss = { capturedMedia = null },
            onSendToChat = {
                val chatId = cameraSourceChatId ?: "chat_moussa"
                val mediaType = if (media.isVideo) "VIDEO" else "PHOTO"
                val captionText = mediaCaption.ifBlank {
                    if (media.isVideo) "🎥 Vidéo avec effet ${media.effect.name}" else "📷 Photo avec effet ${media.effect.name}"
                }
                viewModel.sendPhotoOrVideoMessage(
                    chatId = chatId,
                    caption = captionText,
                    mediaType = mediaType,
                    durationSeconds = media.durationSeconds
                )
                toastMessage = "Média envoyé dans la discussion !"
                capturedMedia = null
                viewModel.navigateTo(AppScreen.CHAT_ROOM)
            },
            onPublishStatus = {
                val statusText = mediaCaption.ifBlank {
                    if (media.isVideo) "🎥 Vidéo Studio (${media.effect.name})" else "📷 Photo Studio (${media.effect.name})"
                }
                val bgHex = when (media.effect.id) {
                    "CITRON" -> "#FFD600"
                    "FRAISIER" -> "#FF4081"
                    "ORANGE" -> "#FF6D00"
                    "WHITE" -> "#ECEFF1"
                    "NOIR_BLANC" -> "#212121"
                    "BLEU" -> "#00B0FF"
                    "EMERAUDE" -> "#00E676"
                    "VINTAGE" -> "#8D6E63"
                    "NEON" -> "#E040FB"
                    else -> "#075E54"
                }
                viewModel.createStatusStory(statusText, bgHex)
                toastMessage = "Publié dans vos Statuts ADT Pro !"
                capturedMedia = null
                viewModel.navigateTo(AppScreen.MAIN_TABS)
            },
            onSaveGallery = {
                toastMessage = "Enregistré avec succès dans votre galerie !"
            }
        )
    }
}

@Composable
fun FilterChipSwatch(
    effect: CameraEffect,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) effect.accentColor else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = effect.accentColor.copy(alpha = 0.25f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(effect.emoji, fontSize = 22.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = effect.name,
            color = if (isSelected) effect.accentColor else Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CameraPreviewCanvas(
    isFrontCamera: Boolean,
    zoomLevel: Float,
    effect: CameraEffect,
    isRecording: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Simulated camera depth background
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF15191C), Color(0xFF0D1012), Color(0xFF1A1F24))
            )
        )

        // Geometric studio guides simulating studio lighting
        val glowColor = effect.accentColor.copy(alpha = 0.15f * zoomLevel)
        drawCircle(glowColor, radius = width * 0.45f * zoomLevel, center = Offset(width * 0.5f, height * 0.4f))

        // Center silhouette portrait outline to represent person or subject
        val headCenter = Offset(width * 0.5f, height * 0.36f)
        drawCircle(
            color = Color.White.copy(alpha = 0.20f),
            radius = 65f * zoomLevel,
            center = headCenter
        )
        // Shoulders contour
        drawOval(
            color = Color.White.copy(alpha = 0.15f),
            topLeft = Offset(width * 0.25f, height * 0.46f),
            size = androidx.compose.ui.geometry.Size(width * 0.5f, height * 0.35f)
        )

        // Front / Back camera indicator watermark
        val cameraModeText = if (isFrontCamera) "CAMÉRA AVANT (SELFIE)" else "CAMÉRA PRINCIPALE"
    }
}

@Composable
fun CameraGridView() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val gridColor = Color.White.copy(alpha = 0.15f)

        // Vertical lines
        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1f)
        drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = 1f)

        // Horizontal lines
        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = 1f)
    }
}

@Composable
fun CapturedMediaReviewDialog(
    media: CapturedMediaData,
    initialCaption: String,
    onCaptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSendToChat: () -> Unit,
    onPublishStatus: () -> Unit,
    onSaveGallery: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top review header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Recommencer", tint = Color.White)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = media.effect.accentColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, media.effect.accentColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(media.effect.emoji, fontSize = 14.sp)
                        Text(
                            "Effet ${media.effect.name} appliqué",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onSaveGallery) {
                    Icon(Icons.Default.Download, contentDescription = "Enregistrer", tint = Color.White)
                }
            }

            // Preview viewport container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF212529), Color(0xFF101214)))),
                contentAlignment = Alignment.Center
            ) {
                if (media.previewBitmap != null) {
                    Image(
                        bitmap = media.previewBitmap.asImageBitmap(),
                        contentDescription = "Photo capturée",
                        colorFilter = ColorFilter.colorMatrix(media.effect.colorMatrix),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Studio rendered preview
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = media.effect.accentColor.copy(alpha = 0.25f),
                            modifier = Modifier.size(90.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(media.effect.emoji, fontSize = 42.sp)
                            }
                        }
                        Text(
                            if (media.isVideo) "Vidéo capturée (${media.durationSeconds}s)" else "Photo capturée avec succès",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            media.effect.description,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }

            // Caption input
            OutlinedTextField(
                value = initialCaption,
                onValueChange = onCaptionChange,
                placeholder = { Text("Ajouter une légende...", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = media.effect.accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedContainerColor = Color(0xFF1A1F24),
                    unfocusedContainerColor = Color(0xFF1A1F24)
                ),
                singleLine = true
            )

            // Action buttons row
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSendToChat,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Envoyer dans la discussion", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = onPublishStatus,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = media.effect.accentColor)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publier dans mes Statuts (24h)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}
