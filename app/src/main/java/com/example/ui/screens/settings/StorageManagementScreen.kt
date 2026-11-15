package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.ChatStorageConsumption
import com.example.data.model.StorageBreakdownOverview
import com.example.data.model.StorageMediaItem
import com.example.data.model.formatBytes
import com.example.ui.theme.*
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

private val StoragePhotoColor = Color(0xFF00A884)
private val StorageVideoColor = Color(0xFF34B7F1)
private val StorageAudioColor = Color(0xFFFF9800)
private val StorageDocColor = Color(0xFF9C27B0)
private val StorageCacheColor = Color(0xFF78909C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagementScreen(viewModel: AdtViewModel) {
    val overview by viewModel.storageOverview.collectAsState()
    val isLoading by viewModel.isStorageLoading.collectAsState()
    val feedbackMessage by viewModel.storageActionFeedback.collectAsState()
    val selectedChat by viewModel.selectedChatForStorage.collectAsState()

    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var selectedLargeFileIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteLargeConfirm by remember { mutableStateOf(false) }
    var chatFilterQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        if (selectedChat != null) {
            viewModel.selectChatForStorage(null)
        } else {
            viewModel.navigateTo(AppScreen.SETTINGS)
        }
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStorageFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gérer le stockage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Nettoyage des médias & base de données",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("btn_back_storage")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour aux paramètres"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshStorageOverview() },
                        enabled = !isLoading,
                        modifier = Modifier.testTag("btn_refresh_storage")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AdtPrimary
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val filteredChats = remember(overview.chatsStorage, chatFilterQuery) {
            if (chatFilterQuery.isBlank()) overview.chatsStorage
            else overview.chatsStorage.filter {
                it.chatTitle.contains(chatFilterQuery, ignoreCase = true)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Bar Visualizer & Overall Stats
            item {
                StorageOverviewCard(
                    overview = overview,
                    onClearCacheClick = { showClearCacheConfirm = true }
                )
            }

            // Large Files Cleanup Section (> 1.5 Mo)
            if (overview.largeFiles.isNotEmpty()) {
                item {
                    LargeFilesSection(
                        largeFiles = overview.largeFiles,
                        selectedIds = selectedLargeFileIds,
                        onToggleSelect = { id ->
                            selectedLargeFileIds = if (selectedLargeFileIds.contains(id)) {
                                selectedLargeFileIds - id
                            } else {
                                selectedLargeFileIds + id
                            }
                        },
                        onSelectAll = {
                            selectedLargeFileIds = if (selectedLargeFileIds.size == overview.largeFiles.size) {
                                emptySet()
                            } else {
                                overview.largeFiles.map { it.messageId }.toSet()
                            }
                        },
                        onDeleteSelected = {
                            if (selectedLargeFileIds.isNotEmpty()) {
                                showDeleteLargeConfirm = true
                            }
                        }
                    )
                }
            }

            // Chats Storage List Header & Search Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Discussions & Consommation",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdtPrimary
                        )
                        Text(
                            text = "${overview.chatsStorage.size} discussion(s)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = chatFilterQuery,
                        onValueChange = { chatFilterQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_storage_chats"),
                        placeholder = { Text("Filtrer les discussions...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (chatFilterQuery.isNotEmpty()) {
                                IconButton(onClick = { chatFilterQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Chats List
            if (filteredChats.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (chatFilterQuery.isNotBlank()) "Aucune discussion ne correspond à la recherche" else "Aucun média détecté dans vos discussions",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredChats, key = { it.chatId }) { chat ->
                    ChatStorageItemCard(
                        chat = chat,
                        onClick = { viewModel.selectChatForStorage(chat) }
                    )
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Dialog to Inspect & Delete Media for a Selected Chat
    selectedChat?.let { chat ->
        ChatMediaDetailDialog(
            chat = chat,
            onDismiss = { viewModel.selectChatForStorage(null) },
            onDeleteSelected = { messageIds ->
                viewModel.deleteStorageAttachments(messageIds)
            },
            onClearAllChatMedia = {
                viewModel.clearStorageForChat(chat.chatId)
            }
        )
    }

    // Confirm Clear App Cache Dialog
    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Vider le cache d'application ?") },
            text = {
                Text(
                    "Cette action supprimera ${overview.formattedCacheSize} de fichiers temporaires (miniatures Coil, fichiers audio transitoires). Vos messages et photos d'origine restent protégés."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheConfirm = false
                        viewModel.clearStorageCache()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                    modifier = Modifier.testTag("btn_confirm_clear_cache")
                ) {
                    Text("Vider le cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Confirm Delete Large Files Dialog
    if (showDeleteLargeConfirm) {
        val totalBytesToDelete = overview.largeFiles
            .filter { selectedLargeFileIds.contains(it.messageId) }
            .sumOf { it.sizeBytes }

        AlertDialog(
            onDismissRequest = { showDeleteLargeConfirm = false },
            title = { Text("Supprimer les pièces jointes ?") },
            text = {
                Text(
                    "Voulez-vous supprimer les ${selectedLargeFileIds.size} pièce(s) sélectionnée(s) (${formatBytes(totalBytesToDelete)}) ? L'historique des discussions sera conservé avec une mention d'espace libéré."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteLargeConfirm = false
                        viewModel.deleteStorageAttachments(selectedLargeFileIds)
                        selectedLargeFileIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_large")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteLargeConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun StorageOverviewCard(
    overview: StorageBreakdownOverview,
    onClearCacheClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stockage utilisé par ADT Pro",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatBytes(overview.totalAppSizeBytes),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdtPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AdtPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Libre : ${overview.formattedFreeSpace}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AdtPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Visual Segmented Bar
            StorageSegmentedBar(overview = overview)

            // Legend Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StorageLegendItem(color = StoragePhotoColor, label = "Photos", size = overview.photosTotalSizeBytes)
                StorageLegendItem(color = StorageVideoColor, label = "Vidéos", size = overview.videosTotalSizeBytes)
                StorageLegendItem(color = StorageAudioColor, label = "Audio", size = overview.audioTotalSizeBytes)
                StorageLegendItem(color = StorageDocColor, label = "Docs", size = overview.docsTotalSizeBytes)
                StorageLegendItem(color = StorageCacheColor, label = "Cache", size = overview.cacheSizeBytes)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Quick Cache Cleanup Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = StorageCacheColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text("Cache temporaire", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            overview.formattedCacheSize,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onClearCacheClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_clear_cache")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vider le cache", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StorageSegmentedBar(overview: StorageBreakdownOverview) {
    val total = (overview.totalAppSizeBytes).coerceAtLeast(1L).toFloat()
    val photoRatio = (overview.photosTotalSizeBytes / total).coerceIn(0f, 1f)
    val videoRatio = (overview.videosTotalSizeBytes / total).coerceIn(0f, 1f)
    val audioRatio = (overview.audioTotalSizeBytes / total).coerceIn(0f, 1f)
    val docRatio = (overview.docsTotalSizeBytes / total).coerceIn(0f, 1f)
    val cacheRatio = (overview.cacheSizeBytes / total).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (photoRatio > 0f) {
            Box(
                modifier = Modifier
                    .weight(photoRatio.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .background(StoragePhotoColor)
            )
        }
        if (videoRatio > 0f) {
            Box(
                modifier = Modifier
                    .weight(videoRatio.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .background(StorageVideoColor)
            )
        }
        if (audioRatio > 0f) {
            Box(
                modifier = Modifier
                    .weight(audioRatio.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .background(StorageAudioColor)
            )
        }
        if (docRatio > 0f) {
            Box(
                modifier = Modifier
                    .weight(docRatio.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .background(StorageDocColor)
            )
        }
        if (cacheRatio > 0f) {
            Box(
                modifier = Modifier
                    .weight(cacheRatio.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .background(StorageCacheColor)
            )
        }
    }
}

@Composable
private fun StorageLegendItem(color: Color, label: String, size: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = formatBytes(size),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LargeFilesSection(
    largeFiles: List<StorageMediaItem>,
    selectedIds: Set<String>,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Attachment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Fichiers volumineux (> 1.5 Mo)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onSelectAll,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (selectedIds.size == largeFiles.size) "Désélectionner" else "Tout choisir",
                        fontSize = 11.sp,
                        color = AdtPrimary
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(largeFiles, key = { it.messageId }) { item ->
                    val isSelected = selectedIds.contains(item.messageId)
                    LargeMediaCard(
                        item = item,
                        isSelected = isSelected,
                        onToggle = { onToggleSelect(item.messageId) }
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedIds.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Button(
                    onClick = onDeleteSelected,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_delete_large_files")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Supprimer (${selectedIds.size}) fichier(s) volumineux",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LargeMediaCard(
    item: StorageMediaItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.5.dp,
            if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .width(130.dp)
            .clickable { onToggle() }
            .testTag("large_file_item_${item.messageId}")
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.mediaType == "PHOTO" || item.mediaType == "IMAGE") {
                    AsyncImage(
                        model = item.mediaUrl,
                        contentDescription = "Miniature",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = when (item.mediaType) {
                            "VIDEO" -> Icons.Default.Videocam
                            "AUDIO" -> Icons.Default.Mic
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = when (item.mediaType) {
                            "VIDEO" -> StorageVideoColor
                            "AUDIO" -> StorageAudioColor
                            else -> StorageDocColor
                        },
                        modifier = Modifier.size(32.dp)
                    )
                }

                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(24.dp)
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Text(
                        text = item.formattedSize,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.chatTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.formattedDate,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChatStorageItemCard(
    chat: ChatStorageConsumption,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("chat_storage_item_${chat.chatId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(AdtPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (chat.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = chat.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = AdtPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.chatTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = chat.formattedTotalSize,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdtPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chat.photosCount > 0) {
                        Text(
                            text = "📷 ${chat.photosCount}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (chat.videosCount > 0) {
                        Text(
                            text = "🎬 ${chat.videosCount}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (chat.audioCount > 0) {
                        Text(
                            text = "🎵 ${chat.audioCount}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (chat.docsCount > 0) {
                        Text(
                            text = "📄 ${chat.docsCount}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (chat.totalMediaCount == 0) {
                        Text(
                            text = "Aucun média",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ouvrir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ChatMediaDetailDialog(
    chat: ChatStorageConsumption,
    onDismiss: () -> Unit,
    onDeleteSelected: (Collection<String>) -> Unit,
    onClearAllChatMedia: () -> Unit
) {
    var selectedMediaTypeFilter by remember { mutableStateOf("ALL") }
    var selectedMessageIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showConfirmClearChat by remember { mutableStateOf(false) }

    val filteredItems = remember(chat.mediaItems, selectedMediaTypeFilter) {
        when (selectedMediaTypeFilter) {
            "PHOTO" -> chat.mediaItems.filter { it.mediaType == "PHOTO" || it.mediaType == "IMAGE" }
            "VIDEO" -> chat.mediaItems.filter { it.mediaType == "VIDEO" }
            "AUDIO" -> chat.mediaItems.filter { it.mediaType == "AUDIO" }
            "DOC" -> chat.mediaItems.filter { it.mediaType == "DOC" || it.mediaType == "DOCUMENT" }
            else -> chat.mediaItems
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chat.chatTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${chat.formattedTotalSize} • ${chat.totalMediaCount} élément(s)",
                            fontSize = 12.sp,
                            color = AdtPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Media Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedMediaTypeFilter == "ALL",
                            onClick = { selectedMediaTypeFilter = "ALL" },
                            label = { Text("Tous (${chat.totalMediaCount})", fontSize = 11.sp) }
                        )
                    }
                    if (chat.photosCount > 0) {
                        item {
                            FilterChip(
                                selected = selectedMediaTypeFilter == "PHOTO",
                                onClick = { selectedMediaTypeFilter = "PHOTO" },
                                label = { Text("Photos (${chat.photosCount})", fontSize = 11.sp) }
                            )
                        }
                    }
                    if (chat.videosCount > 0) {
                        item {
                            FilterChip(
                                selected = selectedMediaTypeFilter == "VIDEO",
                                onClick = { selectedMediaTypeFilter = "VIDEO" },
                                label = { Text("Vidéos (${chat.videosCount})", fontSize = 11.sp) }
                            )
                        }
                    }
                    if (chat.audioCount > 0) {
                        item {
                            FilterChip(
                                selected = selectedMediaTypeFilter == "AUDIO",
                                onClick = { selectedMediaTypeFilter = "AUDIO" },
                                label = { Text("Audios (${chat.audioCount})", fontSize = 11.sp) }
                            )
                        }
                    }
                    if (chat.docsCount > 0) {
                        item {
                            FilterChip(
                                selected = selectedMediaTypeFilter == "DOC",
                                onClick = { selectedMediaTypeFilter = "DOC" },
                                label = { Text("Documents (${chat.docsCount})", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Selection Actions Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            selectedMessageIds = if (selectedMessageIds.size == filteredItems.size) {
                                emptySet()
                            } else {
                                filteredItems.map { it.messageId }.toSet()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (selectedMessageIds.size == filteredItems.size) "Désélectionner tout" else "Sélectionner tout",
                            fontSize = 11.sp,
                            color = AdtPrimary
                        )
                    }

                    if (chat.totalMediaCount > 0) {
                        TextButton(
                            onClick = { showConfirmClearChat = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("btn_clear_chat_all_media")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tout purger", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // List of items
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucun élément dans cette catégorie", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredItems, key = { it.messageId }) { item ->
                            val isChecked = selectedMessageIds.contains(item.messageId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedMessageIds = if (isChecked) selectedMessageIds - item.messageId
                                        else selectedMessageIds + item.messageId
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedMessageIds = if (checked == true) selectedMessageIds + item.messageId
                                        else selectedMessageIds - item.messageId
                                    },
                                    modifier = Modifier.size(20.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (item.mediaType) {
                                            "PHOTO", "IMAGE" -> Icons.Default.Image
                                            "VIDEO" -> Icons.Default.Videocam
                                            "AUDIO" -> Icons.Default.Mic
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        tint = when (item.mediaType) {
                                            "PHOTO", "IMAGE" -> StoragePhotoColor
                                            "VIDEO" -> StorageVideoColor
                                            "AUDIO" -> StorageAudioColor
                                            else -> StorageDocColor
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.mediaType} (${item.formattedSize})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${item.senderName} • ${item.formattedDate}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Delete Button for Selection
                if (selectedMessageIds.isNotEmpty()) {
                    val selectedBytes = filteredItems
                        .filter { selectedMessageIds.contains(it.messageId) }
                        .sumOf { it.sizeBytes }

                    Button(
                        onClick = {
                            onDeleteSelected(selectedMessageIds)
                            selectedMessageIds = emptySet()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("btn_delete_selected_chat_media")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Supprimer (${selectedMessageIds.size}) • ${formatBytes(selectedBytes)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Confirmation for purge all chat media
    if (showConfirmClearChat) {
        AlertDialog(
            onDismissRequest = { showConfirmClearChat = false },
            title = { Text("Purger tous les médias ?") },
            text = {
                Text(
                    "Voulez-vous supprimer l'ensemble des ${chat.totalMediaCount} pièces jointes (${chat.formattedTotalSize}) de cette discussion ? Les messages texte resteront conservés."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearChat = false
                        onClearAllChatMedia()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Purger")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearChat = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
