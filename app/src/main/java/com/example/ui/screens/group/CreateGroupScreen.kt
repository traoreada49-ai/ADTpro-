package com.example.ui.screens.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ContactEntity
import com.example.ui.components.AdtAvatar
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(viewModel: AdtViewModel) {
    val contacts by viewModel.allContacts.collectAsState()
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var groupCategory by remember { mutableStateOf("Entreprise & Partenaires") }
    var contactSearchQuery by remember { mutableStateOf("") }
    val selectedContacts = remember { mutableStateListOf<ContactEntity>() }

    val categories = listOf(
        "Entreprise & Partenaires",
        "Équipe Projet Télécom",
        "Commerce & Vente",
        "Réseau ADT Sahel & Europe",
        "Famille & Amis"
    )

    val filteredContacts = remember(contactSearchQuery, contacts) {
        if (contactSearchQuery.isBlank()) contacts
        else contacts.filter { it.name.contains(contactSearchQuery, ignoreCase = true) || it.phone.contains(contactSearchQuery) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nouveau groupe", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${selectedContacts.size} participant(s) sélectionné(s)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.MAIN_TABS) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (groupName.isNotBlank() && selectedContacts.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.createGroup(
                            name = groupName,
                            selected = selectedContacts.toList(),
                            category = groupCategory,
                            desc = groupDesc.ifBlank { "Groupe d'échange professionnel" }
                        )
                    },
                    containerColor = AdtPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("btn_confirm_create_group")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Créer le groupe (${selectedContacts.size + 1} membres)")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            item {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Nom du groupe *") },
                    placeholder = { Text("Ex: Équipe Orange & Moov Projets") },
                    leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, tint = AdtPrimary) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_name"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = groupDesc,
                    onValueChange = { groupDesc = it },
                    label = { Text("Description du groupe") },
                    placeholder = { Text("Objectifs, thématiques et règles du groupe...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Text("Catégorie :", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = groupCategory == cat,
                            onClick = { groupCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Selected Contacts Chips Row
            if (selectedContacts.isNotEmpty()) {
                item {
                    Text("Membres ajoutés :", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AdtPrimary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        items(selectedContacts, key = { it.phone }) { contact ->
                            InputChip(
                                selected = true,
                                onClick = { selectedContacts.remove(contact) },
                                label = { Text(contact.name, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Retirer",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                OutlinedTextField(
                    value = contactSearchQuery,
                    onValueChange = { contactSearchQuery = it },
                    placeholder = { Text("Rechercher parmi les contacts...", fontSize = 13.5.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        AdtAvatar(name = contact.name, sizeDp = 42)
                        Column {
                            Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(contact.bio.ifBlank { contact.phone }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
