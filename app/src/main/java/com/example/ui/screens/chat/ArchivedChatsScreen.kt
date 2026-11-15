package com.example.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatEntity
import com.example.data.search.ArchivedSearchOptions
import com.example.data.search.ArchivedSearchResult
import com.example.data.search.DateFilter
import com.example.data.search.MatchMode
import com.example.data.search.SenderFilter
import com.example.ui.components.AdtAvatar
import com.example.ui.theme.AdtAccentGreen
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedChatsScreen(viewModel: AdtViewModel) {
    val archivedChats by viewModel.archivedChats.collectAsState()
    val searchQuery by viewModel.archivedSearchQuery.collectAsState()
    val searchResults by viewModel.archivedSearchResults.collectAsState()
    val searchStats by viewModel.archivedSearchStats.collectAsState()
    val searchOptions by viewModel.archivedSearchOptions.collectAsState()
    val isLoading by viewModel.isArchivedSearchLoading.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    val quickSearchPhrases = listOf(
        "\"contrat signé\"",
        "\"fibre optique\"",
        "audit sécurité",
        "AES-256",
        "conformité",
        "facture",
        "Moov"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Discussions archivées",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Surface(
                                shape = CircleShape,
                                color = AdtPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${archivedChats.size}",
                                    color = AdtPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Recherche plein texte • Room Database",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("btn_back_archived")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.testTag("btn_search_filters")
                    ) {
                        Badge(
                            containerColor = if (searchOptions.matchMode != MatchMode.ALL_WORDS ||
                                searchOptions.dateFilter != DateFilter.ALL_TIME ||
                                searchOptions.senderFilter != SenderFilter.ALL
                            ) AdtPrimary else Color.Transparent
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filtres de recherche",
                                tint = if (searchOptions.matchMode != MatchMode.ALL_WORDS ||
                                    searchOptions.dateFilter != DateFilter.ALL_TIME ||
                                    searchOptions.senderFilter != SenderFilter.ALL
                                ) AdtPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Full-Text Search Field
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateArchivedSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = "Rechercher par mot-clé ou phrase dans l'historique...",
                                fontSize = 13.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Rechercher",
                                tint = AdtPrimary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearArchivedSearch() },
                                    modifier = Modifier.testTag("btn_clear_archived_search")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdtPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_bar_archived")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Search Suggestions
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(quickSearchPhrases) { phrase ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.clickable {
                                    viewModel.updateArchivedSearchQuery(phrase)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NorthWest,
                                        contentDescription = null,
                                        tint = AdtPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = phrase,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Active Filter Chips indicator bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { showFilterSheet = true },
                        label = { Text(searchOptions.matchMode.label, fontSize = 11.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(14.dp), tint = AdtPrimary)
                        }
                    )
                    if (searchOptions.dateFilter != DateFilter.ALL_TIME) {
                        AssistChip(
                            onClick = { showFilterSheet = true },
                            label = { Text(searchOptions.dateFilter.label, fontSize = 11.5.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                    if (searchOptions.senderFilter != SenderFilter.ALL) {
                        AssistChip(
                            onClick = { showFilterSheet = true },
                            label = { Text(searchOptions.senderFilter.label, fontSize = 11.5.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }

            // Search Stats Banner when searching
            if (searchQuery.isNotBlank() && searchStats != null) {
                item {
                    val stats = searchStats!!
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AdtPrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = AdtPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "${stats.matchCount} message(s) trouvé(s) dans ${stats.matchingChatsCount} discussion(s)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AdtPrimary
                                    )
                                    Text(
                                        text = "Indexé en ${stats.executionTimeMs} ms • ${stats.totalArchivedMessages} messages archivés analysés",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AdtAccentGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Room FTS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdtAccentGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Results List
            if (searchQuery.isNotBlank()) {
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AdtPrimary, modifier = Modifier.size(32.dp))
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Aucun message trouvé pour « $searchQuery »",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Vérifiez l'orthographe, essayez d'autres mots-clés ou modifiez les filtres de recherche.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(searchResults) { result ->
                        ArchivedSearchResultCard(
                            result = result,
                            onOpenChat = {
                                viewModel.openChat(result.message.chatId)
                            },
                            onUnarchiveChat = {
                                viewModel.setChatArchived(result.message.chatId, false)
                            }
                        )
                    }
                }
            } else {
                // When not searching: Display All Archived Conversations + Guidance
                item {
                    ArchivedInfoGuideCard()
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Toutes les discussions archivées (${archivedChats.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (archivedChats.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "Aucune discussion archivée",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Vous pouvez archiver des conversations depuis le menu de l'onglet Discussions pour les retrouver ici avec la recherche plein texte.",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(archivedChats) { chat ->
                        ArchivedChatItem(
                            chat = chat,
                            onOpenChat = { viewModel.openChat(chat.id) },
                            onUnarchiveChat = { viewModel.setChatArchived(chat.id, false) }
                        )
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtres de recherche plein texte",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            viewModel.updateArchivedSearchOptions(ArchivedSearchOptions())
                        }
                    ) {
                        Text("Réinitialiser", color = AdtPrimary, fontSize = 12.5.sp)
                    }
                }

                // Match Mode Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mode de correspondance", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    MatchMode.entries.forEach { mode ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (searchOptions.matchMode == mode) AdtPrimary.copy(alpha = 0.12f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateArchivedSearchOptions(searchOptions.copy(matchMode = mode))
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(mode.label, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
                                    Text(mode.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RadioButton(
                                    selected = searchOptions.matchMode == mode,
                                    onClick = { viewModel.updateArchivedSearchOptions(searchOptions.copy(matchMode = mode)) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Date Filter Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Période temporelle", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateFilter.entries.forEach { dateFilter ->
                            FilterChip(
                                selected = searchOptions.dateFilter == dateFilter,
                                onClick = {
                                    viewModel.updateArchivedSearchOptions(searchOptions.copy(dateFilter = dateFilter))
                                },
                                label = { Text(dateFilter.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Sender Filter Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Expéditeur", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SenderFilter.entries.forEach { senderFilter ->
                            FilterChip(
                                selected = searchOptions.senderFilter == senderFilter,
                                onClick = {
                                    viewModel.updateArchivedSearchOptions(searchOptions.copy(senderFilter = senderFilter))
                                },
                                label = { Text(senderFilter.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Button(
                    onClick = { showFilterSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Appliquer les filtres", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ArchivedSearchResultCard(
    result: ArchivedSearchResult,
    onOpenChat: () -> Unit,
    onUnarchiveChat: () -> Unit
) {
    val formattedTime = remember(result.message.timestamp) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(result.message.timestamp))
    }

    val annotatedSnippet = remember(result.snippet, result.matchStartInSnippet, result.matchEndInSnippet) {
        buildAnnotatedString {
            val snippet = result.snippet
            val start = result.matchStartInSnippet.coerceIn(0, snippet.length)
            val end = result.matchEndInSnippet.coerceIn(start, snippet.length)

            if (start > 0) {
                append(snippet.substring(0, start))
            }
            if (start < end) {
                withStyle(
                    SpanStyle(
                        color = Color(0xFF00A884),
                        fontWeight = FontWeight.Bold,
                        background = Color(0xFF00A884).copy(alpha = 0.18f)
                    )
                ) {
                    append(snippet.substring(start, end))
                }
            }
            if (end < snippet.length) {
                append(snippet.substring(end))
            }
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onOpenChat() }
            .testTag("search_result_${result.message.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Chat Title + Timestamp
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
                    AdtAvatar(name = result.chat?.title ?: result.message.senderName, sizeDp = 28)
                    Text(
                        text = result.chat?.title ?: "Discussion archivée",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sender identity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (result.message.isOutgoing) Icons.Default.ArrowOutward else Icons.Default.SouthWest,
                    contentDescription = null,
                    tint = if (result.message.isOutgoing) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (result.message.isOutgoing) "Vous" else result.message.senderName,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (result.message.isOutgoing) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (result.isExactPhrase) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AdtPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Phrase exacte",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdtPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Snippet with highlight
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = annotatedSnippet,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onUnarchiveChat,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Désarchiver", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(4.dp))
                FilledTonalButton(
                    onClick = onOpenChat,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = AdtPrimary.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = AdtPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Voir le message", fontSize = 11.5.sp, color = AdtPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ArchivedChatItem(
    chat: ChatEntity,
    onOpenChat: () -> Unit,
    onUnarchiveChat: () -> Unit
) {
    val formattedTime = remember(chat.lastTimestamp) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(chat.lastTimestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenChat() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("archived_chat_${chat.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            AdtAvatar(name = chat.title, sizeDp = 46)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chat.lastMessage,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onUnarchiveChat,
            modifier = Modifier.testTag("btn_unarchive_${chat.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Unarchive,
                contentDescription = "Désarchiver",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(start = 76.dp))
}

@Composable
fun ArchivedInfoGuideCard() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = AdtPrimary.copy(alpha = 0.07f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = AdtPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ManageSearch,
                        contentDescription = null,
                        tint = AdtPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Moteur de recherche plein texte Room",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdtPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Recherchez instantanément des phrases exactes (entre guillemets) ou des mots-clés dans vos conversations archivées, protégées par chiffrement local AES-256-GCM.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
