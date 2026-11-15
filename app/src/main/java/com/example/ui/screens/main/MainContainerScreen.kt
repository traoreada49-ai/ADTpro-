package com.example.ui.screens.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.live.*
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.StatusEntity
import com.example.ui.components.AdtAvatar
import com.example.ui.components.ShareLiveLocationDialog
import com.example.ui.screens.live.StartLiveBroadcastDialog
import com.example.ui.theme.*
import com.example.ui.util.AppLanguage
import com.example.ui.util.TranslationStrings
import com.example.ui.util.getStringsForLanguage
import com.example.viewmodel.ActiveCallState
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen
import com.example.ui.screens.metaai.MetaAiGlowingRing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(viewModel: AdtViewModel) {
    val activeChats by viewModel.activeChats.collectAsState()
    val allStatuses by viewModel.allStatuses.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val allCalls by viewModel.allCalls.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val matchingChatIdsFromMessages by viewModel.matchingChatIdsFromMessages.collectAsState()
    val matchingMessages by viewModel.matchingMessages.collectAsState()
    val showShareLocationDialog by viewModel.showShareLocationDialog.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()

    val strings = remember(appSettings?.language) {
        getStringsForLanguage(appSettings?.language)
    }
    val currentLang = remember(appSettings?.language) {
        AppLanguage.fromCode(appSettings?.language)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showQuickLangDialog by remember { mutableStateOf(false) }
    var showStartLiveDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }

    val availableLiveStreams by viewModel.availableLiveStreams.collectAsState()
    val creatorAuth by viewModel.liveCreatorAuthorization.collectAsState()

    val tabTitles = listOf(
        strings.tabDiscussions,
        strings.tabStatuses,
        "Direct 🔴",
        strings.tabChannels,
        strings.tabCalls
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(strings.searchPlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                strings.appTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = AdtPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AdtPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    strings.appBadge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdtPrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // TikTok Live Quick Action
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFE2C55).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFFE2C55).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clickable { selectedTabIndex = 2 }
                            .testTag("btn_top_live_direct")
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(Color(0xFFFE2C55), CircleShape)
                            )
                            Text("LIVE", color = Color(0xFFFE2C55), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // Camera Studio Quick Action
                    IconButton(
                        onClick = { viewModel.openCamera() },
                        modifier = Modifier.testTag("btn_top_camera_studio")
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Appareil Photo & Vidéo",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Quick Search
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Rechercher",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Language Quick Switcher button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable { showQuickLangDialog = true }
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(currentLang.flag, fontSize = 16.sp)
                            Text(currentLang.code, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Meta AI Direct Action Icon (WhatsApp iridescent ring)
                    IconButton(
                        onClick = { viewModel.openMetaAiDirect() },
                        modifier = Modifier.testTag("btn_meta_ai_topbar")
                    ) {
                        MetaAiGlowingRing(size = 28.dp)
                    }

                    // Theme Toggle (Sombre / Clair / Auto selon horaire)
                    IconButton(
                        onClick = {
                            viewModel.toggleThemeModeCycle()
                        },
                        modifier = Modifier.testTag("btn_theme_toggle")
                    ) {
                        val currentMode = appSettings?.themeMode ?: "DARK"
                        val (icon, desc) = when (currentMode) {
                            "DARK" -> Icons.Default.LightMode to "Mode sombre actif — Toucher pour mode clair"
                            "LIGHT" -> Icons.Default.Schedule to "Mode clair actif — Toucher pour mode auto programmation"
                            "AUTO_SCHEDULE" -> Icons.Default.DarkMode to "Mode auto actif (horaire) — Toucher pour mode sombre"
                            else -> Icons.Default.DarkMode to "Basculer thème"
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = desc,
                            tint = if (currentMode == "AUTO_SCHEDULE") AdtPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Overflow Menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Plus d'options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("✨ Meta AI Direct") },
                                onClick = {
                                    showMenu = false
                                    viewModel.openMetaAiDirect()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF0084FF)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🔴 Direct Live comme TikTok") },
                                onClick = {
                                    showMenu = false
                                    selectedTabIndex = 2
                                },
                                leadingIcon = { Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color(0xFFFE2C55)) }
                            )
                            DropdownMenuItem(
                                text = { Text("🎥 Lancer mon direct") },
                                onClick = {
                                    showMenu = false
                                    showStartLiveDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFFFE2C55)) }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.menuNewGroup) },
                                onClick = {
                                    showMenu = false
                                    viewModel.navigateTo(AppScreen.CREATE_GROUP)
                                },
                                leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.menuNewChannel) },
                                onClick = {
                                    showMenu = false
                                    viewModel.navigateTo(AppScreen.CREATE_CHANNEL)
                                },
                                leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Localisation en direct") },
                                onClick = {
                                    showMenu = false
                                    viewModel.openShareLocationDialog()
                                },
                                leadingIcon = { Icon(Icons.Default.ShareLocation, contentDescription = null, tint = AdtPrimary) }
                            )
                            DropdownMenuItem(
                                text = { Text("Appareil Photo & Vidéo") },
                                onClick = {
                                    showMenu = false
                                    viewModel.openCamera()
                                },
                                leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Fonds d'écran") },
                                onClick = {
                                    showMenu = false
                                    viewModel.openWallpaperPicker()
                                },
                                leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.menuLanguage) },
                                onClick = {
                                    showMenu = false
                                    showQuickLangDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Code QR & Réseau Pro") },
                                onClick = {
                                    showMenu = false
                                    viewModel.openContactQrCode()
                                },
                                leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null, tint = AdtPrimary) }
                            )
                            DropdownMenuItem(
                                text = { Text("Inviter des amis & Contacts") },
                                onClick = {
                                    showMenu = false
                                    viewModel.openSyncContacts()
                                },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = AdtPrimary) }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.menuSettings) },
                                onClick = {
                                    showMenu = false
                                    viewModel.navigateTo(AppScreen.SETTINGS)
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            when (selectedTabIndex) {
                0 -> { // Chats
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Meta AI Direct Floating Action Button (WhatsApp signature iridescent ring)
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00DFD8), Color(0xFF0084FF), Color(0xFF7928CA), Color(0xFFFF0080))
                                    ),
                                    CircleShape
                                )
                                .clickable { viewModel.openMetaAiDirect() }
                                .testTag("fab_meta_ai_direct")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "Meta AI Direct",
                                            tint = Color(0xFF0084FF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        SmallFloatingActionButton(
                            onClick = { viewModel.openContactQrCode() },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = AdtPrimary,
                            modifier = Modifier.testTag("fab_qr_code")
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = "Mon QR Code & Scanner")
                        }
                        SmallFloatingActionButton(
                            onClick = { viewModel.navigateTo(AppScreen.CREATE_GROUP) },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = AdtPrimary,
                            modifier = Modifier.testTag("fab_new_group")
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Nouveau groupe")
                        }
                        FloatingActionButton(
                            onClick = { viewModel.navigateTo(AppScreen.CONTACTS_LIST) },
                            containerColor = AdtPrimary,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("fab_new_chat")
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "Nouvelle discussion")
                        }
                    }
                }
                1 -> { // Status
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = { viewModel.navigateTo(AppScreen.CREATE_STATUS) },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Écrire un statut")
                        }
                        FloatingActionButton(
                            onClick = { viewModel.openCamera(forStatus = true) },
                            containerColor = AdtPrimary,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("fab_new_status")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Photo/Vidéo statut")
                        }
                    }
                }
                2 -> { // Direct 🔴 (TikTok Live)
                    FloatingActionButton(
                        onClick = { showStartLiveDialog = true },
                        containerColor = Color(0xFFFE2C55),
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_go_live")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Lancer un Direct")
                    }
                }
                3 -> { // Channels
                    FloatingActionButton(
                        onClick = { viewModel.navigateTo(AppScreen.CREATE_CHANNEL) },
                        containerColor = AdtPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_new_channel")
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = strings.createChannel)
                    }
                }
                4 -> { // Calls
                    FloatingActionButton(
                        onClick = { viewModel.navigateTo(AppScreen.CONTACTS_LIST) },
                        containerColor = AdtPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_new_call")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = strings.newCall)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Floating Mini Call Banner if a WebRTC call is ongoing or incoming
            if (activeCall != null) {
                FloatingMiniCallBanner(
                    activeCall = activeCall!!,
                    onRestore = { viewModel.restoreCall() },
                    onToggleMute = { viewModel.toggleCallMute() },
                    onEndCall = { viewModel.endCall() },
                    onAccept = { viewModel.acceptIncomingCall() }
                )
            }

            // Tab Row
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = AdtPrimary,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = if (selectedTabIndex == 2) Color(0xFFFE2C55) else AdtPrimary
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (index == 2 && selectedTabIndex == index) Color(0xFFFE2C55) else Color.Unspecified
                                )
                                if (index == 0) {
                                    val totalUnread = activeChats.sumOf { it.unreadCount }
                                    if (totalUnread > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = AdtPrimary,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("$totalUnread", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else if (index == 2) {
                                    if (availableLiveStreams.isNotEmpty()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFFE2C55),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${availableLiveStreams.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> ChatsTabContent(
                    chats = activeChats.filter {
                        if (searchQuery.isNotBlank()) {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.lastMessage.contains(searchQuery, ignoreCase = true) ||
                            it.id in matchingChatIdsFromMessages
                        } else when (selectedFilterIndex) {
                            1 -> it.unreadCount > 0
                            2 -> it.isGroup
                            else -> true
                        }
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        viewModel.updateChatSearchQuery(query)
                    },
                    matchingMessages = matchingMessages,
                    selectedFilterIndex = selectedFilterIndex,
                    onSelectFilterIndex = { selectedFilterIndex = it },
                    strings = strings,
                    onChatClick = { viewModel.openChat(it.id) },
                    onOpenLiveLocationShare = { viewModel.openShareLocationDialog() }
                )
                1 -> StatusTabContent(
                    statuses = allStatuses,
                    liveStreams = availableLiveStreams,
                    strings = strings,
                    onStatusClick = { viewModel.openStatus(it) },
                    onCreateStatus = { viewModel.navigateTo(AppScreen.CREATE_STATUS) },
                    onWatchLiveStream = { viewModel.watchLiveStream(it) }
                )
                2 -> LiveDirectTabContent(
                    liveStreams = availableLiveStreams,
                    onWatchStream = { viewModel.watchLiveStream(it) },
                    onStartLiveClick = { showStartLiveDialog = true }
                )
                3 -> ChannelsTabContent(
                    channels = allChannels.filter {
                        if (searchQuery.isNotBlank()) it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
                        else true
                    },
                    strings = strings,
                    onChannelClick = { viewModel.openChannel(it.id) },
                    onCreateChannel = { viewModel.navigateTo(AppScreen.CREATE_CHANNEL) }
                )
                4 -> CallsTabContent(
                    calls = allCalls,
                    strings = strings,
                    onStartCall = { name, phone, avatar, isVideo ->
                        viewModel.startCall(name, phone, avatar, isVideo)
                    },
                    onSimulateIncomingCall = { isVideo ->
                        viewModel.simulateIncomingCall(isVideo)
                    }
                )
            }
        }
    }

    // Quick Language Dialog
    if (showQuickLangDialog) {
        AlertDialog(
            onDismissRequest = { showQuickLangDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = AdtPrimary)
                    Text(strings.selectLanguageTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = currentLang == lang
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) AdtPrimary.copy(alpha = 0.15f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLanguage(lang.code)
                                    showQuickLangDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(lang.flag, fontSize = 24.sp)
                                    Column {
                                        Text(
                                            "${lang.displayName} (${lang.countryName})",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            lang.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AdtPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickLangDialog = false }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = AdtPrimary)
                }
            }
        )
    }

    // Share Live Location Dialog
    if (showShareLocationDialog) {
        ShareLiveLocationDialog(
            allContacts = allContacts,
            preselectedContactPhoneOrChatId = null,
            onDismiss = { viewModel.closeShareLocationDialog() },
            onConfirmShare = { contacts, duration, comment, location ->
                viewModel.shareLiveLocation(contacts, duration, comment, location)
            }
        )
    }

    // Start TikTok Live Broadcast Dialog
    if (showStartLiveDialog) {
        StartLiveBroadcastDialog(
            creatorAuth = creatorAuth,
            onStartLive = { title, category ->
                viewModel.startHostingLive(title, category)
                showStartLiveDialog = false
            },
            onDismiss = { showStartLiveDialog = false }
        )
    }
}

@Composable
fun ChatsTabContent(
    chats: List<ChatEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    matchingMessages: List<MessageEntity> = emptyList(),
    selectedFilterIndex: Int,
    onSelectFilterIndex: (Int) -> Unit,
    strings: TranslationStrings,
    onChatClick: (ChatEntity) -> Unit,
    onOpenLiveLocationShare: () -> Unit = {}
) {
    val filters = listOf(strings.filterAll, strings.filterUnread, strings.filterGroups)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Prominent Main Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(strings.searchPlaceholder, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = AdtPrimary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdtPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("search_bar_chats")
            )
        }

        if (searchQuery.isNotBlank()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${chats.size} discussion(s) trouvée(s)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AdtPrimary
                    )
                    Text(
                        text = "Effacer",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onSearchQueryChange("") }
                            .padding(4.dp)
                    )
                }
            }
        }

        // Meta AI Direct Quick Launcher Banner
        if (searchQuery.isBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(Color(0xFF00DFD8).copy(alpha = 0.45f), Color(0xFF0084FF).copy(alpha = 0.45f), Color(0xFF7928CA).copy(alpha = 0.45f))
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 3.dp)
                        .clickable { onChatClick(ChatEntity(id = "chat_meta_ai", title = "✨ Meta AI", isGroup = false, avatarUrl = "meta_ai", lastMessage = "", lastTimestamp = 0L)) }
                        .testTag("banner_meta_ai_direct")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetaAiGlowingRing(size = 30.dp)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Meta AI Direct", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF0084FF), modifier = Modifier.size(14.dp))
                                }
                                Text("Posez une question ou tapez /imagine 🎨", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Ouvrir Meta AI",
                            tint = Color(0xFF0084FF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Filter Chips Row
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters.size) { idx ->
                    FilterChip(
                        selected = selectedFilterIndex == idx,
                        onClick = { onSelectFilterIndex(idx) },
                        label = { Text(filters[idx], fontSize = 12.sp) }
                    )
                }
            }
        }

        if (chats.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        strings.noChatsFound,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        }

        items(chats) { chat ->
            val timeFormatted = remember(chat.lastTimestamp) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.lastTimestamp))
            }
            val matchingMsg = remember(chat.id, matchingMessages, searchQuery) {
                if (searchQuery.isNotBlank() && !chat.title.contains(searchQuery, ignoreCase = true)) {
                    matchingMessages.find { it.chatId == chat.id }
                } else null
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChatClick(chat) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("chat_item_${chat.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (chat.id == "chat_meta_ai" || chat.avatarUrl == "meta_ai") {
                        MetaAiGlowingRing(size = 50.dp)
                    } else {
                        AdtAvatar(name = chat.title, sizeDp = 50, isOnline = !chat.isGroup)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = chat.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (chat.id == "chat_meta_ai" || chat.avatarUrl == "meta_ai") {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Vérifié",
                                        tint = Color(0xFF0084FF),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = timeFormatted,
                                fontSize = 11.sp,
                                color = if (chat.unreadCount > 0) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chat.lastMessage,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (chat.isPinned) {
                                Icon(Icons.Default.PushPin, contentDescription = "Épinglé", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                            if (chat.unreadCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = AdtPrimary,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${chat.unreadCount}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        if (matchingMsg != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AdtPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = AdtPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Message : \"${matchingMsg.text}\"",
                                        fontSize = 11.5.sp,
                                        color = AdtPrimary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(start = 76.dp))
        }
    }
}

@Composable
fun StatusTabContent(
    statuses: List<StatusEntity>,
    liveStreams: List<LiveStreamItem> = emptyList(),
    strings: TranslationStrings,
    onStatusClick: (StatusEntity) -> Unit,
    onCreateStatus: () -> Unit,
    onWatchLiveStream: (String) -> Unit = {}
) {
    val currentTime = remember { System.currentTimeMillis() }
    val twentyFourHoursAgo = remember(currentTime) { currentTime - (24L * 60L * 60L * 1000L) }
    val activeStatuses = remember(statuses) {
        statuses.filter { it.timestamp >= twentyFourHoursAgo }
    }
    val myStatus = activeStatuses.find { it.isSelf }
    val contactStatuses = activeStatuses.filter { !it.isSelf }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // My Status Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (myStatus != null) {
                            onStatusClick(myStatus)
                        } else {
                            onCreateStatus()
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AdtAvatar(name = strings.myStatus, sizeDp = 52, hasStatusStory = myStatus != null)
                    Surface(
                        shape = CircleShape,
                        color = AdtPrimary,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (myStatus != null) Icons.Default.Visibility else Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.myStatus, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (myStatus != null) {
                        val hoursLeft = ((myStatus.timestamp + 24L * 3600L * 1000L - System.currentTimeMillis()) / (3600L * 1000L)).coerceAtLeast(0)
                        Text(
                            "Actif • Expire dans ${hoursLeft}h • ${myStatus.viewsCount} vues",
                            fontSize = 12.sp,
                            color = AdtPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(strings.tapToAddStatus, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (myStatus != null) {
                    IconButton(onClick = onCreateStatus) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Ajouter un autre statut", tint = AdtPrimary)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }

        // Live Broadcasters Carousel (TikTok Style)
        if (liveStreams.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFE2C55), CircleShape))
                            Text(
                                "En Direct Maintenant 🔴",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFFE2C55)
                            )
                        }
                        Text(
                            "${liveStreams.size} en cours",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(liveStreams) { live ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clickable { onWatchLiveStream(live.id) }
                                    .testTag("status_live_${live.id}")
                            ) {
                                Box(contentAlignment = Alignment.BottomCenter) {
                                    Surface(
                                        modifier = Modifier.size(62.dp),
                                        shape = CircleShape,
                                        color = Color.Transparent,
                                        border = BorderStroke(2.5.dp, Brush.sweepGradient(listOf(Color(0xFFFE2C55), Color(0xFF25F4EE), Color(0xFFFE2C55))))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            AdtAvatar(name = live.hostName, sizeDp = 52)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFE2C55),
                                        modifier = Modifier.offset(y = 4.dp)
                                    ) {
                                        Text(
                                            "LIVE",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    live.hostName,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 68.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "👁️ ${live.viewerCount}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strings.recentUpdates,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = AdtPrimary
                )
                Text(
                    "Éphémères (24h)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (contactStatuses.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        "Aucune mise à jour récente",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Les statuts publiés par vos contacts disparaissent au bout de 24h",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(contactStatuses) { status ->
                val timeFormatted = remember(status.timestamp) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(status.timestamp))
                }
                val hoursRemaining = remember(status.timestamp) {
                    val expire = status.timestamp + 24L * 3600L * 1000L
                    ((expire - System.currentTimeMillis()) / (3600L * 1000L)).coerceAtLeast(0)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStatusClick(status) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("status_item_${status.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AdtAvatar(name = status.authorName, sizeDp = 50, hasStatusStory = true)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(status.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "$timeFormatted • Reste ${hoursRemaining}h",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!status.mediaUrl.isNullOrBlank()) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Photo",
                            tint = AdtPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelsTabContent(
    channels: List<ChannelEntity>,
    strings: TranslationStrings,
    onChannelClick: (ChannelEntity) -> Unit,
    onCreateChannel: () -> Unit
) {
    var selectedCategoryFilter by remember { mutableStateOf("Toutes") }
    val categories = listOf("Toutes", "Actualités", "Télécoms", "Emploi", "Business")

    val adminChannels = channels.filter { it.isOwner || it.adminName.contains("Créateur") }
    val otherChannels = channels.filter { !it.isOwner && !it.adminName.contains("Créateur") }

    val filteredChannels = channels.filter { chan ->
        if (selectedCategoryFilter == "Toutes") true
        else chan.category.contains(selectedCategoryFilter, ignoreCase = true) || chan.name.contains(selectedCategoryFilter, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Broadcast Intro Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AdtPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(32.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chaînes de diffusion unidirectionnelle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "Recevez des actualités vérifiées ou créez votre propre chaîne pour diffuser auprès de milliers d'abonnés.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Header & Create Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Explorer les chaînes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Button(
                    onClick = onCreateChannel,
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Créer une chaîne", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Filter chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Admin-owned channels section (if any)
        if (adminChannels.isNotEmpty() && selectedCategoryFilter == "Toutes") {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                    Text("Vos chaînes d'administration", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AdtPrimary)
                }
            }
            items(adminChannels) { channel ->
                ChannelItemRow(channel = channel, strings = strings, onChannelClick = onChannelClick)
            }
            item {
                Text("Toutes les chaînes certifiées", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            items(otherChannels) { channel ->
                ChannelItemRow(channel = channel, strings = strings, onChannelClick = onChannelClick)
            }
        } else {
            items(filteredChannels) { channel ->
                ChannelItemRow(channel = channel, strings = strings, onChannelClick = onChannelClick)
            }
        }
    }
}

@Composable
private fun ChannelItemRow(
    channel: ChannelEntity,
    strings: TranslationStrings,
    onChannelClick: (ChannelEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (channel.isOwner) AdtPrimary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (channel.isOwner) androidx.compose.foundation.BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.3f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChannelClick(channel) }
            .testTag("channel_item_${channel.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AdtAvatar(name = channel.name, sizeDp = 42)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (channel.isVerified) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(15.dp))
                            }
                        }
                        Text(
                            "${channel.followersCount} ${strings.subscribers} • ${channel.category}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (channel.isOwner) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AdtPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text("👑 Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdtPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                } else {
                    FilledTonalButton(
                        onClick = { onChannelClick(channel) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (channel.isFollowing) MaterialTheme.colorScheme.surface else AdtPrimary,
                            contentColor = if (channel.isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (channel.isFollowing) strings.following else strings.followChannel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = channel.latestPostText.ifBlank { channel.description },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CallsTabContent(
    calls: List<CallLogEntity>,
    strings: TranslationStrings,
    onStartCall: (name: String, phone: String, avatar: String, isVideo: Boolean) -> Unit,
    onSimulateIncomingCall: ((isVideo: Boolean) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // E2EE Call Security Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Chiffrement de bout en bout WebRTC",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Tous les appels audio et vidéo transitent en direct pair à pair, protégés par DTLS 1.3 et SRTP-AES-256-GCM. Aucun tiers ne peut écouter vos échanges.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 15.sp
                    )

                    // Quick Test incoming call buttons
                    if (onSimulateIncomingCall != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSimulateIncomingCall(false) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f).testTag("btn_test_audio_call")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = CallGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test audio entrant", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onSimulateIncomingCall(true) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f).testTag("btn_test_video_call")
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test vidéo entrant", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Historique des appels récents",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = AdtPrimary,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp)
            )
        }

        if (calls.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PhoneCallback,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "Aucun appel récent. Démarrez un appel sécurisé avec vos contacts !",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        items(calls) { call ->
            val timeFormatted = remember(call.timestamp) {
                SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(call.timestamp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AdtAvatar(name = call.contactName, sizeDp = 46)
                    Column {
                        Text(
                            text = call.contactName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (call.isMissed) CallRed else MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (call.isOutgoing) Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived,
                                contentDescription = null,
                                tint = if (call.isMissed) CallRed else CallGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                "$timeFormatted • ${if (call.isVideo) strings.videoCall else strings.voiceCall} • E2EE",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            onStartCall(call.contactName, call.contactPhone, call.contactAvatar, false)
                        }
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = strings.voiceCall,
                            tint = CallGreen
                        )
                    }
                    IconButton(
                        onClick = {
                            onStartCall(call.contactName, call.contactPhone, call.contactAvatar, true)
                        }
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = strings.videoCall,
                            tint = AdtPrimary
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(start = 76.dp))
        }
    }
}

@Composable
fun FloatingMiniCallBanner(
    activeCall: ActiveCallState,
    onRestore: () -> Unit,
    onToggleMute: () -> Unit,
    onEndCall: () -> Unit,
    onAccept: () -> Unit
) {
    val durationText = remember(activeCall.durationSeconds) {
        val minutes = activeCall.durationSeconds / 60
        val seconds = activeCall.durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp,
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onRestore() }
            .testTag("floating_mini_call_banner")
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                AdtAvatar(name = activeCall.contactName, sizeDp = 38)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (activeCall.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = null,
                            tint = if (activeCall.isConnected) CallGreen else Color(0xFFFFB703),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = activeCall.contactName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (activeCall.isConnected) "🔒 E2EE en cours • $durationText" else if (activeCall.isIncoming) "📞 Appel entrant..." else "Connexion...",
                        fontSize = 11.sp,
                        color = if (activeCall.isConnected) CallGreen else Color(0xFFFFB703)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (activeCall.isIncoming && !activeCall.isConnected) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(36.dp)
                            .background(CallGreen, CircleShape)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Répondre", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                } else {
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (activeCall.isMuted) CallRed else Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            if (activeCall.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Micro",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(36.dp)
                        .background(CallRed, CircleShape)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Raccrocher", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * TikTok Live Direct Discovery & Exploration Tab Content
 */
@Composable
fun LiveDirectTabContent(
    liveStreams: List<LiveStreamItem>,
    onWatchStream: (String) -> Unit,
    onStartLiveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<LiveCategory?>(null) }
    val filteredStreams = remember(liveStreams, selectedCategory) {
        if (selectedCategory == null) liveStreams
        else liveStreams.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. TikTok Live Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFFFE2C55), Color(0xFF25F4EE)))),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_live_hero_banner")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFE2C55)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
                                    Text("TIKTOK LIVE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                }
                            }
                            Text("Directs Certifiés", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x3325D366)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(13.dp))
                                Text("Chiffré WebRTC", color = Color(0xFF25D366), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = "Vibrez en direct avec les créateurs : envoyez des cœurs ❤️, faites pleuvoir des cadeaux animés 🎁, vibrez avec les battles PK en direct et profitez du son en haute définition.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = onStartLiveClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE2C55)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_hero_start_live")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lancer mon direct maintenant 🎥", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // 2. Category Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Explorer par thématique",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Tous les Directs", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFE2C55),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(LiveCategory.entries.toTypedArray()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text("${cat.icon} ${cat.label}", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFE2C55),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // 3. Streams Count Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredStreams.size} direct(s) actif(s)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFFE2C55)
                )
                Text(
                    text = "Glissez en plein écran pour zapper",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. Live Stream Cards List
        items(filteredStreams, key = { it.id }) { stream ->
            LiveStreamCardItem(
                stream = stream,
                onWatch = { onWatchStream(stream.id) }
            )
        }
    }
}

@Composable
fun LiveStreamCardItem(
    stream: LiveStreamItem,
    onWatch: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, Color(0xFFFE2C55).copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onWatch() }
            .testTag("stream_card_${stream.id}")
    ) {
        Column {
            // Header cover banner with Host and Live Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(stream.coverGradientStart), Color(stream.coverGradientEnd))
                        )
                    )
                    .padding(12.dp)
            ) {
                // Top Badges (LIVE + Viewers)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFFE2C55),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
                            Text("EN DIRECT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }

                    Surface(
                        color = Color(0x99000000),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("👁️", fontSize = 10.sp)
                            Text(
                                text = "${stream.viewerCount}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Bottom of Cover: Host Capsule & Sound Track
                Row(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = Color(0xFFFE2C55)
                        ) {}
                        AdtAvatar(name = stream.hostName, sizeDp = 34)
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(stream.hostName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                        }
                        Text(stream.musicTrack, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Card Body: Title, Category and Join Action
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stream.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "${stream.category.icon} ${stream.category.label}",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "❤️ ${formatLiveCount(stream.likesCount)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onWatch,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE2C55)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_join_live_${stream.id}")
                    ) {
                        Text("Rejoindre le Live", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun formatLiveCount(count: Int): String {
    return if (count >= 1_000_000) {
        String.format("%.1fM", count / 1_000_000f)
    } else if (count >= 1_000) {
        String.format("%.1fK", count / 1_000f)
    } else {
        count.toString()
    }
}
