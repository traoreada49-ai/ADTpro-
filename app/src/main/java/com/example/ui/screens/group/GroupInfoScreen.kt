package com.example.ui.screens.group

import androidx.compose.animation.*
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
import androidx.compose.material.icons.outlined.*
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
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.GroupMemberEntity
import com.example.data.model.EphemeralMessageDuration
import com.example.ui.screens.chat.EphemeralMessageTimerDialog
import com.example.ui.components.AdtAvatar
import com.example.ui.theme.AdtPrimary
import com.example.ui.theme.AdtSecondary
import com.example.ui.theme.CallRed
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(viewModel: AdtViewModel) {
    val activeChatId by viewModel.activeChatId.collectAsState()
    val chats by viewModel.activeChats.collectAsState()
    val currentChat = chats.find { it.id == activeChatId }
    val allContacts by viewModel.allContacts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val membersFlow = remember(activeChatId) {
        if (activeChatId != null) viewModel.getGroupMembers(activeChatId!!) else null
    }
    val members by (membersFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showEditInfoDialog by remember { mutableStateOf(false) }
    var showLeaveGroupConfirm by remember { mutableStateOf(false) }
    var showDeleteGroupConfirm by remember { mutableStateOf(false) }
    var selectedMemberForAction by remember { mutableStateOf<GroupMemberEntity?>(null) }
    var memberToRemove by remember { mutableStateOf<GroupMemberEntity?>(null) }
    var showEphemeralDialog by remember { mutableStateOf(false) }

    val chatEphemeralDurations by viewModel.chatEphemeralDurations.collectAsState()
    val currentEphemeralDuration = activeChatId?.let { id ->
        chatEphemeralDurations[id] ?: viewModel.getChatEphemeralDuration(id)
    } ?: EphemeralMessageDuration.OFF

    val myPhone = currentUser?.phone ?: ""
    val isUserAdmin = members.any { it.contactPhone == myPhone && it.isAdmin } || currentChat?.creatorPhone == myPhone

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Infos du groupe", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.CHAT_ROOM) },
                        modifier = Modifier.testTag("btn_group_info_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditInfoDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier le groupe", tint = AdtPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (currentChat == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aucun groupe sélectionné", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Group Hero Header
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Large Avatar
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                shape = CircleShape,
                                color = AdtPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(96.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Groups,
                                        contentDescription = "Groupe",
                                        tint = AdtPrimary,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = AdtPrimary,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Changer la photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Group Title
                        Text(
                            text = currentChat.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.testTag("group_detail_title")
                        )

                        // Subtitle with member count and encryption
                        Text(
                            text = "Groupe • ${members.size.coerceAtLeast(currentChat.membersCount)} membres",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Quick action buttons (Call, Video, Add, Search)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            GroupActionButton(
                                icon = Icons.Default.Call,
                                label = "Vocal",
                                onClick = {
                                    viewModel.startCall(
                                        name = currentChat.title,
                                        phone = currentChat.creatorPhone.ifBlank { "+221 77 000 11 22" },
                                        avatar = currentChat.avatarUrl,
                                        isVideo = false
                                    )
                                }
                            )

                            GroupActionButton(
                                icon = Icons.Default.Videocam,
                                label = "Vidéo",
                                onClick = {
                                    viewModel.startCall(
                                        name = currentChat.title,
                                        phone = currentChat.creatorPhone.ifBlank { "+221 77 000 11 22" },
                                        avatar = currentChat.avatarUrl,
                                        isVideo = true
                                    )
                                }
                            )

                            GroupActionButton(
                                icon = Icons.Default.PersonAdd,
                                label = "Ajouter",
                                onClick = { showAddMemberDialog = true },
                                testTag = "btn_group_add_participant"
                            )

                            GroupActionButton(
                                icon = Icons.Default.Search,
                                label = "Chercher",
                                onClick = { viewModel.navigateTo(AppScreen.CHAT_ROOM) }
                            )
                        }
                    }
                }
            }

            // Description and Info Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Description et règles du groupe",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = AdtPrimary
                        )
                        Text(
                            text = currentChat.description.ifBlank { "Aucune description renseignée pour ce groupe." },
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                "Chiffrement de bout en bout actif pour tous les membres",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Ephemeral Disappearing Messages Row for Group
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEphemeralDialog = true }
                                .padding(vertical = 4.dp)
                                .testTag("row_group_ephemeral_messages"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.HistoryToggleOff,
                                    contentDescription = null,
                                    tint = if (currentEphemeralDuration == EphemeralMessageDuration.OFF) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        AdtPrimary
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "Messages éphémères",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (currentEphemeralDuration == EphemeralMessageDuration.OFF) {
                                            "Désactivé (les messages sont conservés)"
                                        } else {
                                            "Actif • Autodestruction après ${currentEphemeralDuration.label}"
                                        },
                                        fontSize = 12.sp,
                                        color = if (currentEphemeralDuration == EphemeralMessageDuration.OFF) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            AdtPrimary
                                        }
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Members Header with count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${members.size} participants",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    TextButton(onClick = { showAddMemberDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = AdtPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter", color = AdtPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Add Member Quick Button Row
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { showAddMemberDialog = true }
                        .testTag("card_add_members"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AdtPrimary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                        Column {
                            Text("Ajouter des participants", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Inviter des contacts dans ce groupe", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Members List
            items(members, key = { it.id }) { member ->
                val isSelf = member.contactPhone == myPhone || member.name.contains("(Vous)") || member.name.contains("Vous")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { selectedMemberForAction = member },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AdtAvatar(name = member.name, sizeDp = 44)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isSelf) "${member.name} (Vous)" else member.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (member.isAdmin) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AdtPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "Admin",
                                                color = AdtPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = member.contactPhone.ifBlank { member.bio.ifBlank { "Membre ADT Pro" } },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Remove action or options icon
                        IconButton(onClick = { selectedMemberForAction = member }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options pour ${member.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Group Actions: Leave / Delete
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Quitter le groupe", color = CallRed, fontWeight = FontWeight.SemiBold) },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = CallRed) },
                            modifier = Modifier.clickable { showLeaveGroupConfirm = true }
                        )

                        if (isUserAdmin) {
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text("Supprimer le groupe", color = CallRed, fontWeight = FontWeight.SemiBold) },
                                leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CallRed) },
                                modifier = Modifier.clickable { showDeleteGroupConfirm = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet for Member actions (Direct message, Admin toggle, Remove)
    if (selectedMemberForAction != null) {
        val member = selectedMemberForAction!!
        val isSelf = member.contactPhone == myPhone || member.name.contains("(Vous)")

        ModalBottomSheet(onDismissRequest = { selectedMemberForAction = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdtAvatar(name = member.name, sizeDp = 48)
                    Column {
                        Text(member.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(member.contactPhone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                if (!isSelf) {
                    ListItem(
                        headlineContent = { Text("Envoyer un message direct") },
                        leadingContent = { Icon(Icons.Default.Chat, contentDescription = null, tint = AdtPrimary) },
                        modifier = Modifier.clickable {
                            selectedMemberForAction = null
                            val matchingContact = allContacts.find { it.phone == member.contactPhone }
                            if (matchingContact != null) {
                                viewModel.openDirectChatWithContact(matchingContact)
                            } else {
                                viewModel.openDirectChatWithContact(
                                    ContactEntity(
                                        id = member.contactPhone,
                                        name = member.name,
                                        phone = member.contactPhone,
                                        countryCode = "+221",
                                        avatarUrl = member.avatarUrl,
                                        bio = member.bio
                                    )
                                )
                            }
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Appel vocal") },
                        leadingContent = { Icon(Icons.Default.Call, contentDescription = null, tint = AdtPrimary) },
                        modifier = Modifier.clickable {
                            selectedMemberForAction = null
                            viewModel.startCall(
                                name = member.name,
                                phone = member.contactPhone,
                                avatar = member.avatarUrl,
                                isVideo = false
                            )
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Appel vidéo") },
                        leadingContent = { Icon(Icons.Default.Videocam, contentDescription = null, tint = AdtPrimary) },
                        modifier = Modifier.clickable {
                            selectedMemberForAction = null
                            viewModel.startCall(
                                name = member.name,
                                phone = member.contactPhone,
                                avatar = member.avatarUrl,
                                isVideo = true
                            )
                        }
                    )

                    // Admin toggle
                    ListItem(
                        headlineContent = {
                            Text(if (member.isAdmin) "Retirer des administrateurs" else "Nommer administrateur du groupe")
                        },
                        leadingContent = { Icon(Icons.Default.Security, contentDescription = null, tint = AdtSecondary) },
                        modifier = Modifier.clickable {
                            if (activeChatId != null) {
                                viewModel.toggleGroupAdmin(activeChatId!!, member.contactPhone, member.name, !member.isAdmin)
                            }
                            selectedMemberForAction = null
                        }
                    )

                    // Remove Member from group
                    ListItem(
                        headlineContent = { Text("Retirer du groupe", color = CallRed, fontWeight = FontWeight.SemiBold) },
                        leadingContent = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = CallRed) },
                        modifier = Modifier.clickable {
                            val target = member
                            selectedMemberForAction = null
                            memberToRemove = target
                        }
                    )
                } else {
                    ListItem(
                        headlineContent = { Text("Quitter le groupe", color = CallRed) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = CallRed) },
                        modifier = Modifier.clickable {
                            selectedMemberForAction = null
                            showLeaveGroupConfirm = true
                        }
                    )
                }
            }
        }
    }

    // Add Member Dialog
    if (showAddMemberDialog && activeChatId != null) {
        val existingPhones = members.map { it.contactPhone }.toSet()
        val availableContacts = allContacts.filter { !existingPhones.contains(it.phone) }

        AddGroupMembersDialog(
            availableContacts = availableContacts,
            onDismiss = { showAddMemberDialog = false },
            onAddConfirmed = { selectedList ->
                viewModel.addGroupMembers(activeChatId!!, selectedList)
                showAddMemberDialog = false
            }
        )
    }

    // Edit Group Info Dialog
    if (showEditInfoDialog && currentChat != null && activeChatId != null) {
        EditGroupInfoDialog(
            initialTitle = currentChat.title,
            initialDesc = currentChat.description,
            onDismiss = { showEditInfoDialog = false },
            onSave = { newTitle, newDesc ->
                viewModel.updateGroupInfo(activeChatId!!, newTitle, newDesc)
                showEditInfoDialog = false
            }
        )
    }

    // Confirm Remove Member Dialog
    if (memberToRemove != null && activeChatId != null) {
        val member = memberToRemove!!
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Retirer du groupe") },
            text = { Text("Êtes-vous sûr de vouloir retirer « ${member.name} » de ce groupe ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeGroupMember(activeChatId!!, member.contactPhone, member.name)
                        memberToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CallRed),
                    modifier = Modifier.testTag("btn_confirm_remove_member")
                ) {
                    Text("Retirer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Confirm Leave Group Dialog
    if (showLeaveGroupConfirm && activeChatId != null) {
        AlertDialog(
            onDismissRequest = { showLeaveGroupConfirm = false },
            title = { Text("Quitter le groupe") },
            text = { Text("Voulez-vous vraiment quitter ce groupe ? Vous ne recevrez plus de messages.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveGroupConfirm = false
                        viewModel.leaveGroup(activeChatId!!)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CallRed)
                ) {
                    Text("Quitter", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveGroupConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Confirm Delete Group Dialog
    if (showDeleteGroupConfirm && activeChatId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupConfirm = false },
            title = { Text("Supprimer le groupe") },
            text = { Text("Attention : cette action supprimera définitivement le groupe, tous les messages et ses membres.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteGroupConfirm = false
                        viewModel.deleteGroup(activeChatId!!)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CallRed)
                ) {
                    Text("Supprimer définitivement", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Ephemeral Disappearing Messages Dialog for Group
    if (showEphemeralDialog && activeChatId != null) {
        val chatId = activeChatId!!
        EphemeralMessageTimerDialog(
            currentDuration = currentEphemeralDuration,
            onDismiss = { showEphemeralDialog = false },
            onSelectDuration = { newDuration ->
                viewModel.setChatEphemeralDuration(chatId, newDuration)
                showEphemeralDialog = false
            }
        )
    }
}

@Composable
fun GroupActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .size(46.dp)
                .clickable { onClick() }
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = AdtPrimary, modifier = Modifier.size(22.dp))
            }
        }
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AddGroupMembersDialog(
    availableContacts: List<ContactEntity>,
    onDismiss: () -> Unit,
    onAddConfirmed: (List<ContactEntity>) -> Unit
) {
    val selectedContacts = remember { mutableStateListOf<ContactEntity>() }
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(searchQuery, availableContacts) {
        if (searchQuery.isBlank()) availableContacts
        else availableContacts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Ajouter des participants", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "${selectedContacts.size} contact(s) sélectionné(s)",
                    fontSize = 12.sp,
                    color = AdtPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un contact...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (availableContacts.isEmpty()) "Tous vos contacts sont déjà dans ce groupe !" else "Aucun contact correspondant trouvé.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredContacts, key = { it.phone }) { contact ->
                            val isSelected = selectedContacts.contains(contact)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedContacts.remove(contact)
                                        else selectedContacts.add(contact)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AdtAvatar(name = contact.name, sizeDp = 38)
                                    Column {
                                        Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                        Text(contact.phone, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) selectedContacts.remove(contact)
                                        else selectedContacts.add(contact)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = AdtPrimary)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddConfirmed(selectedContacts.toList()) },
                enabled = selectedContacts.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                modifier = Modifier.testTag("btn_confirm_add_members")
            ) {
                Text("Ajouter (${selectedContacts.size})", color = Color.White)
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
fun EditGroupInfoDialog(
    initialTitle: String,
    initialDesc: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var desc by remember { mutableStateOf(initialDesc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier les informations du groupe", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nom du groupe *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, desc) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
            ) {
                Text("Enregistrer", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
