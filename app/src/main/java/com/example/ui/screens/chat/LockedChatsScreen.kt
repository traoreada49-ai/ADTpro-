package com.example.ui.screens.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.local.entity.ChatEntity
import com.example.ui.components.AdtAvatar
import com.example.ui.theme.AdtAccentGreen
import com.example.ui.theme.AdtPrimary
import com.example.util.BiometricAuthManager
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedChatsScreen(viewModel: AdtViewModel) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val isSessionUnlocked by viewModel.isLockedChatsSessionUnlocked.collectAsState()
    val lockedChats by viewModel.lockedChats.collectAsState()
    val activeChats by viewModel.activeChats.collectAsState()
    val twoStepState by viewModel.twoStepVerificationState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddChatDialog by remember { mutableStateOf(false) }
    var chatToUnlockConfirm by remember { mutableStateOf<ChatEntity?>(null) }
    var authErrorText by remember { mutableStateOf<String?>(null) }

    // Intercept back button to automatically re-lock session
    BackHandler {
        viewModel.lockLockedChatsSession()
        viewModel.navigateTo(AppScreen.MAIN_TABS)
    }

    val triggerBiometricAuth = {
        if (activity != null) {
            authErrorText = null
            BiometricAuthManager.authenticateForLockedChats(
                activity = activity,
                title = "Conversations verrouillées",
                subtitle = "Confirmez votre empreinte digitale ou visage pour accéder aux conversations protégées",
                negativeButtonText = if (twoStepState.isEnabled) "Code PIN" else "Annuler",
                onSuccess = {
                    viewModel.unlockLockedChatsSession()
                    authErrorText = null
                },
                onError = { code, errString ->
                    authErrorText = errString.toString()
                },
                onFailed = {
                    authErrorText = "Empreinte ou visage non reconnu. Réessayez."
                }
            )
        } else {
            // Fallback unlock if activity context unavailable
            viewModel.unlockLockedChatsSession()
        }
    }

    // Auto-prompt biometric on entering if not yet unlocked
    LaunchedEffect(isSessionUnlocked) {
        if (!isSessionUnlocked) {
            triggerBiometricAuth()
        }
    }

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
                                text = "Conversations verrouillées",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Surface(
                                shape = CircleShape,
                                color = AdtAccentGreen.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = AdtAccentGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${lockedChats.size}",
                                        color = AdtAccentGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Protégé par empreinte digitale & visage",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.lockLockedChatsSession()
                            viewModel.navigateTo(AppScreen.MAIN_TABS)
                        },
                        modifier = Modifier.testTag("btn_back_locked_chats")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (isSessionUnlocked) {
                        IconButton(
                            onClick = { showAddChatDialog = true },
                            modifier = Modifier.testTag("btn_add_to_locked_chats")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Verrouiller une discussion", tint = AdtPrimary)
                        }
                        IconButton(
                            onClick = {
                                viewModel.lockLockedChatsSession()
                                Toast.makeText(context, "Conversations re-verrouillées", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("btn_relock_locked_chats")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Re-verrouiller", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isSessionUnlocked) {
                // LOCK CHALLENGE SCREEN
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AdtPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(100.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = "Biométrie",
                                tint = AdtPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Conversations verrouillées",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pour préserver votre confidentialité, scannez votre empreinte digitale ou utilisez la reconnaissance faciale pour afficher ces conversations.",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    if (authErrorText != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = authErrorText!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { triggerBiometricAuth() },
                        colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(50.dp)
                            .testTag("btn_unlock_biometric_action")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Déverrouiller avec la biométrie", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.navigateTo(AppScreen.MAIN_TABS)
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Text("Retour aux discussions")
                    }
                }
            } else {
                // UNLOCKED VIEW: LIST OF LOCKED CHATS
                val filteredChats = remember(lockedChats, searchQuery) {
                    if (searchQuery.isBlank()) lockedChats
                    else lockedChats.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        it.lastMessage.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // INFO BANNER
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = AdtPrimary.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = AdtPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Protection Biométrique & Chiffrement",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = AdtPrimary
                                    )
                                    Text(
                                        text = "Ces discussions sont masquées de l'écran d'accueil et requièrent une validation biométrique à chaque accès.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // SEARCH BAR
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Rechercher dans les discussions verrouillées...", fontSize = 13.5.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = AdtPrimary)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AdtPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("search_bar_locked_chats")
                        )
                    }

                    if (filteredChats.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (searchQuery.isBlank()) "Aucune discussion verrouillée" else "Aucun résultat trouvé",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (searchQuery.isBlank())
                                        "Verrouillez des conversations privées pour les masquer de la liste principale et les protéger par votre empreinte ou visage."
                                    else "Aucune discussion verrouillée ne correspond à votre recherche.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                FilledTonalButton(
                                    onClick = { showAddChatDialog = true },
                                    modifier = Modifier.testTag("btn_lock_new_chat_empty")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verrouiller une discussion")
                                }
                            }
                        }
                    } else {
                        items(filteredChats, key = { it.id }) { chat ->
                            val timeFormatted = remember(chat.lastTimestamp) {
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.lastTimestamp))
                            }
                            var showItemMenu by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.openChat(chat.id)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("locked_chat_item_${chat.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box {
                                        AdtAvatar(name = chat.title, sizeDp = 50, isOnline = !chat.isGroup)
                                        Surface(
                                            shape = CircleShape,
                                            color = AdtAccentGreen,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .align(Alignment.BottomEnd)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = chat.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Text(
                                                text = timeFormatted,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            if (chat.unreadCount > 0) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = AdtPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            "${chat.unreadCount}",
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Box {
                                    IconButton(onClick = { showItemMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                                    }
                                    DropdownMenu(
                                        expanded = showItemMenu,
                                        onDismissRequest = { showItemMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Ouvrir la discussion") },
                                            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, tint = AdtPrimary) },
                                            onClick = {
                                                showItemMenu = false
                                                viewModel.openChat(chat.id)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Déverrouiller la discussion") },
                                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = AdtAccentGreen) },
                                            onClick = {
                                                showItemMenu = false
                                                chatToUnlockConfirm = chat
                                            }
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 80.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }

    // DIALOG: ADD/LOCK AN ACTIVE CHAT
    if (showAddChatDialog) {
        AlertDialog(
            onDismissRequest = { showAddChatDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AdtPrimary)
                    Text("Verrouiller une discussion", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Text(
                        text = "Sélectionnez une discussion à masquer et protéger par empreinte digitale / visage :",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeChats.isEmpty()) {
                        Text("Toutes vos discussions sont déjà verrouillées.", fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(activeChats) { chat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setChatLocked(chat.id, true)
                                            showAddChatDialog = false
                                            Toast.makeText(context, "'${chat.title}' est maintenant verrouillée", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AdtAvatar(name = chat.title, sizeDp = 40, isOnline = !chat.isGroup)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(chat.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(chat.lastMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.Lock, contentDescription = "Verrouiller", tint = AdtPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddChatDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // DIALOG: CONFIRM UNLOCK CHAT
    if (chatToUnlockConfirm != null) {
        val chat = chatToUnlockConfirm!!
        AlertDialog(
            onDismissRequest = { chatToUnlockConfirm = null },
            icon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = AdtAccentGreen) },
            title = { Text("Déverrouiller cette discussion ?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "La discussion '${chat.title}' sera réintégrée dans la liste principale et ne sera plus protégée par authentification biométrique.",
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setChatLocked(chat.id, false)
                        chatToUnlockConfirm = null
                        Toast.makeText(context, "Discussion déverrouillée", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdtAccentGreen)
                ) {
                    Text("Déverrouiller")
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToUnlockConfirm = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
