package com.example.ui.screens.contacts

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ContactEntity
import com.example.data.model.CountryData
import com.example.data.model.CountryItem
import com.example.ui.components.AdtAvatar
import com.example.ui.components.PhoneWithDialCodeInput
import com.example.ui.theme.AdtAccentGreen
import com.example.ui.theme.AdtPrimary
import com.example.util.ContactManager
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(viewModel: AdtViewModel) {
    val context = LocalContext.current
    val contacts by viewModel.allContacts.collectAsState()
    val isSyncing by viewModel.isSyncingContacts.collectAsState()
    val syncMessage by viewModel.syncResultMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchContact by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(0) } // 0: Tous, 1: ADT Pro, 2: À Inviter
    var showAddContactDialog by remember { mutableStateOf(false) }

    // State for Add Contact dialog
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }
    var newContactCountry by remember { mutableStateOf(CountryData.supportedCountries.first()) }
    var newContactDialCode by remember { mutableStateOf(CountryData.supportedCountries.first().dialCode.removePrefix("+")) }

    // Permission launcher for READ_CONTACTS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Whether granted or denied, ContactManager will scan device ContentResolver or fallback cleanly
        viewModel.syncContactsWithDevice()
    }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

    val adtUsers = remember(contacts) { contacts.filter { it.isAdtUser } }
    val inviteUsers = remember(contacts) { contacts.filter { !it.isAdtUser } }

    val filteredContacts = remember(contacts, searchContact, selectedFilter) {
        val baseList = when (selectedFilter) {
            1 -> adtUsers
            2 -> inviteUsers
            else -> contacts
        }
        val query = searchContact.trim()
        if (query.isBlank()) {
            baseList
        } else {
            val queryDigits = query.filter { it.isDigit() || it == '+' }
            baseList.filter { contact ->
                val nameMatches = contact.name.contains(query, ignoreCase = true)
                val directPhoneMatches = contact.phone.contains(query, ignoreCase = true)
                val digitsMatches = queryDigits.length >= 2 && contact.phone.filter { it.isDigit() || it == '+' }.contains(queryDigits)
                nameMatches || directPhoneMatches || digitsMatches
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nouvelle discussion",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${adtUsers.size} sur ADT Pro • ${contacts.size} contacts au total",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.MAIN_TABS) },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openContactQrCode(initialTab = 0) },
                        modifier = Modifier.testTag("btn_top_qr_code")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Code QR Dynamique & Scanner",
                            tint = AdtPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openSyncContacts() },
                        modifier = Modifier.testTag("btn_open_contacts_sync_page")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Inviter des amis",
                            tint = AdtPrimary
                        )
                    }
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 12.dp),
                            strokeWidth = 2.5.dp,
                            color = AdtPrimary
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (ContactManager.hasContactsPermission(context)) {
                                    viewModel.syncContactsWithDevice()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                            modifier = Modifier.testTag("sync_contacts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Synchroniser les contacts",
                                tint = AdtPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openContactQrCode(initialTab = 1) },
                containerColor = AdtAccentGreen,
                contentColor = Color.White,
                icon = {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Scanner QR Code Contact",
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        "Scanner QR Ami",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                },
                modifier = Modifier.testTag("fab_contacts_scan_qr")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pinned Top Search Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchContact,
                        onValueChange = { searchContact = it },
                        placeholder = {
                            Text("Rechercher par nom ou numéro...", fontSize = 14.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = AdtPrimary)
                        },
                        trailingIcon = {
                            if (searchContact.isNotEmpty()) {
                                IconButton(onClick = { searchContact = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Effacer la recherche")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdtPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_contacts_input")
                    )

                    // Live Filter Badge & Clear
                    if (searchContact.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredContacts.size} contact(s) trouvé(s) pour \"$searchContact\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AdtPrimary
                            )
                            Text(
                                text = "Effacer",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { searchContact = "" }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

            // QR Code Quick Access Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AdtPrimary.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AdtPrimary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Partage Dynamique par QR Code",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Échange de contact instantané & chiffré",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { viewModel.openContactQrCode(initialTab = 0) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_contacts_my_qr")
                            ) {
                                Text("Mon QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.openContactQrCode(initialTab = 1) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                                modifier = Modifier.testTag("btn_contacts_scan_qr")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Sync Banner Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AdtPrimary.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AdtPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ContactPhone,
                                        contentDescription = null,
                                        tint = AdtPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Gestionnaire de Contacts (ContentResolver)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Compare le carnet d'adresses avec l'annuaire ADT Pro",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.openSyncContacts() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_banner_invite_friends")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Inviter", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    if (ContactManager.hasContactsPermission(context)) {
                                        viewModel.syncContactsWithDevice()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    }
                                },
                                enabled = !isSyncing,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                            ) {
                                Text(
                                    text = if (isSyncing) "Sync..." else "Scanner",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == 0,
                        onClick = { selectedFilter = 0 },
                        label = { Text("Tous (${contacts.size})", fontSize = 12.sp) },
                        modifier = Modifier.testTag("filter_all_contacts")
                    )
                    FilterChip(
                        selected = selectedFilter == 1,
                        onClick = { selectedFilter = 1 },
                        label = { Text("Sur ADT Pro (${adtUsers.size})", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AdtAccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("filter_adt_users")
                    )
                    FilterChip(
                        selected = selectedFilter == 2,
                        onClick = { selectedFilter = 2 },
                        label = { Text("À inviter (${inviteUsers.size})", fontSize = 12.sp) },
                        modifier = Modifier.testTag("filter_invite_users")
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Quick actions (Nouveau groupe, Nouvelle chaîne)
            if (searchContact.isEmpty() && selectedFilter != 2) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                newContactName = ""
                                newContactPhone = ""
                                showAddContactDialog = true
                            }
                            .padding(vertical = 8.dp)
                            .testTag("action_add_contact_dialog"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AdtPrimary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                            }
                        }
                        Column {
                            Text("Nouveau contact", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Ajouter avec indicatif pays à part (+221, +225...)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(AppScreen.CREATE_GROUP) }
                            .padding(vertical = 8.dp)
                            .testTag("action_create_group"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AdtPrimary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color.White)
                            }
                        }
                        Column {
                            Text("Nouveau groupe", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Discutez en équipe chiffrée de bout en bout", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(AppScreen.CREATE_CHANNEL) }
                            .padding(vertical = 8.dp)
                            .testTag("action_create_channel"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AdtPrimary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White)
                            }
                        }
                        Column {
                            Text("Nouvelle chaîne de diffusion", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Publiez des actualités à un nombre illimité d'abonnés", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // Header for contacts
            item {
                Text(
                    text = when (selectedFilter) {
                        1 -> "MEMBRES INSCRITS SUR ADT PRO"
                        2 -> "CONTACTS À INVITER PAR SMS"
                        else -> "CONTACTS DU TÉLÉPHONE & ADT PRO"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Contact Items
            items(filteredContacts, key = { it.id }) { contact ->
                ContactListItem(
                    contact = contact,
                    onOpenChat = { viewModel.openDirectChatWithContact(contact) },
                    onStartVoiceCall = { viewModel.startCall(contact.name, contact.phone, contact.avatarUrl, false) },
                    onStartVideoCall = { viewModel.startCall(contact.name, contact.phone, contact.avatarUrl, true) },
                    onInvite = { viewModel.inviteContact(context, contact) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }

            // Empty Search State
            if (filteredContacts.isEmpty() && searchContact.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Aucun contact trouvé",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Aucun résultat ne correspond à \"$searchContact\" (nom ou numéro de téléphone).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    newContactName = if (searchContact.none { it.isDigit() }) searchContact else ""
                                    newContactPhone = searchContact.filter { it.isDigit() }
                                    showAddContactDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                                modifier = Modifier.testTag("btn_add_unmatched_contact")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ajouter ce contact", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

    // Modal dialog to add a new contact with separate dial code
    if (showAddContactDialog) {
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = AdtPrimary)
                    Text("Nouveau Contact", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Saisissez le nom et le numéro de téléphone avec l'indicatif pays à part :",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Nom du contact") },
                        placeholder = { Text("Ex: Amadou Diallo") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_contact_name")
                    )

                    // Phone input with separate country dial code
                    PhoneWithDialCodeInput(
                        selectedCountry = newContactCountry,
                        enteredDialCode = newContactDialCode,
                        phoneNumber = newContactPhone,
                        onCountrySelected = { newContactCountry = it },
                        onDialCodeChanged = { newContactDialCode = it },
                        onPhoneNumberChanged = { newContactPhone = it },
                        label = "Numéro de téléphone",
                        placeholder = newContactCountry.exampleNumber
                    )
                }
            },
            confirmButton = {
                val fullPhone = if (newContactPhone.isNotBlank()) {
                    val rawCode = if (newContactDialCode.isNotBlank()) newContactDialCode.trim().removePrefix("+") else newContactCountry.dialCode.removePrefix("+")
                    val code = "+$rawCode"
                    var digits = newContactPhone.trim().replace(" ", "").replace("-", "")
                    if (digits.startsWith(rawCode)) {
                        digits = digits.removePrefix(rawCode)
                    }
                    "$code$digits"
                } else ""

                Button(
                    onClick = {
                        if (fullPhone.isNotBlank()) {
                            viewModel.addNewContact(
                                name = newContactName.ifBlank { fullPhone },
                                phone = fullPhone,
                                countryCode = newContactCountry.dialCode
                            ) { success ->
                                if (success) {
                                    android.widget.Toast.makeText(context, "Contact ajouté avec succès", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            showAddContactDialog = false
                        }
                    },
                    enabled = newContactPhone.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                    modifier = Modifier.testTag("btn_confirm_add_new_contact")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun ContactListItem(
    contact: ContactEntity,
    onOpenChat: () -> Unit,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onInvite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (contact.isAdtUser) {
                    onOpenChat()
                } else {
                    onInvite()
                }
            }
            .padding(vertical = 6.dp)
            .testTag("contact_item_${contact.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            AdtAvatar(name = contact.name, sizeDp = 44, isOnline = contact.isOnline)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (contact.isAdtUser) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AdtAccentGreen.copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = "ADT",
                                color = AdtAccentGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (contact.isAdtUser) contact.bio else contact.phone,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        if (contact.isAdtUser) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onStartVoiceCall,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Appel vocal",
                        tint = AdtPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(
                    onClick = onStartVideoCall,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = "Appel vidéo",
                        tint = AdtPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = onInvite,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AdtPrimary),
                modifier = Modifier.testTag("invite_button_${contact.id}")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Inviter", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
