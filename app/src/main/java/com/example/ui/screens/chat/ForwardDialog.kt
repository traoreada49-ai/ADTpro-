package com.example.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MessageEntity
import com.example.ui.components.AdtAvatar
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardMessageDialog(
    viewModel: AdtViewModel,
    message: MessageEntity
) {
    val chats by viewModel.activeChats.collectAsState()
    val contacts by viewModel.allContacts.collectAsState()
    val channels by viewModel.allChannels.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { viewModel.cancelForwarding() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Transférer à...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = when (message.mediaType) {
                            "PHOTO" -> "Photo : ${message.text}"
                            "VIDEO" -> "Vidéo : ${message.text}"
                            "AUDIO" -> "Message vocal (${message.mediaDuration}s)"
                            "STICKER" -> "Autocollant : ${message.text}"
                            else -> message.text.take(30) + if (message.text.length > 30) "..." else ""
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { viewModel.cancelForwarding() }) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un contact ou groupe...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filteredChats = chats.filter { it.title.contains(searchQuery, ignoreCase = true) }
                    val filteredContacts = contacts.filter { it.name.contains(searchQuery, ignoreCase = true) }

                    item {
                        Text(
                            "Discussions récentes & Groupes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdtPrimary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(filteredChats) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.completeForwardTo(chat.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                AdtAvatar(name = chat.title, sizeDp = 40)
                                Column {
                                    Text(
                                        chat.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (chat.isGroup) "Groupe (${chat.membersCount} membres)" else "Discussion privée",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.completeForwardTo(chat.id) },
                                modifier = Modifier.testTag("btn_forward_${chat.id}")
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Envoyer",
                                    tint = AdtPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        },
        confirmButton = {}
    )
}
