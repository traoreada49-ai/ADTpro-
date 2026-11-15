package com.example.ui.components

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
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.ContactEntity
import com.example.data.model.DurationOption
import com.example.data.model.LocationDurationPresets
import com.example.data.model.SampleLocationPoint
import com.example.data.model.SampleLocations
import com.example.ui.theme.AdtPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLiveLocationDialog(
    allContacts: List<ContactEntity>,
    preselectedContactPhoneOrChatId: String? = null,
    onDismiss: () -> Unit,
    onConfirmShare: (
        selectedContacts: List<String>,
        durationMinutes: Int,
        comment: String,
        selectedLocation: SampleLocationPoint
    ) -> Unit
) {
    // Selected contacts IDs (phone numbers or chat IDs)
    val initialSelected = remember(preselectedContactPhoneOrChatId) {
        if (!preselectedContactPhoneOrChatId.isNullOrBlank()) {
            val matchedContact = allContacts.find {
                it.phone == preselectedContactPhoneOrChatId ||
                        "chat_" + it.phone.filter { char -> char.isDigit() } == preselectedContactPhoneOrChatId
            }
            if (matchedContact != null) listOf(matchedContact.phone)
            else listOf(preselectedContactPhoneOrChatId)
        } else {
            // Default select first contact if none preselected
            allContacts.take(1).map { it.phone }
        }
    }
    var selectedContactPhones by remember { mutableStateOf(initialSelected.toSet()) }
    var selectedDuration by remember { mutableStateOf(LocationDurationPresets.options[1]) } // 1 heure default
    var commentText by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf(SampleLocations.presets.first()) }
    var contactSearchQuery by remember { mutableStateOf("") }
    var showLocationSelector by remember { mutableStateOf(false) }

    val filteredContacts = remember(allContacts, contactSearchQuery) {
        if (contactSearchQuery.isBlank()) allContacts
        else allContacts.filter {
            it.name.contains(contactSearchQuery, ignoreCase = true) ||
                    it.phone.contains(contactSearchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Partager ma localisation en direct",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                "GPS haute précision • Chiffrement E2EE",
                                fontSize = 11.5.sp,
                                color = AdtPrimary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${selectedContactPhones.size} contact(s) sélectionné(s)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Durée : ${selectedDuration.label}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    if (selectedContactPhones.isNotEmpty()) {
                                        onConfirmShare(
                                            selectedContactPhones.toList(),
                                            selectedDuration.minutes,
                                            commentText.trim(),
                                            selectedLocation
                                        )
                                    }
                                },
                                enabled = selectedContactPhones.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.testTag("btn_confirm_share_live_location")
                            ) {
                                Icon(Icons.Default.ShareLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Partager en direct", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Live Map Preview Card
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            ) {
                                MapGraphicCanvas(
                                    latitude = selectedLocation.latitude,
                                    longitude = selectedLocation.longitude,
                                    isLive = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Centered Pin with pulse
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RadarPulseRings()
                                    LocationMarkerPin(senderName = "Moi", isLive = true)
                                }
                                // Change location badge
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .clickable { showLocationSelector = !showLocationSelector }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.EditLocation, contentDescription = null, modifier = Modifier.size(14.dp), tint = AdtPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Modifier lieu", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AdtPrimary)
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = selectedLocation.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = selectedLocation.address,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Lat: %.4f, Lng: %.4f • Précision GPS ±5m".format(selectedLocation.latitude, selectedLocation.longitude),
                                    fontSize = 10.5.sp,
                                    color = AdtPrimary
                                )
                            }
                        }
                    }
                }

                // Location Presets Dropdown / Selector if expanded
                if (showLocationSelector) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Choisir un point GPS de départ :", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                                SampleLocations.presets.forEach { preset ->
                                    val isSelected = selectedLocation == preset
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedLocation = preset
                                                showLocationSelector = false
                                            }
                                            .background(
                                                if (isSelected) AdtPrimary.copy(alpha = 0.1f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(preset.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(preset.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Duration Selector (15 minutes, 1 heure, 8 heures)
                item {
                    Text(
                        text = "Durée du partage en temps réel :",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LocationDurationPresets.options.forEach { option ->
                            val isSelected = selectedDuration == option
                            DurationCard(
                                option = option,
                                isSelected = isSelected,
                                onClick = { selectedDuration = option },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Optional Comment / Message
                item {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Ajouter un message (ex: En route pour le bureau...)") },
                        leadingIcon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = AdtPrimary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_location_comment"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // 4. Multi-Contact Selector
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Partager avec (${selectedContactPhones.size} sélectionné(s)) :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        TextButton(
                            onClick = {
                                selectedContactPhones = if (selectedContactPhones.size == allContacts.size) {
                                    emptySet()
                                } else {
                                    allContacts.map { it.phone }.toSet()
                                }
                            }
                        ) {
                            Text(
                                if (selectedContactPhones.size == allContacts.size) "Tout désélectionner" else "Tout sélectionner",
                                fontSize = 12.sp,
                                color = AdtPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Contact Search Field
                    OutlinedTextField(
                        value = contactSearchQuery,
                        onValueChange = { contactSearchQuery = it },
                        placeholder = { Text("Rechercher un contact...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (contactSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { contactSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_location_contacts"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Selected Contact Avatars Chips
                if (selectedContactPhones.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selectedItems = allContacts.filter { selectedContactPhones.contains(it.phone) }
                            items(selectedItems) { contact ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = AdtPrimary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        AdtAvatar(name = contact.name, sizeDp = 22)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(contact.name.split(" ").firstOrNull() ?: contact.name, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Retirer",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    selectedContactPhones = selectedContactPhones - contact.phone
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Contact List with Checkboxes
                items(filteredContacts) { contact ->
                    val isChecked = selectedContactPhones.contains(contact.phone)
                    ContactSelectionRow(
                        contact = contact,
                        isChecked = isChecked,
                        onToggle = {
                            selectedContactPhones = if (isChecked) {
                                selectedContactPhones - contact.phone
                            } else {
                                selectedContactPhones + contact.phone
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun DurationCard(
    option: DurationOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AdtPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) AdtPrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .clickable { onClick() }
            .testTag("btn_duration_${option.badge}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = option.label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.5.sp,
                color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ContactSelectionRow(
    contact: ContactEntity,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isChecked) AdtPrimary.copy(alpha = 0.08f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("contact_item_${contact.phone}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                AdtAvatar(name = contact.name, sizeDp = 40, isOnline = contact.isOnline)
                Column {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = contact.phone,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = AdtPrimary)
            )
        }
    }
}
