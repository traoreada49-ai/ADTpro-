package com.example.ui.screens.channel

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.ChannelPostEntity
import com.example.ui.components.AdtAvatar
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailScreen(viewModel: AdtViewModel) {
    val channelId by viewModel.activeChannelId.collectAsState()
    val channels by viewModel.allChannels.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val channel = channels.find { it.id == channelId } ?: return

    val postsFlow = remember(channel.id) { viewModel.getPostsForChannel(channel.id) }
    val posts by postsFlow.collectAsState(initial = emptyList())

    val isChannelAdmin = remember(channel, currentUser) {
        channel.isOwner ||
                (currentUser != null && (currentUser?.phone?.isNotBlank() == true && currentUser?.phone == channel.adminPhone)) ||
                channel.adminName.contains("Créateur", ignoreCase = true) ||
                channel.adminName.contains("Direction", ignoreCase = true) ||
                channel.adminName.contains("Vous", ignoreCase = true)
    }

    var showAdminBroadcastModal by remember { mutableStateOf(false) }
    var showChannelInfoModal by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var quickBroadcastText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clickable { showChannelInfoModal = true }
                            .padding(vertical = 4.dp)
                    ) {
                        AdtAvatar(name = channel.name, sizeDp = 40)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    channel.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (channel.isVerified) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = "Chaîne certifiée",
                                        tint = AdtPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                "${channel.followersCount} abonnés • ${channel.category}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.MAIN_TABS) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    // Mute / Unmute bell
                    IconButton(onClick = { viewModel.toggleChannelMute(channel.id, channel.isMuted) }) {
                        Icon(
                            if (channel.isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (channel.isMuted) MaterialTheme.colorScheme.onSurfaceVariant else AdtPrimary
                        )
                    }

                    // Follow/Unfollow or Admin Badge
                    if (isChannelAdmin) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AdtPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(14.dp))
                                Text("Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                            }
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { viewModel.toggleChannelFollow(channel.id, channel.isFollowing) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (channel.isFollowing) MaterialTheme.colorScheme.surfaceVariant else AdtPrimary,
                                contentColor = if (channel.isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                            ),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                if (channel.isFollowing) "Abonné" else "+ Suivre",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Overflow Menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options de chaîne")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Infos sur la chaîne") },
                                onClick = {
                                    showMenu = false
                                    showChannelInfoModal = true
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Copier le lien d'invitation") },
                                onClick = {
                                    showMenu = false
                                    clipboardManager.setText(AnnotatedString("https://adt.pro/channel/${channel.id}"))
                                    copiedNotice = true
                                },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                            )
                            if (isChannelAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Créer un sondage", color = AdtPrimary) },
                                    onClick = {
                                        showMenu = false
                                        showAdminBroadcastModal = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Poll, contentDescription = null, tint = AdtPrimary) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Supprimer la chaîne", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.deleteChannel(channel.id)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (isChannelAdmin) {
                // Admin Broadcast Composer Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Rich Broadcast Dialog trigger (Poll, Announcement, Photo)
                            IconButton(
                                onClick = { showAdminBroadcastModal = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AdtPrimary.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = "Nouvelle diffusion riche",
                                    tint = AdtPrimary
                                )
                            }

                            OutlinedTextField(
                                value = quickBroadcastText,
                                onValueChange = { quickBroadcastText = it },
                                placeholder = { Text("Diffuser à tous les abonnés...", fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_broadcast_message"),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AdtPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )

                            IconButton(
                                onClick = {
                                    if (quickBroadcastText.isNotBlank()) {
                                        viewModel.broadcastPost(
                                            channelId = channel.id,
                                            text = quickBroadcastText,
                                            mediaType = "TEXT"
                                        )
                                        quickBroadcastText = ""
                                    }
                                },
                                enabled = quickBroadcastText.isNotBlank(),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (quickBroadcastText.isNotBlank()) AdtPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("btn_send_broadcast")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Diffuser",
                                    tint = if (quickBroadcastText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Follower Read-Only Information Banner
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Diffusion unidirectionnelle • Seuls les administrateurs peuvent publier",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
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
            // Channel Role Banner
            Surface(
                color = if (isChannelAdmin) AdtPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (isChannelAdmin) Icons.Default.Campaign else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = AdtPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            if (isChannelAdmin)
                                "👑 Vous diffusez en tant qu'administrateur à ${channel.followersCount} abonnés."
                            else
                                "📢 Canal officiel d'actualités et d'annonces vérifiées ADT Pro.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isChannelAdmin) {
                        TextButton(
                            onClick = { showAdminBroadcastModal = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("+ Options", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                        }
                    }
                }
            }

            // Posts Stream
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = AdtPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Aucune publication pour le moment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Les messages et annonces diffusés par les administrateurs apparaîtront ici.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        ChannelPostCard(
                            post = post,
                            channel = channel,
                            isChannelAdmin = isChannelAdmin,
                            onReact = { emoji -> viewModel.reactToChannelPost(post.id, emoji) },
                            onVotePoll = { optionIdx -> viewModel.voteChannelPoll(post.id, optionIdx) },
                            onTogglePin = { viewModel.togglePinChannelPost(post.id, !post.isPinned) },
                            onDelete = { viewModel.deleteChannelPost(post.id) },
                            onShare = {
                                clipboardManager.setText(AnnotatedString("${channel.name} :\n${post.text}"))
                                copiedNotice = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal: Create Rich Broadcast (Admin only)
    if (showAdminBroadcastModal) {
        CreateBroadcastDialog(
            channelName = channel.name,
            onDismiss = { showAdminBroadcastModal = false },
            onBroadcast = { text, type, isPinned, pollQ, opt1, opt2, opt3, opt4 ->
                viewModel.broadcastPost(
                    channelId = channel.id,
                    text = text,
                    mediaType = type,
                    isPinned = isPinned,
                    pollQuestion = pollQ,
                    pollOption1 = opt1,
                    pollOption2 = opt2,
                    pollOption3 = opt3,
                    pollOption4 = opt4
                )
                showAdminBroadcastModal = false
            }
        )
    }

    // Modal: Channel Info & Subscribers Details
    if (showChannelInfoModal) {
        ChannelInfoDialog(
            channel = channel,
            isChannelAdmin = isChannelAdmin,
            onDismiss = { showChannelInfoModal = false },
            onToggleFollow = { viewModel.toggleChannelFollow(channel.id, channel.isFollowing) },
            onToggleMute = { viewModel.toggleChannelMute(channel.id, channel.isMuted) }
        )
    }

    if (copiedNotice) {
        LaunchedEffect(copiedNotice) {
            snackbarHostState.showSnackbar("Copié dans le presse-papiers")
            copiedNotice = false
        }
    }
}

@Composable
fun ChannelPostCard(
    post: ChannelPostEntity,
    channel: ChannelEntity,
    isChannelAdmin: Boolean,
    onReact: (String) -> Unit,
    onVotePoll: (Int) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showPostMenu by remember { mutableStateOf(false) }

    val formattedTime = remember(post.timestamp) {
        val sdf = SimpleDateFormat("dd MMM • HH:mm", Locale.FRENCH)
        sdf.format(Date(post.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (post.mediaType) {
                "ANNOUNCEMENT" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (post.isPinned) {
                    Modifier.border(1.dp, AdtPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                } else Modifier
            )
            .testTag("broadcast_post_${post.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Pinned indicator
            if (post.isPinned) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(Icons.Default.PushPin, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(13.dp))
                    Text(
                        "Annonce épinglée",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdtPrimary
                    )
                }
            }

            // Post Header (Author & Role)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AdtAvatar(name = channel.name, sizeDp = 32)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                channel.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (channel.isVerified) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(13.dp))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                post.authorRole,
                                fontSize = 10.sp,
                                color = AdtPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                formattedTime,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isChannelAdmin) {
                    Box {
                        IconButton(
                            onClick = { showPostMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "Options du post", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = showPostMenu,
                            onDismissRequest = { showPostMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (post.isPinned) "Désépingler" else "Épingler en haut") },
                                onClick = {
                                    showPostMenu = false
                                    onTogglePin()
                                },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Supprimer la diffusion", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showPostMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Text / Announcement Header
            if (post.mediaType == "ANNOUNCEMENT") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AdtPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                        Text(
                            "COMMUNIQUÉ OFFICIEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdtPrimary
                        )
                    }
                }
            }

            Text(
                text = post.text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Interactive Poll rendering if type is POLL
            if (post.mediaType == "POLL" && !post.pollQuestion.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                ChannelPollWidget(
                    post = post,
                    onVote = onVotePoll
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer (Reactions bar, Views counter, Share button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Emoji Reactions Strip
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReactionBadge(emoji = "❤️", count = post.reactionsHeart, isSelected = post.userReaction == "❤️") { onReact("❤️") }
                    ReactionBadge(emoji = "👍", count = post.reactionsLike, isSelected = post.userReaction == "👍") { onReact("👍") }
                    ReactionBadge(emoji = "🚀", count = post.reactionsRocket, isSelected = post.userReaction == "🚀") { onReact("🚀") }
                    ReactionBadge(emoji = "🔥", count = post.reactionsFire, isSelected = post.userReaction == "🔥") { onReact("🔥") }
                    ReactionBadge(emoji = "👏", count = post.reactionsClap, isSelected = post.userReaction == "👏") { onReact("👏") }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        Text(
                            "${post.viewsCount}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Partager la diffusion",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelPollWidget(
    post: ChannelPostEntity,
    onVote: (Int) -> Unit
) {
    val options = listOfNotNull(
        post.pollOption1?.let { 1 to it },
        post.pollOption2?.let { 2 to it },
        post.pollOption3?.let { 3 to it },
        post.pollOption4?.let { 4 to it }
    )

    val votes = listOf(post.pollVotes1, post.pollVotes2, post.pollVotes3, post.pollVotes4)
    val totalVotes = votes.sum().coerceAtLeast(1)
    val hasVoted = post.userVotedOption != null

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Poll, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                Text(
                    post.pollQuestion ?: "Sondage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            options.forEach { (index, text) ->
                val voteCount = votes.getOrElse(index - 1) { 0 }
                val percentage = (voteCount * 100) / totalVotes
                val isSelected = post.userVotedOption == index

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) AdtPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = !hasVoted) { onVote(index) }
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Progress bar fill if voted
                        if (hasVoted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(percentage / 100f)
                                    .background(if (isSelected) AdtPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (hasVoted) {
                                Text(
                                    "$percentage%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${votes.sum()} votes enregistrés • ${if (hasVoted) "Votre vote est pris en compte" else "Touchez une option pour voter"}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReactionBadge(
    emoji: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AdtPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AdtPrimary) else null,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(emoji, fontSize = 12.sp)
            if (count > 0) {
                Text(
                    "$count",
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBroadcastDialog(
    channelName: String,
    onDismiss: () -> Unit,
    onBroadcast: (text: String, type: String, isPinned: Boolean, pollQ: String?, opt1: String?, opt2: String?, opt3: String?, opt4: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf("TEXT") } // "TEXT", "ANNOUNCEMENT", "POLL"
    var broadcastText by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }

    // Poll fields
    var pollQuestion by remember { mutableStateOf("") }
    var option1 by remember { mutableStateOf("") }
    var option2 by remember { mutableStateOf("") }
    var option3 by remember { mutableStateOf("") }
    var option4 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = AdtPrimary)
                Text("Diffuser sur « $channelName »", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Type de diffusion :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == "TEXT",
                            onClick = { selectedType = "TEXT" },
                            label = { Text("Texte", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Subject, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = selectedType == "ANNOUNCEMENT",
                            onClick = { selectedType = "ANNOUNCEMENT" },
                            label = { Text("Annonce 📢", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedType == "POLL",
                            onClick = { selectedType = "POLL" },
                            label = { Text("Sondage 📊", fontSize = 11.sp) }
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = broadcastText,
                        onValueChange = { broadcastText = it },
                        label = { Text(if (selectedType == "POLL") "Message introductif (optionnel)" else "Message à diffuser *") },
                        placeholder = { Text("Rédigez votre communiqué ou annonce...") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (selectedType == "POLL") {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Question du sondage *", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = pollQuestion,
                                onValueChange = { pollQuestion = it },
                                placeholder = { Text("Ex: Quel horaire préférez-vous pour le webinaire ?") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Text("Options de vote :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = option1,
                                onValueChange = { option1 = it },
                                placeholder = { Text("Option 1 *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = option2,
                                onValueChange = { option2 = it },
                                placeholder = { Text("Option 2 *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = option3,
                                onValueChange = { option3 = it },
                                placeholder = { Text("Option 3 (optionnelle)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = option4,
                                onValueChange = { option4 = it },
                                placeholder = { Text("Option 4 (optionnelle)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                            Text("Épingler en haut de la chaîne", fontSize = 12.sp)
                        }
                        Switch(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalMessage = if (broadcastText.isBlank() && selectedType == "POLL") pollQuestion else broadcastText
                    onBroadcast(
                        finalMessage,
                        selectedType,
                        isPinned,
                        if (selectedType == "POLL") pollQuestion else null,
                        if (selectedType == "POLL") option1.ifBlank { null } else null,
                        if (selectedType == "POLL") option2.ifBlank { null } else null,
                        if (selectedType == "POLL") option3.ifBlank { null } else null,
                        if (selectedType == "POLL") option4.ifBlank { null } else null
                    )
                },
                enabled = (selectedType == "POLL" && pollQuestion.isNotBlank() && option1.isNotBlank() && option2.isNotBlank()) ||
                        (selectedType != "POLL" && broadcastText.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
            ) {
                Text("Diffuser immédiatement", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun ChannelInfoDialog(
    channel: ChannelEntity,
    isChannelAdmin: Boolean,
    onDismiss: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleMute: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdtAvatar(name = channel.name, sizeDp = 46)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (channel.isVerified) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(channel.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    channel.description,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Abonnés", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${channel.followersCount} membres", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Administrateur", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(channel.adminName.ifBlank { "Direction ADT Pro" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Type de diffusion", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Unidirectionnelle (Public)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(18.dp))
                        Text(
                            "Votre identité et votre numéro de téléphone restent 100% masqués auprès des autres abonnés de la chaîne.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", fontWeight = FontWeight.Bold, color = AdtPrimary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelScreen(viewModel: AdtViewModel) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Actualités & Business") }

    val categories = listOf(
        "Actualités & Business",
        "Opérateurs & Réseaux",
        "Emploi & Carrière",
        "Technologies & IA",
        "Fintech & Mobile Money",
        "Santé & Sécurité",
        "Communautaire & Teranga"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer une chaîne de diffusion", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.MAIN_TABS) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
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
                        Column {
                            Text("Diffusion unidirectionnelle illimitée", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "En tant qu'administrateur, vous seul pourrez publier des textes, sondages et communiqués. Les abonnés reçoivent les alertes en temps réel.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("Nom de la chaîne *") },
                    placeholder = { Text("Ex: Marché Télécoms & Business Afrique") },
                    singleLine = true,
                    supportingText = { Text("${name.length}/50 caractères") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_channel_name"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AdtPrimary)
                )
            }

            item {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { if (it.length <= 250) desc = it },
                    label = { Text("Description de la chaîne *") },
                    placeholder = { Text("Expliquez ce que vous allez diffuser aux abonnés...") },
                    maxLines = 4,
                    supportingText = { Text("${desc.length}/250 caractères") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_channel_description"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AdtPrimary)
                )
            }

            item {
                Text("Catégorie professionnelle *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Live Preview Card
            item {
                Text("Aperçu de votre chaîne :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdtAvatar(name = name.ifBlank { "Ma Chaîne" }, sizeDp = 44)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                name.ifBlank { "Nom de votre chaîne" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                desc.ifBlank { "Description de vos diffusions professionnelles..." },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "👑 Administrateur : Vous • 1 abonné",
                                fontSize = 10.sp,
                                color = AdtPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.createChannel(
                                name = name.trim(),
                                description = desc.ifBlank { "Chaîne officielle de diffusion." }.trim(),
                                category = selectedCategory
                            )
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_confirm_create_channel"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Créer et lancer la chaîne", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}
