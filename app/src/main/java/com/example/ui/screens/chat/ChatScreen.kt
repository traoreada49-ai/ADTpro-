package com.example.ui.screens.chat

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.audio.VoiceToTextManager
import com.example.data.local.entity.MessageEntity
import com.example.ui.util.AppLanguage
import com.example.ui.components.AdtAvatar
import com.example.ui.components.ChatWallpaperBackground
import com.example.ui.components.DoubleTapHeartBurst
import com.example.ui.components.ExtendedEmojiPickerSheet
import com.example.ui.components.LiveLocationMapBubble
import com.example.ui.components.MessageReactionBadge
import com.example.ui.components.QuickReactionFloatingBar
import com.example.ui.components.ShareLiveLocationDialog
import com.example.ui.components.DocumentBubbleContent
import com.example.ui.components.DocumentPickerDialog
import com.example.ui.components.DocumentPreviewDialog
import com.example.ui.components.DocumentSecurityReportDialog
import com.example.data.model.SecureDocument
import com.example.data.model.SyncEnergyMode
import com.example.data.model.EphemeralMessageDuration
import com.example.ui.screens.main.FloatingMiniCallBanner
import com.example.ui.screens.metaai.MetaAiGlowingRing
import com.example.ui.theme.*
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen
import com.example.viewmodel.QuickRepliesUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: AdtViewModel) {
    val activeChatId by viewModel.activeChatId.collectAsState()
    val chats by viewModel.activeChats.collectAsState()
    val lockedChats by viewModel.lockedChats.collectAsState()
    val currentChat = chats.find { it.id == activeChatId } ?: lockedChats.find { it.id == activeChatId }
    val appSettings by viewModel.appSettings.collectAsState()
    val forwardingMessage by viewModel.forwardingMessage.collectAsState()
    val showShareLocation by viewModel.showShareLocationDialog.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()
    val locationShareFromChatId by viewModel.locationShareFromChatId.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val quickReplies by viewModel.quickReplies.collectAsState()
    val isGeneratingQuickReplies by viewModel.isGeneratingQuickReplies.collectAsState()
    val quickRepliesUiState by viewModel.quickRepliesUiState.collectAsState()
    val quickRepliesTone by viewModel.quickRepliesTone.collectAsState()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsState()
    val currentTtsMsgId by viewModel.currentTtsMessageId.collectAsState()
    val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsState()

    // Document Sharing & Safety Scanning state
    val showDocumentPicker by viewModel.showDocumentPicker.collectAsState()
    val isDocUploading by viewModel.isDocumentUploading.collectAsState()
    val docUploadProgress by viewModel.documentUploadProgress.collectAsState()
    val docStatusMsg by viewModel.documentUploadStatusMessage.collectAsState()
    val previewingDoc by viewModel.previewingDocument.collectAsState()
    val inspectingSecurityDoc by viewModel.inspectingSecurityReportDocument.collectAsState()

    // Media Auto-Download & Battery/Data Saver State
    val downloadedMediaMessageIds by viewModel.downloadedMediaMessageIds.collectAsState()
    val downloadingMessageIds by viewModel.downloadingMessageIds.collectAsState()
    val mediaRules by viewModel.mediaAutoDownloadRules.collectAsState()
    val syncEnergyMode by viewModel.syncEnergyMode.collectAsState()

    // Ephemeral Disappearing Messages State
    val chatEphemeralDurations by viewModel.chatEphemeralDurations.collectAsState()
    val currentEphemeralDuration = activeChatId?.let { id ->
        chatEphemeralDurations[id] ?: viewModel.getChatEphemeralDuration(id)
    } ?: EphemeralMessageDuration.OFF
    var showEphemeralDurationDialog by remember { mutableStateOf(false) }

    var messageInput by remember { mutableStateOf("") }
    var replyingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var selectedMessageForMenu by remember { mutableStateOf<MessageEntity?>(null) }
    var messageForInfoDialog by remember { mutableStateOf<MessageEntity?>(null) }
    var messageForEmojiPicker by remember { mutableStateOf<MessageEntity?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordDurationSeconds by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messagesFlow = remember(activeChatId) {
        if (activeChatId != null) viewModel.getMessagesForChat(activeChatId!!) else null
    }
    val messages by (messagesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    // Auto-scroll when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Trigger Firebase AI contextual quick reply generation based on last received message
    LaunchedEffect(messages) {
        viewModel.checkAndGenerateQuickReplies(messages)
    }

    // Audio recording timer simulation
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordDurationSeconds = 0
            while (isRecordingAudio) {
                delay(1000)
                recordDurationSeconds++
            }
        }
    }

    var showTopMenu by remember { mutableStateOf(false) }
    var showSafetyNumberDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val cloudStickers by viewModel.cloudStickers.collectAsState()
    val stickerSyncStatus by viewModel.stickerSyncStatus.collectAsState()

    // Real-time Voice-to-Text SpeechRecognizer Manager
    val voiceToTextManager = remember(context) { VoiceToTextManager(context) }
    val voiceToTextState by voiceToTextManager.state.collectAsState()
    var baseMessageBeforeDictation by remember { mutableStateOf("") }

    DisposableEffect(voiceToTextManager) {
        onDispose { voiceToTextManager.destroy() }
    }

    LaunchedEffect(voiceToTextState.partialText, voiceToTextState.finalText) {
        val transcript = voiceToTextState.finalText.ifBlank { voiceToTextState.partialText }
        if (transcript.isNotBlank()) {
            messageInput = if (baseMessageBeforeDictation.isBlank()) {
                transcript
            } else {
                "$baseMessageBeforeDictation $transcript"
            }
        }
    }

    val speechLanguageTag = remember(appSettings?.language) {
        when (AppLanguage.fromCode(appSettings?.language)) {
            AppLanguage.FR -> "fr-FR"
            AppLanguage.EN -> "en-US"
            AppLanguage.ES -> "es-ES"
            AppLanguage.AR -> "ar-SA"
            AppLanguage.PT -> "pt-PT"
            AppLanguage.RU -> "ru-RU"
            AppLanguage.ZH -> "zh-CN"
            AppLanguage.BM -> "fr-FR"
        }
    }

    val startVoiceToTextDictation = {
        baseMessageBeforeDictation = messageInput.trim()
        voiceToTextManager.startListening(speechLanguageTag)
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceToTextDictation()
        } else {
            Toast.makeText(
                context,
                "Permission microphone requise pour la transcription vocale en temps réel.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val toggleVoiceToText = {
        if (voiceToTextState.isListening) {
            voiceToTextManager.stopListening()
        } else {
            val audioGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (audioGranted) {
                startVoiceToTextDictation()
            } else {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Launch real-time Live Location Sharing with Google Maps API and Location Permission checks
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            Toast.makeText(context, "Permission accordée. Ouverture du partage direct Google Maps...", Toast.LENGTH_SHORT).show()
            viewModel.openLiveLocationSharingScreen(fromChatId = activeChatId)
        } else {
            Toast.makeText(
                context,
                "Permission de localisation requise pour partager votre position en direct.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val requestLiveLocationSharing = {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.openLiveLocationSharingScreen(fromChatId = activeChatId)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clickable {
                                if (currentChat?.isGroup == true) {
                                    viewModel.openGroupInfo()
                                }
                            }
                            .testTag("chat_topbar_title_container")
                    ) {
                        AdtAvatar(
                            name = currentChat?.title ?: "Contact",
                            sizeDp = 38,
                            isOnline = !(currentChat?.isGroup ?: false)
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentChat?.title ?: "Discussion",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (currentEphemeralDuration != EphemeralMessageDuration.OFF) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = AdtPrimary.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .clickable { showEphemeralDurationDialog = true }
                                            .testTag("badge_ephemeral_timer_header")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.HistoryToggleOff,
                                                contentDescription = "Messages éphémères actifs",
                                                tint = AdtPrimary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = currentEphemeralDuration.shortLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AdtPrimary
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = if (currentChat?.isGroup == true) {
                                    "${currentChat.membersCount} membres • Appuyez pour infos"
                                } else {
                                    "En ligne • ADT Pro"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeChat() },
                        modifier = Modifier.testTag("btn_chat_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    // Live Location Sharing button (Google Maps API + Geolocation permissions)
                    if (currentChat != null && !currentChat.isGroup) {
                        IconButton(
                            onClick = { requestLiveLocationSharing() },
                            modifier = Modifier.testTag("btn_share_live_location_topbar")
                        ) {
                            Icon(
                                Icons.Default.ShareLocation,
                                contentDescription = "Partager la position en direct",
                                tint = AdtPrimary
                            )
                        }
                    }

                    // Meta AI Direct Shortcut
                    IconButton(
                        onClick = { viewModel.openMetaAiDirect() },
                        modifier = Modifier.testTag("btn_meta_ai_chat_topbar")
                    ) {
                        MetaAiGlowingRing(size = 24.dp)
                    }

                    // Video Call action
                    IconButton(
                        onClick = {
                            if (currentChat != null) {
                                if (currentChat.isGroup) {
                                    viewModel.startGroupConferenceCall(
                                        groupId = currentChat.id,
                                        groupTitle = currentChat.title,
                                        isVideo = true
                                    )
                                } else {
                                    viewModel.startCall(
                                        name = currentChat.title,
                                        phone = currentChat.creatorPhone.ifBlank { "+221 77 000 11 22" },
                                        avatar = currentChat.avatarUrl,
                                        isVideo = true
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("btn_video_call")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Appel vidéo", tint = AdtPrimary)
                    }

                    // Audio Call action
                    IconButton(
                        onClick = {
                            if (currentChat != null) {
                                if (currentChat.isGroup) {
                                    viewModel.startGroupConferenceCall(
                                        groupId = currentChat.id,
                                        groupTitle = currentChat.title,
                                        isVideo = false
                                    )
                                } else {
                                    viewModel.startCall(
                                        name = currentChat.title,
                                        phone = currentChat.creatorPhone.ifBlank { "+221 77 000 11 22" },
                                        avatar = currentChat.avatarUrl,
                                        isVideo = false
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("btn_audio_call")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Appel vocal", tint = AdtPrimary)
                    }

                    // More Menu
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Plus d'options", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            if (currentChat?.isGroup == true) {
                                DropdownMenuItem(
                                    text = { Text("Infos du groupe") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = AdtPrimary) },
                                    onClick = {
                                        showTopMenu = false
                                        viewModel.openGroupInfo()
                                    },
                                    modifier = Modifier.testTag("menu_item_group_info")
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Chiffrement de bout en bout") },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = AdtPrimary) },
                                onClick = {
                                    showTopMenu = false
                                    showSafetyNumberDialog = true
                                },
                                modifier = Modifier.testTag("menu_item_encryption_info")
                            )
                            DropdownMenuItem(
                                text = { Text("Suggestions IA (Gemini)") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AdtPrimary) },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.regenerateQuickReplies()
                                },
                                modifier = Modifier.testTag("menu_item_gemini_quick_replies")
                            )
                            DropdownMenuItem(
                                text = { Text("Partager position en direct") },
                                leadingIcon = { Icon(Icons.Default.ShareLocation, contentDescription = null, tint = AdtPrimary) },
                                onClick = {
                                    showTopMenu = false
                                    requestLiveLocationSharing()
                                },
                                modifier = Modifier.testTag("menu_item_live_location")
                            )
                            DropdownMenuItem(
                                text = { Text("Médias et liens") },
                                leadingIcon = { Icon(Icons.Default.PermMedia, contentDescription = null) },
                                onClick = { showTopMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Appareil photo & Vidéo") },
                                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AdtPrimary) },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.openCamera(fromChatId = activeChatId)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Fonds d'écran") },
                                leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null, tint = AdtPrimary) },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.openWallpaperPicker()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Messages éphémères")
                                        Text(
                                            text = if (currentEphemeralDuration == EphemeralMessageDuration.OFF) {
                                                "Désactivé"
                                            } else {
                                                "Actif (${currentEphemeralDuration.label})"
                                            },
                                            fontSize = 11.sp,
                                            color = if (currentEphemeralDuration == EphemeralMessageDuration.OFF) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                AdtPrimary
                                            }
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.HistoryToggleOff,
                                        contentDescription = null,
                                        tint = if (currentEphemeralDuration == EphemeralMessageDuration.OFF) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            AdtPrimary
                                        }
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    showEphemeralDurationDialog = true
                                },
                                modifier = Modifier.testTag("menu_item_ephemeral_messages")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(if (currentChat?.isLocked == true) "Déverrouiller la discussion" else "Verrouiller la discussion (Biométrie)")
                                },
                                leadingIcon = {
                                    Icon(
                                        if (currentChat?.isLocked == true) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (currentChat?.isLocked == true) AdtAccentGreen else AdtPrimary
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    val isCurrentlyLocked = currentChat?.isLocked == true
                                    activeChatId?.let { id ->
                                        viewModel.setChatLocked(id, !isCurrentlyLocked)
                                        Toast.makeText(
                                            context,
                                            if (isCurrentlyLocked) "Discussion déverrouillée" else "Discussion verrouillée et protégée par biométrie",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.testTag("menu_item_lock_chat")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        ChatWallpaperBackground(
            wallpaperKey = appSettings?.wallpaperTheme ?: "EMERALD",
            isDark = MaterialTheme.colorScheme.background == DarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Ongoing or incoming WebRTC Call banner
                if (activeCall != null) {
                    FloatingMiniCallBanner(
                        activeCall = activeCall!!,
                        onRestore = { viewModel.restoreCall() },
                        onToggleMute = { viewModel.toggleCallMute() },
                        onEndCall = { viewModel.endCall() },
                        onAccept = { viewModel.acceptIncomingCall() }
                    )
                }

                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // Security encryption bubble at top
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .widthIn(max = 340.dp)
                                    .clickable { showSafetyNumberDialog = true }
                                    .testTag("security_banner_clickable")
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(14.dp))
                                    Text(
                                        "Messages chiffrés de bout en bout (Protocole Signal). Toucher pour vérifier.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    items(messages) { msg ->
                        val isThisSpeaking = isTtsPlaying && currentTtsMsgId == msg.id
                        val isLong = viewModel.isLongTextMessage(msg.text, msg.mediaType)
                        val isMedia = msg.mediaType in listOf("PHOTO", "VIDEO", "AUDIO", "DOCUMENT", "DOC")
                        val isDownloaded = if (msg.isOutgoing || !isMedia) true else viewModel.isMediaDownloaded(msg.id, msg.isOutgoing, msg.mediaType)
                        val isDownloading = downloadingMessageIds.contains(msg.id)
                        val reason = if (!isDownloaded) {
                            mediaRules.getReasonHeldBack(
                                isBatterySaverActive = syncEnergyMode == SyncEnergyMode.BATTERY_SAVER,
                                scheduledWindowHint = viewModel.getScheduledSyncWindowHint()
                            )
                        } else ""

                        MessageBubble(
                            message = msg,
                            isGroup = currentChat?.isGroup == true,
                            onLongClick = { selectedMessageForMenu = msg },
                            onForward = { viewModel.startForwarding(msg) },
                            onReply = { replyingMessage = msg },
                            onStopLiveSharing = { viewModel.stopLiveLocationSharing(it) },
                            onReact = { emoji -> viewModel.reactToMessage(msg.id, emoji) },
                            onReactionClick = { selectedMessageForMenu = msg },
                            onOpenLiveTracking = { payload ->
                                val chat = currentChat
                                viewModel.openLiveTracking(
                                    messageId = msg.id,
                                    chatId = msg.chatId,
                                    contactName = if (msg.isOutgoing) (chat?.title ?: "Moi") else msg.senderName.ifBlank { chat?.title ?: "Contact" },
                                    contactPhone = if (msg.isOutgoing) "" else msg.senderPhone,
                                    contactAvatar = chat?.avatarUrl ?: "",
                                    payload = payload
                                )
                            },
                            isTtsPlaying = isTtsPlaying,
                            isCurrentTtsMessage = isThisSpeaking,
                            speechRate = ttsSpeechRate,
                            isLongMessage = isLong,
                            onSpeak = { viewModel.speakMessage(msg.id, msg.text) },
                            onStopTts = { viewModel.stopTts() },
                            onCycleSpeechRate = { viewModel.cycleTtsSpeechRate() },
                            onOpenDocument = { doc -> viewModel.openDocumentPreview(doc) },
                            onShowSecurityReport = { doc -> viewModel.openDocumentSecurityReport(doc) },
                            isMediaDownloaded = isDownloaded,
                            isMediaDownloading = isDownloading,
                            reasonHeldBack = reason,
                            onDownloadMedia = { viewModel.downloadMediaForMessage(msg.id) }
                        )
                    }
                }

                // Quoted reply banner if active
                if (replyingMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(32.dp)
                                        .background(AdtPrimary, RoundedCornerShape(2.dp))
                                )
                                Column {
                                    Text(
                                        text = replyingMessage?.senderName ?: "Message",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = AdtPrimary
                                    )
                                    Text(
                                        text = replyingMessage?.text ?: "",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { replyingMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Annuler réponse", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Custom Sticker Picker Sheet (Firebase Storage + Categories)
                if (showStickerPicker) {
                    CustomStickerPicker(
                        onStickerSelected = { emoji, title, imageUrl ->
                            viewModel.sendStickerMessage(emoji, title, imageUrl)
                            showStickerPicker = false
                        },
                        onClose = { showStickerPicker = false },
                        cloudStickers = cloudStickers,
                        syncStatus = stickerSyncStatus,
                        onSyncCloudStickers = { viewModel.syncCloudStickers() },
                        onCreateAndUploadSticker = { emoji, title, category, uri ->
                            viewModel.createAndUploadCustomSticker(
                                emoji = emoji,
                                title = title,
                                category = category,
                                pickedImageUri = uri,
                                sendImmediately = true
                            )
                            showStickerPicker = false
                        },
                        onDeleteCloudSticker = { viewModel.deleteCloudSticker(it) }
                    )
                }

                // Attachment Selection Panel
                if (showAttachmentMenu) {
                    AttachmentOptionsPanel(
                        onOpenLiveLocation = {
                            showAttachmentMenu = false
                            requestLiveLocationSharing()
                        },
                        onOpenCamera = {
                            showAttachmentMenu = false
                            viewModel.openCamera(fromChatId = activeChatId)
                        },
                        onOpenWallpapers = {
                            showAttachmentMenu = false
                            viewModel.openWallpaperPicker()
                        },
                        onSelectPhoto = { label ->
                            viewModel.sendPhotoMessage("Photo partagée", label)
                            showAttachmentMenu = false
                        },
                        onSelectVideo = { label ->
                            viewModel.sendVideoMessage("Vidéo pro", 45)
                            showAttachmentMenu = false
                        },
                        onSelectDoc = {
                            showAttachmentMenu = false
                            viewModel.openDocumentPicker()
                        },
                        onClose = { showAttachmentMenu = false }
                    )
                }

                // Text-to-Speech active player banner
                AnimatedVisibility(
                    visible = isTtsPlaying,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val activeMsg = messages.firstOrNull { it.id == currentTtsMsgId }
                    TtsActiveBanner(
                        currentMessageText = activeMsg?.text ?: "Synthèse vocale en cours...",
                        senderName = if (activeMsg?.isOutgoing == true) "Moi" else activeMsg?.senderName?.ifBlank { "Contact" } ?: "Message",
                        speechRate = ttsSpeechRate,
                        onCycleSpeechRate = { viewModel.cycleTtsSpeechRate() },
                        onStop = { viewModel.stopTts() }
                    )
                }

                // Bottom Input Bar with Firebase AI Contextual Quick Replies
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Firebase AI Contextual Quick Replies Row
                        AnimatedVisibility(
                            visible = quickReplies.isNotEmpty() || isGeneratingQuickReplies || quickRepliesUiState is QuickRepliesUiState.Error,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = AdtPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isGeneratingQuickReplies && quickReplies.isEmpty()) {
                                                "Firebase AI analyse le message..."
                                            } else if (isGeneratingQuickReplies) {
                                                "Régénération Firebase AI..."
                                            } else {
                                                "Réponses rapides suggérées par Firebase AI"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AdtPrimary
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Sélecteur de style contextuel (Amical / Pro / Court)
                                        val toneLabel = when (quickRepliesTone) {
                                            "PROFESSIONAL" -> "Pro"
                                            "SHORT" -> "Court"
                                            else -> "Amical"
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = AdtPrimary.copy(alpha = 0.12f),
                                            modifier = Modifier
                                                .clickable {
                                                    val nextTone = when (quickRepliesTone) {
                                                        "FRIENDLY" -> "PROFESSIONAL"
                                                        "PROFESSIONAL" -> "SHORT"
                                                        else -> "FRIENDLY"
                                                    }
                                                    viewModel.setQuickRepliesTone(nextTone)
                                                }
                                                .testTag("btn_toggle_quick_reply_tone")
                                        ) {
                                            Text(
                                                text = toneLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AdtPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.regenerateQuickReplies() },
                                            modifier = Modifier.size(24.dp).testTag("btn_refresh_quick_replies"),
                                            enabled = !isGeneratingQuickReplies
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Régénérer d'autres suggestions",
                                                tint = AdtPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.clearQuickReplies() },
                                            modifier = Modifier.size(24.dp).testTag("btn_dismiss_quick_replies")
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Ignorer suggestions",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                if (isGeneratingQuickReplies) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .padding(horizontal = 12.dp, vertical = 2.dp),
                                        color = AdtPrimary,
                                        trackColor = AdtPrimary.copy(alpha = 0.2f)
                                    )
                                }

                                val currentQrState = quickRepliesUiState
                                if (currentQrState is QuickRepliesUiState.Error && quickReplies.isEmpty() && !isGeneratingQuickReplies) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .testTag("quick_replies_error_banner"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = "Erreur suggestions IA",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = currentQrState.message,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (quickReplies.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        itemsIndexed(quickReplies.take(3)) { index, reply ->
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.45f)),
                                                shadowElevation = 1.dp,
                                                modifier = Modifier.testTag("btn_quick_reply_$index")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
                                                ) {
                                                    // Clic principal : envoi instantané
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier
                                                            .clickable {
                                                                viewModel.sendTextMessage(reply)
                                                            }
                                                            .padding(vertical = 3.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.AutoMirrored.Filled.Send,
                                                            contentDescription = "Envoyer directement",
                                                            tint = AdtPrimary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = reply,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(4.dp))

                                                    // Bouton modifier : insère dans le champ message sans envoyer
                                                    IconButton(
                                                        onClick = {
                                                            messageInput = if (messageInput.isBlank()) reply else "$messageInput $reply"
                                                        },
                                                        modifier = Modifier.size(24.dp).testTag("btn_edit_quick_reply_${reply.hashCode()}")
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            contentDescription = "Modifier avant envoi",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Live Voice-to-Text Transcription Status Banner
                        AnimatedVisibility(
                            visible = voiceToTextState.isListening || !voiceToTextState.errorMessage.isNullOrBlank(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            val isError = !voiceToTextState.isListening && !voiceToTextState.errorMessage.isNullOrBlank()
                            Surface(
                                color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f) else AdtPrimary.copy(alpha = 0.10f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_to_text_live_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = if (isError) MaterialTheme.colorScheme.error else AdtPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (isError) {
                                                    voiceToTextState.errorMessage ?: ""
                                                } else {
                                                    "Dictée vocale en direct ($speechLanguageTag)..."
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isError) MaterialTheme.colorScheme.error else AdtPrimary
                                            )
                                            if (voiceToTextState.isListening) {
                                                val levelBars = (voiceToTextState.rmsDb / 2f).toInt().coerceIn(1, 5)
                                                Text(
                                                    text = "Parlez maintenant • Signal ${"▮".repeat(levelBars)}${"▯".repeat(5 - levelBars)}",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (voiceToTextState.isListening) {
                                            TextButton(
                                                onClick = { voiceToTextManager.stopListening() },
                                                modifier = Modifier.testTag("btn_stop_voice_to_text")
                                            ) {
                                                Text("Terminer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { voiceToTextManager.clearError() },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                        if (isRecordingAudio) {
                            // Recording Vocal Active State
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FiberManualRecord,
                                        contentDescription = "Enregistrement",
                                        tint = CallRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Enregistrement vocal : 0:${String.format("%02d", recordDurationSeconds)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CallRed
                                    )
                                }

                                TextButton(onClick = { isRecordingAudio = false }) {
                                    Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }

                            // Send Voice Button
                            FloatingActionButton(
                                onClick = {
                                    viewModel.sendVoiceNote(recordDurationSeconds.coerceAtLeast(2))
                                    isRecordingAudio = false
                                },
                                containerColor = AdtPrimary,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer vocal")
                            }
                        } else {
                            // Normal Text Input State
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Emoji & Sticker button
                                    IconButton(onClick = { showStickerPicker = !showStickerPicker }) {
                                        Icon(
                                            if (showStickerPicker) Icons.Default.Keyboard else Icons.Default.SentimentSatisfiedAlt,
                                            contentDescription = "Autocollants & Emojis",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Input TextField
                                    TextField(
                                        value = messageInput,
                                        onValueChange = { messageInput = it },
                                        placeholder = { Text("Message...", fontSize = 15.sp) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("chat_input_field"),
                                        maxLines = 4
                                    )

                                    // Firebase AI Quick Replies trigger icon when chips are dismissed
                                    if (quickReplies.isEmpty() && !isGeneratingQuickReplies && messageInput.isBlank()) {
                                        IconButton(
                                            onClick = { viewModel.forceGenerateQuickRepliesForLastReceived() },
                                            modifier = Modifier.testTag("btn_trigger_ai_replies")
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = "Générer suggestions Firebase AI",
                                                tint = AdtPrimary
                                            )
                                        }
                                    }

                                    // Real-Time Voice-to-Text Speech Recognition Button
                                    IconButton(
                                        onClick = toggleVoiceToText,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("btn_voice_to_text")
                                    ) {
                                        Icon(
                                            imageVector = if (voiceToTextState.isListening) Icons.Default.StopCircle else Icons.Default.RecordVoiceOver,
                                            contentDescription = if (voiceToTextState.isListening) {
                                                "Arrêter la dictée vocale"
                                            } else {
                                                "Dicter un message (voix vers texte)"
                                            },
                                            tint = if (voiceToTextState.isListening) CallRed else AdtPrimary
                                        )
                                    }

                                    // Attachment button (Paperclip)
                                    IconButton(
                                        onClick = { showAttachmentMenu = !showAttachmentMenu },
                                        modifier = Modifier.testTag("btn_attachment")
                                    ) {
                                        Icon(
                                            Icons.Default.AttachFile,
                                            contentDescription = "Pièce jointe",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Camera shortcut
                                    IconButton(
                                        onClick = {
                                            viewModel.openCamera(fromChatId = activeChatId)
                                        },
                                        modifier = Modifier.testTag("btn_input_camera")
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = "Caméra Studio Effets",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Send or Mic FAB
                            if (messageInput.isNotBlank()) {
                                FloatingActionButton(
                                    onClick = {
                                        viewModel.sendTextMessage(messageInput, replyingMessage)
                                        messageInput = ""
                                        replyingMessage = null
                                    },
                                    containerColor = AdtPrimary,
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .testTag("btn_send_message")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer")
                                }
                            } else {
                                FloatingActionButton(
                                    onClick = {
                                        isRecordingAudio = true
                                    },
                                    containerColor = AdtPrimary,
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .testTag("btn_record_audio")
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Enregistrer vocal")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Forward Dialog if requested
    if (forwardingMessage != null) {
        ForwardMessageDialog(viewModel = viewModel, message = forwardingMessage!!)
    }

    // Share Live Location Dialog
    if (showShareLocation) {
        ShareLiveLocationDialog(
            allContacts = allContacts,
            preselectedContactPhoneOrChatId = locationShareFromChatId ?: activeChatId,
            onDismiss = { viewModel.closeShareLocationDialog() },
            onConfirmShare = { contacts, duration, comment, location ->
                viewModel.shareLiveLocation(contacts, duration, comment, location)
            }
        )
    }

    // Secure Document Picker Dialog (PDF, Office, Firebase Storage & Server-side Scan)
    if (showDocumentPicker) {
        DocumentPickerDialog(
            onDismiss = { viewModel.closeDocumentPicker() },
            onSendDocument = { doc, bytes, caption ->
                viewModel.sendSecureDocument(doc, bytes, caption)
            },
            isUploading = isDocUploading,
            uploadProgress = docUploadProgress,
            statusMessage = docStatusMsg
        )
    }

    // In-App Document Preview Dialog
    previewingDoc?.let { doc ->
        DocumentPreviewDialog(
            document = doc,
            onDismiss = { viewModel.closeDocumentPreview() },
            onShowSecurityReport = {
                viewModel.closeDocumentPreview()
                viewModel.openDocumentSecurityReport(doc)
            }
        )
    }

    // Cloud Security & VirusTotal Scan Report Dialog
    inspectingSecurityDoc?.let { doc ->
        DocumentSecurityReportDialog(
            document = doc,
            onDismiss = { viewModel.closeDocumentSecurityReport() },
            onOpenDocument = {
                viewModel.closeDocumentSecurityReport()
                viewModel.openDocumentPreview(doc)
            }
        )
    }

    // Message action bottom sheet
    if (selectedMessageForMenu != null) {
        val msg = selectedMessageForMenu!!
        ModalBottomSheet(onDismissRequest = { selectedMessageForMenu = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Reaction Bar
                Text(
                    text = "Réagir au message :",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                QuickReactionFloatingBar(
                    currentReaction = msg.reaction,
                    onSelectEmoji = { emoji ->
                        viewModel.reactToMessage(msg.id, emoji)
                        selectedMessageForMenu = null
                    },
                    onOpenExtendedPicker = {
                        messageForEmojiPicker = msg
                        selectedMessageForMenu = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (msg.reaction != null) {
                    ListItem(
                        headlineContent = { Text("Supprimer ma réaction (${msg.reaction})", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            viewModel.reactToMessage(msg.id, null)
                            selectedMessageForMenu = null
                        }
                    )
                }

                HorizontalDivider()

                // Actions: Info, Transfert, Répondre, Supprimer
                if (msg.isOutgoing) {
                    ListItem(
                        headlineContent = { Text("Infos du message", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Accusés d'envoi, de distribution et de lecture") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null, tint = AdtPrimary) },
                        modifier = Modifier.clickable {
                            messageForInfoDialog = msg
                            selectedMessageForMenu = null
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text("Transférer le message", fontWeight = FontWeight.SemiBold) },
                    leadingContent = { Icon(Icons.Default.Forward, contentDescription = null, tint = AdtPrimary) },
                    modifier = Modifier.clickable {
                        viewModel.startForwarding(msg)
                        selectedMessageForMenu = null
                    }
                )

                ListItem(
                    headlineContent = { Text("Répondre") },
                    leadingContent = { Icon(Icons.Default.Reply, contentDescription = null) },
                    modifier = Modifier.clickable {
                        replyingMessage = msg
                        selectedMessageForMenu = null
                    }
                )

                // Firebase AI contextual quick replies for received text messages
                if (!msg.isOutgoing && msg.mediaType == "TEXT") {
                    ListItem(
                        headlineContent = { Text("Réponses rapides Firebase AI", fontWeight = FontWeight.SemiBold, color = AdtPrimary) },
                        supportingContent = { Text("Générer 3 suggestions intelligentes pour ce message") },
                        leadingContent = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AdtPrimary) },
                        modifier = Modifier
                            .testTag("btn_menu_generate_ai_replies")
                            .clickable {
                                viewModel.generateQuickRepliesForSpecificMessage(msg)
                                selectedMessageForMenu = null
                            }
                    )
                }

                // Text-to-Speech (TTS) option
                if (msg.mediaType == "TEXT") {
                    val isThisSpeaking = isTtsPlaying && currentTtsMsgId == msg.id
                    ListItem(
                        headlineContent = {
                            Text(
                                if (isThisSpeaking) "Arrêter la synthèse vocale" else "Écouter le message (Synthèse vocale)",
                                fontWeight = FontWeight.SemiBold,
                                color = if (isThisSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(if (isThisSpeaking) "Lecture vocale en cours (${ttsSpeechRate}x) — Appuyer pour arrêter" else "Convertit le texte en lecture vocale à voix haute")
                        },
                        leadingContent = {
                            Icon(
                                if (isThisSpeaking) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
                                contentDescription = "Synthèse vocale",
                                tint = if (isThisSpeaking) MaterialTheme.colorScheme.error else AdtPrimary
                            )
                        },
                        trailingContent = {
                            if (isThisSpeaking) {
                                TextButton(onClick = { viewModel.cycleTtsSpeechRate() }) {
                                    Text("${ttsSpeechRate}x", fontWeight = FontWeight.Bold, color = AdtPrimary)
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            if (isThisSpeaking) {
                                viewModel.stopTts()
                            } else {
                                viewModel.speakMessage(msg.id, msg.text)
                            }
                            selectedMessageForMenu = null
                        }.testTag(if (isThisSpeaking) "menu_item_stop_tts" else "menu_item_play_tts")
                    )
                }

                ListItem(
                    headlineContent = { Text("Supprimer pour moi", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        viewModel.deleteMessage(msg.id)
                        selectedMessageForMenu = null
                    }
                )
            }
        }
    }

    // Extended Emoji Picker Sheet
    if (messageForEmojiPicker != null) {
        val msg = messageForEmojiPicker!!
        ExtendedEmojiPickerSheet(
            currentReaction = msg.reaction,
            onDismiss = { messageForEmojiPicker = null },
            onSelectEmoji = { emoji ->
                viewModel.reactToMessage(msg.id, emoji)
                messageForEmojiPicker = null
            }
        )
    }

    // Message Info Sheet
    if (messageForInfoDialog != null) {
        val msg = messageForInfoDialog!!
        val isGroup = currentChat?.isGroup ?: false
        val groupMembers by if (isGroup && activeChatId != null) {
            viewModel.getGroupMembers(activeChatId!!).collectAsState(initial = emptyList())
        } else {
            remember { mutableStateOf(emptyList()) }
        }

        MessageInfoSheet(
            message = msg,
            isGroup = isGroup,
            groupMembers = groupMembers,
            chatTitle = currentChat?.title ?: "Discussion",
            onDismiss = { messageForInfoDialog = null }
        )
    }

    // Signal Protocol Safety Number Verification Dialog
    if (showSafetyNumberDialog) {
        val otherPhone = currentChat?.creatorPhone?.ifBlank { "+221770001122" } ?: "+221770001122"
        val safetyNumber = viewModel.getSafetyNumber(activeChatId ?: "chat_default", otherPhone)
        val myPubKey = viewModel.getMyIdentityPublicKey()

        SignalSafetyNumberDialog(
            chatTitle = currentChat?.title ?: "Discussion",
            isGroup = currentChat?.isGroup ?: false,
            safetyNumber = safetyNumber,
            myPublicKey = myPubKey,
            onDismiss = { showSafetyNumberDialog = false },
            onViewAllKeys = { viewModel.openEncryptionKeysScreen() },
            onToggleVerified = {
                activeChatId?.let { viewModel.toggleFingerprintVerified(it) }
            }
        )
    }

    // Ephemeral Disappearing Messages Dialog
    if (showEphemeralDurationDialog && activeChatId != null) {
        val chatId = activeChatId!!
        EphemeralMessageTimerDialog(
            currentDuration = currentEphemeralDuration,
            onDismiss = { showEphemeralDurationDialog = false },
            onSelectDuration = { newDuration ->
                viewModel.setChatEphemeralDuration(chatId, newDuration)
                showEphemeralDurationDialog = false
            }
        )
    }
}
}

@Composable
fun SignalSafetyNumberDialog(
    chatTitle: String,
    isGroup: Boolean,
    safetyNumber: String,
    myPublicKey: String,
    onDismiss: () -> Unit,
    onViewAllKeys: (() -> Unit)? = null,
    onToggleVerified: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isVerified by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = AdtPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = AdtPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Numéro de sécurité",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isGroup) "Canal Groupe Chiffré" else "Protocole Signal E2EE",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Pour vérifier la sécurité du chiffrement de bout en bout avec $chatTitle, comparez les 60 chiffres ci-dessous avec le numéro figurant sur son appareil.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                // 60-digit safety number grid
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Display safety number in 4 rows of 3 blocks (12 blocks of 5 digits)
                        val blocks = safetyNumber.split(" ")
                        val rows = blocks.chunked(3)

                        rows.forEach { rowBlocks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowBlocks.forEach { block ->
                                    Text(
                                        text = block,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Copy button
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Safety Number", safetyNumber)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Numéro de sécurité copié", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_copy_safety_number")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copier les 60 chiffres", fontSize = 11.5.sp)
                        }
                    }
                }

                // Cryptographic Engine Specs
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AdtPrimary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Paramètres cryptographiques :",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdtPrimary
                        )
                        Text(
                            text = "• Algorithme : Double Ratchet + Sender Keys",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Chiffrement : AES-256-GCM (Auth Tag 128-bit)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Clés : ECDH secp256r1 + HKDF-SHA256",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Stockage Cloud : Ciphertext hermétique Firestore",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Verification toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isVerified = !isVerified }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = isVerified,
                        onCheckedChange = { isVerified = it },
                        modifier = Modifier.testTag("checkbox_verify_e2ee")
                    )
                    Text(
                        text = "J'ai vérifié ce numéro de sécurité avec ce contact.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isVerified) {
                        onToggleVerified?.invoke()
                        android.widget.Toast.makeText(context, "Chiffrement validé avec succès", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_confirm_safety_number")
            ) {
                Text(if (isVerified) "Valider la vérification" else "Fermer")
            }
        },
        dismissButton = {
            if (onViewAllKeys != null) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onViewAllKeys.invoke()
                    },
                    modifier = Modifier.testTag("btn_view_all_encryption_keys")
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Toutes les clés", fontSize = 12.sp)
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    isGroup: Boolean,
    onLongClick: () -> Unit,
    onForward: () -> Unit,
    onReply: () -> Unit,
    onStopLiveSharing: (String) -> Unit = {},
    onReact: (String) -> Unit = {},
    onReactionClick: () -> Unit = {},
    onOpenLiveTracking: ((com.example.data.model.LiveLocationPayload) -> Unit)? = null,
    isTtsPlaying: Boolean = false,
    isCurrentTtsMessage: Boolean = false,
    speechRate: Float = 1.0f,
    isLongMessage: Boolean = false,
    onSpeak: () -> Unit = {},
    onStopTts: () -> Unit = {},
    onCycleSpeechRate: () -> Unit = {},
    onOpenDocument: (SecureDocument) -> Unit = {},
    onShowSecurityReport: (SecureDocument) -> Unit = {},
    isMediaDownloaded: Boolean = true,
    isMediaDownloading: Boolean = false,
    reasonHeldBack: String = "",
    onDownloadMedia: () -> Unit = {}
) {
    // Handle System Info Messages
    if (message.senderPhone == "system") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        }
        return
    }

    var showHeartBurst by remember { mutableStateOf(false) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(750)
            showHeartBurst = false
        }
    }

    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isOutgoing) {
        if (MaterialTheme.colorScheme.background == DarkBg) DarkBubbleOutgoing else LightBubbleOutgoing
    } else {
        if (MaterialTheme.colorScheme.background == DarkBg) DarkBubbleIncoming else LightBubbleIncoming
    }
    val shape = if (isOutgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val timeString = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (message.reaction != null) 6.dp else 2.dp),
        contentAlignment = alignment
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                color = bubbleColor,
                shape = shape,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .combinedClickable(
                        onClick = { onLongClick() },
                        onLongClick = { onLongClick() },
                        onDoubleClick = {
                            showHeartBurst = true
                            onReact("❤️")
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Forwarded tag if any
                    if (message.isForwarded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Forward,
                                contentDescription = "Transféré",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (message.forwardedCount > 1) "Transféré plusieurs fois" else "Transféré",
                                fontStyle = FontStyle.Italic,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Group sender name
                    if (isGroup && !isOutgoing) {
                        Text(
                            text = message.senderName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AdtSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Quoted reply box if any
                    if (message.replyToText != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(modifier = Modifier.padding(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(24.dp)
                                        .background(AdtPrimary, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        message.replyToSender ?: "Message",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AdtPrimary
                                    )
                                    Text(
                                        message.replyToText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Content depending on MediaType
                    val isMedia = message.mediaType in listOf("PHOTO", "VIDEO", "AUDIO", "DOCUMENT", "DOC")
                    if (!isOutgoing && isMedia && !isMediaDownloaded) {
                        DeferredMediaDownloadPlaceholder(
                            message = message,
                            reason = reasonHeldBack,
                            isDownloading = isMediaDownloading,
                            onDownloadClick = onDownloadMedia
                        )
                    } else {
                        when (message.mediaType) {
                            "AUDIO" -> VoiceNoteBubbleContent(message)
                            "PHOTO" -> PhotoBubbleContent(message)
                            "VIDEO" -> VideoBubbleContent(message)
                            "STICKER" -> StickerBubbleContent(message)
                            "LIVE_LOCATION", "LOCATION" -> {
                                LiveLocationMapBubble(
                                    message = message,
                                    onStopLiveSharing = onStopLiveSharing,
                                    onOpenLiveTracking = onOpenLiveTracking
                                )
                            }
                            "DOCUMENT", "DOC" -> {
                                DocumentBubbleContent(
                                    message = message,
                                    onOpenDocument = onOpenDocument,
                                    onShowSecurityReport = onShowSecurityReport
                                )
                            }
                            else -> {
                                Column {
                                    Text(
                                        text = message.text,
                                        fontSize = 14.5.sp,
                                        color = if (isOutgoing && MaterialTheme.colorScheme.background != DarkBg) Color(0xFF111B21) else MaterialTheme.colorScheme.onSurface
                                    )

                                    // Long message Text-to-Speech (TTS) bar
                                    if (isLongMessage) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LongMessageTtsBar(
                                            text = message.text,
                                            isPlaying = isCurrentTtsMessage,
                                            speechRate = speechRate,
                                            onPlay = onSpeak,
                                            onStop = onStopTts,
                                            onCycleSpeed = onCycleSpeechRate,
                                            isOutgoing = isOutgoing
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Footer: Timestamp + Status checks
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = timeString,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isOutgoing) {
                            val icon = when (message.status) {
                                "SENT" -> Icons.Default.Done
                                else -> Icons.Default.DoneAll
                            }
                            val iconColor = when (message.status) {
                                "READ" -> Color(0xFF53BDEB) // Blue for read
                                else -> MaterialTheme.colorScheme.onSurfaceVariant // Grey for sent / delivered
                            }
                            val statusLabel = when (message.status) {
                                "SENT" -> "Envoyé"
                                "DELIVERED" -> "Distribué"
                                else -> "Lu"
                            }
                            Icon(
                                icon,
                                contentDescription = statusLabel,
                                tint = iconColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Heart pop burst animation on double tap
            DoubleTapHeartBurst(visible = showHeartBurst)
        }

        // WhatsApp-style floating reaction badge slightly overlapping the bubble corner
        if (message.reaction != null) {
            MessageReactionBadge(
                reaction = message.reaction,
                isOutgoing = isOutgoing,
                onClick = onReactionClick,
                modifier = Modifier
                    .align(if (isOutgoing) Alignment.BottomEnd else Alignment.BottomStart)
                    .offset(
                        x = if (isOutgoing) (-10).dp else 10.dp,
                        y = 9.dp
                    )
            )
        }
    }
}

@Composable
fun VoiceNoteBubbleContent(message: MessageEntity) {
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableStateOf(0.35f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = AdtPrimary,
            modifier = Modifier.size(38.dp)
        ) {
            IconButton(onClick = { isPlaying = !isPlaying }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Lecture vocale",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            // Simulated Waveform bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                val barHeights = listOf(0.4f, 0.7f, 1f, 0.6f, 0.8f, 0.3f, 0.9f, 0.5f, 0.75f, 0.4f, 0.9f, 0.6f, 0.3f, 0.8f, 0.5f, 0.7f)
                barHeights.forEachIndexed { index, h ->
                    val isPlayed = index.toFloat() / barHeights.size <= playbackProgress
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(h)
                            .background(
                                if (isPlayed) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0:${String.format("%02d", message.mediaDuration)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Message vocal ADT", fontSize = 10.sp, color = AdtPrimary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * Composant affiché lorsqu'un média entrant n'est pas encore téléchargé
 * en vertu des règles configurées (Wi-Fi uniquement, économie de batterie, roaming).
 */
@Composable
fun DeferredMediaDownloadPlaceholder(
    message: MessageEntity,
    reason: String,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit
) {
    val (mediaLabel, mediaIcon, estimatedSize) = when (message.mediaType.uppercase()) {
        "PHOTO", "IMAGE" -> Triple("Photo", Icons.Default.Image, "2.4 Mo")
        "VIDEO" -> Triple("Vidéo HD", Icons.Default.Videocam, "14.8 Mo")
        "AUDIO", "VOICE" -> Triple("Message Vocal", Icons.Default.Mic, "450 Ko")
        "DOCUMENT", "DOC", "PDF" -> Triple("Document", Icons.Default.Description, "1.8 Mo")
        else -> Triple("Média", Icons.Default.InsertDriveFile, "1.5 Mo")
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDownloading) { onDownloadClick() }
            .testTag("btn_manual_download_${message.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Media Type & Size Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(mediaIcon, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                    Text(mediaLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = estimatedSize,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Central Download Action Button
            Surface(
                shape = CircleShape,
                color = if (isDownloading) AdtPrimary.copy(alpha = 0.15f) else AdtPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(enabled = !isDownloading) { onDownloadClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AdtPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Télécharger le média",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Reason Badge (e.g. Battery saver or Wait for Wi-Fi)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (reason.contains("🔋")) Color(0xFF25D366).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                border = BorderStroke(0.8.dp, if (reason.contains("🔋")) Color(0xFF25D366).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = if (isDownloading) "Téléchargement en cours..." else reason.ifBlank { "Toucher pour télécharger" },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (reason.contains("🔋")) Color(0xFF1B8748) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
    }
}

@Composable
fun PhotoBubbleContent(message: MessageEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E2E38)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(48.dp))
                    Text(message.mediaUrl.ifBlank { "Photo transmise en HD" }, color = Color.White, fontSize = 12.sp)
                }
            }
        }
        if (message.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(message.text, fontSize = 14.sp)
        }
    }
}

@Composable
fun VideoBubbleContent(message: MessageEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF14242C)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Lire vidéo", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        "0:${message.mediaDuration}s • HD",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (message.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(message.text, fontSize = 14.sp)
        }
    }
}

@Composable
fun StickerBubbleContent(message: MessageEntity) {
    val isImageAsset = message.mediaUrl.startsWith("http") ||
        message.mediaUrl.startsWith("content://") ||
        message.mediaUrl.startsWith("file://")

    val stickerEmoji = if (isImageAsset) {
        "🏷️"
    } else {
        message.mediaUrl.ifBlank {
            message.text.take(2).trim()
        }.ifBlank { "🏷️" }
    }

    val stickerLabel = remember(message.text, stickerEmoji, isImageAsset) {
        if (isImageAsset) message.text.trim() else message.text.replace(stickerEmoji, "").trim()
    }

    Column(
        modifier = Modifier
            .widthIn(min = 120.dp, max = 168.dp)
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isImageAsset) {
                AsyncImage(
                    model = message.mediaUrl,
                    contentDescription = stickerLabel.ifBlank { "Autocollant ADT" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Text(
                    text = stickerEmoji,
                    fontSize = 62.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (stickerLabel.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = CardDefaults.outlinedCardBorder(enabled = true),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = stickerLabel,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AttachmentOptionsPanel(
    onOpenLiveLocation: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenWallpapers: () -> Unit,
    onSelectPhoto: (String) -> Unit,
    onSelectVideo: (String) -> Unit,
    onSelectDoc: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Partager un média ou une position :", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttachmentItem(icon = Icons.Default.ShareLocation, label = "Position Direct", color = Color(0xFF00A884)) {
                    onOpenLiveLocation()
                }
                AttachmentItem(icon = Icons.Default.CameraAlt, label = "Caméra Studio", color = Color(0xFFE91E63)) {
                    onOpenCamera()
                }
                AttachmentItem(icon = Icons.Default.Wallpaper, label = "Fond d'écran", color = Color(0xFF00897B)) {
                    onOpenWallpapers()
                }
                AttachmentItem(icon = Icons.Default.Image, label = "Photos", color = Color(0xFF833AB4)) {
                    onSelectPhoto("Photo du projet")
                }
                AttachmentItem(icon = Icons.Default.Videocam, label = "Vidéos", color = Color(0xFFE1306C)) {
                    onSelectVideo("Vidéo commerciale")
                }
                AttachmentItem(icon = Icons.Default.InsertDriveFile, label = "Document", color = Color(0xFF515BD4)) {
                    onSelectDoc()
                }
            }
        }
    }
}

@Composable
fun AttachmentItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInfoSheet(
    message: MessageEntity,
    isGroup: Boolean,
    groupMembers: List<com.example.data.local.entity.GroupMemberEntity>,
    chatTitle: String,
    onDismiss: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy à HH:mm", Locale.FRENCH) }
    val sentTime = remember(message.timestamp) { dateFormat.format(Date(message.timestamp)) }
    val deliveredTime = remember(message.timestamp) {
        val delivDate = Date(message.timestamp + 1200)
        dateFormat.format(delivDate)
    }
    val readTime = remember(message.timestamp) {
        val readDate = Date(message.timestamp + 3200)
        dateFormat.format(readDate)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Infos du message",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AdtPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isGroup) "Groupe" else "Direct",
                        color = AdtPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Message Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = when (message.mediaType) {
                            "AUDIO" -> Icons.Default.Mic
                            "PHOTO" -> Icons.Default.Image
                            "VIDEO" -> Icons.Default.Videocam
                            "STICKER" -> Icons.Default.EmojiEmotions
                            "DOCUMENT", "DOC" -> Icons.Default.InsertDriveFile
                            "LIVE_LOCATION", "LOCATION" -> Icons.Default.LocationOn
                            else -> Icons.Default.Chat
                        },
                        contentDescription = null,
                        tint = AdtPrimary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = message.text.ifBlank { "Média" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Envoyé vers $chatTitle",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // Status Timeline
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 1. Lu (Double Coche Bleue)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF53BDEB).copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Lu",
                                tint = if (message.status == "READ") Color(0xFF53BDEB) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Lu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (message.status == "READ") Color(0xFF53BDEB) else MaterialTheme.colorScheme.onSurface
                            )
                            if (message.status == "READ") {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF53BDEB).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "Double coche bleue",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF53BDEB),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (message.status == "READ") {
                                if (isGroup) {
                                    "Lu par tous les participants • $readTime"
                                } else {
                                    "Lu par le destinataire • $readTime"
                                }
                            } else {
                                "En attente de lecture"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 2. Distribué (Double Coche Grise)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Distribué",
                                tint = if (message.status != "SENT") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Distribué",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (message.status != "SENT") "Reçu sur l'appareil • $deliveredTime" else "En cours de distribution...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 3. Envoyé (Simple Coche)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = "Envoyé",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Envoyé",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Transmis au serveur Firestore Cloud • $sentTime",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Group readers breakdown if group chat
            if (isGroup && groupMembers.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Participants du groupe (${groupMembers.size}) :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                groupMembers.take(4).forEach { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdtAvatar(name = member.name, sizeDp = 28)
                            Text(
                                text = member.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = if (message.status == "READ") Color(0xFF53BDEB) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (message.status == "READ") "Lu" else "Distribué",
                                fontSize = 11.5.sp,
                                color = if (message.status == "READ") Color(0xFF53BDEB) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Security footer
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AdtPrimary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = AdtPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Accusés vérifiés avec chiffrement de bout en bout et synchronisation Firestore en temps réel.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Barre de contrôle Text-to-Speech intégrée aux messages longs dans la bulle de discussion.
 */
@Composable
fun LongMessageTtsBar(
    text: String,
    isPlaying: Boolean,
    speechRate: Float,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onCycleSpeed: () -> Unit,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier
) {
    val estimatedSeconds = remember(text, speechRate) {
        val words = text.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size
        val wordsPerSec = 2.4f * speechRate
        (words / wordsPerSec).toInt().coerceAtLeast(1)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isPlaying) {
            AdtPrimary.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        border = BorderStroke(
            1.dp,
            if (isPlaying) AdtPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (isPlaying) "tts_bar_playing" else "tts_bar_idle")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Play/Stop button + label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Bouton 'Lire' / 'Arrêter' cliquable
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isPlaying) MaterialTheme.colorScheme.error else AdtPrimary,
                    modifier = Modifier
                        .clickable { if (isPlaying) onStop() else onPlay() }
                        .testTag(if (isPlaying) "btn_stop_tts_bubble" else "btn_play_tts_bubble")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = if (isPlaying) "Arrêter" else "Lire",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isPlaying) "Arrêter" else "Lire",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier.clickable { if (isPlaying) onStop() else onPlay() }
                ) {
                    Text(
                        text = if (isPlaying) "Lecture vocale en cours..." else "Synthèse vocale (TTS)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPlaying) AdtPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPlaying) "Appuyer pour arrêter" else "Convertir en audio (~${estimatedSeconds}s)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Speed badge (if playing) or Waveform icon
            if (isPlaying) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AdtPrimary.copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, AdtPrimary),
                    modifier = Modifier
                        .clickable { onCycleSpeed() }
                        .testTag("btn_tts_cycle_speed")
                ) {
                    Text(
                        text = "${speechRate}x",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdtPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = AdtPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Bannière flottante de lecture audio Text-to-Speech active au-dessus de la zone de saisie.
 */
@Composable
fun TtsActiveBanner(
    currentMessageText: String,
    senderName: String,
    speechRate: Float,
    onCycleSpeechRate: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("banner_tts_active")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = AdtPrimary,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Synthèse vocale active",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Lecture vocale ($senderName)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AdtPrimary.copy(alpha = 0.25f),
                            modifier = Modifier.clickable { onCycleSpeechRate() }
                        ) {
                            Text(
                                text = "${speechRate}x",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdtPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = currentMessageText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_stop_tts_banner")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Arrêter la synthèse vocale",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Boîte de dialogue pour sélectionner la durée d'autodestruction des messages éphémères.
 * Propose 24h, 7 jours, 90 jours et Désactivé.
 */
@Composable
fun EphemeralMessageTimerDialog(
    currentDuration: EphemeralMessageDuration,
    onDismiss: () -> Unit,
    onSelectDuration: (EphemeralMessageDuration) -> Unit
) {
    var selectedDuration by remember { mutableStateOf(currentDuration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = AdtPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.HistoryToggleOff,
                        contentDescription = null,
                        tint = AdtPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Messages éphémères",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pour plus de confidentialité, les nouveaux messages de cette discussion disparaîtront automatiquement pour tous les participants après le délai sélectionné.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Options list (24H, 7D, 90D, OFF)
                EphemeralMessageDuration.values().forEach { option ->
                    val isSelected = selectedDuration == option
                    val optionTag = when (option) {
                        EphemeralMessageDuration.HOURS_24 -> "radio_ephemeral_24h"
                        EphemeralMessageDuration.DAYS_7 -> "radio_ephemeral_7d"
                        EphemeralMessageDuration.DAYS_90 -> "radio_ephemeral_90d"
                        EphemeralMessageDuration.OFF -> "radio_ephemeral_off"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AdtPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDuration = option }
                            .testTag(optionTag)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedDuration = option },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AdtPrimary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.5.sp,
                                    color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectDuration(selectedDuration) },
                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_apply_ephemeral_duration")
            ) {
                Text("Appliquer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_ephemeral_dialog")
            ) {
                Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}


