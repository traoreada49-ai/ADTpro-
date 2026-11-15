package com.example.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firebase.StickerData
import com.example.data.firebase.StickerSyncStatus
import com.example.ui.theme.AdtPrimary
import com.example.ui.theme.AdtSecondary

val DEFAULT_STICKERS = listOf(
    // BUSINESS & PRO
    StickerData("b1", "🤝", "Accord Validé", "BUSINESS", listOf("accord", "partenaire", "deal", "contrat", "ok")),
    StickerData("b2", "💼", "Pro & Affaires", "BUSINESS", listOf("business", "travail", "bureau", "pro")),
    StickerData("b3", "✍️", "Contrat Signé", "BUSINESS", listOf("signature", "contrat", "papier", "deal")),
    StickerData("b4", "💳", "Paiement Reçu", "BUSINESS", listOf("argent", "paiement", "facture", "banque")),
    StickerData("b5", "📊", "Rapport Prêt", "BUSINESS", listOf("statistiques", "rapport", "données", "chiffres")),
    StickerData("b6", "🚀", "Projet Lancé", "BUSINESS", listOf("fusée", "lancement", "vitesse", "succès")),
    StickerData("b7", "🎯", "Objectif Atteint", "BUSINESS", listOf("cible", "goal", "succès", "gagné")),
    StickerData("b8", "⏱️", "Urgent ADT", "BUSINESS", listOf("urgent", "rapide", "chronomètre", "vite")),
    StickerData("b9", "🏆", "Performance Top", "BUSINESS", listOf("champion", "trophée", "premier", "victoire")),
    StickerData("b10", "🏛️", "Direction ADT", "BUSINESS", listOf("direction", "bureau", "siège", "conseil")),
    StickerData("b11", "📈", "En Croissance", "BUSINESS", listOf("hausse", "profit", "croissance", "vente")),
    StickerData("b12", "👥", "Réunion Équipe", "BUSINESS", listOf("meeting", "équipe", "groupe", "call")),

    // TERANGA & AFRO
    StickerData("t1", "🦁", "Lion du Sénégal", "TERANGA", listOf("lion", "force", "gainde", "teranga", "dakar")),
    StickerData("t2", "🇸🇳", "Dakar Connect", "TERANGA", listOf("senegal", "dakar", "pays", "afrique")),
    StickerData("t3", "🇨🇮", "Abidjan Business", "TERANGA", listOf("ci", "cote d'ivoire", "abidjan", "business")),
    StickerData("t4", "🇲🇱", "Bamako Solutions", "TERANGA", listOf("mali", "bamako", "solutions")),
    StickerData("t5", "🇬🇳", "Conakry Hub", "TERANGA", listOf("guinee", "conakry", "hub")),
    StickerData("t6", "☕", "Pause Ataya", "TERANGA", listOf("thé", "ataya", "pause", "detente")),
    StickerData("t7", "✨", "Nio Far", "TERANGA", listOf("ensemble", "solidarite", "union", "teranga")),
    StickerData("t8", "🙏", "Jërëjëf", "TERANGA", listOf("merci", "gratitude", "remerciement")),
    StickerData("t9", "👑", "Patron / Boss", "TERANGA", listOf("roi", "boss", "directeur", "respect")),
    StickerData("t10", "🍲", "Thiéboudienne", "TERANGA", listOf("repas", "midi", "bon appétit", "manger")),
    StickerData("t11", "💚", "Teranga ADT", "TERANGA", listOf("coeur", "amour", "bienvenue", "teranga")),
    StickerData("t12", "🌍", "Afrique Émergente", "TERANGA", listOf("afrique", "monde", "continent", "futur")),

    // TECH & TELECOM
    StickerData("tc1", "🔐", "Chiffré E2E", "TECH", listOf("securite", "chiffre", "cadenas", "prive")),
    StickerData("tc2", "📡", "Réseau 5G Pro", "TECH", listOf("antenne", "telecom", "5g", "signal", "connexion")),
    StickerData("tc3", "⚡", "Fibre Optique", "TECH", listOf("rapide", "vitesse", "eclair", "fibre")),
    StickerData("tc4", "🛡️", "Sécurité Max", "TECH", listOf("bouclier", "protection", "antivirus", "safe")),
    StickerData("tc5", "🤖", "Assistant AI", "TECH", listOf("gemini", "ia", "robot", "smart")),
    StickerData("tc6", "💻", "Dev en Cours", "TECH", listOf("code", "ordinateur", "programmeur", "dev")),
    StickerData("tc7", "☁️", "Cloud Synchro", "TECH", listOf("nuage", "sauvegarde", "cloud", "sync")),
    StickerData("tc8", "🔑", "Clé Publique OK", "TECH", listOf("cle", "authentification", "pass")),
    StickerData("tc9", "📶", "Plein Réseau", "TECH", listOf("wifi", "barres", "signal", "connexion")),
    StickerData("tc10", "🛰️", "Liaison Satellite", "TECH", listOf("satellite", "espace", "liaison")),
    StickerData("tc11", "💾", "Sauvegardé", "TECH", listOf("disquette", "save", "fichier")),
    StickerData("tc12", "🔋", "Batterie 100%", "TECH", listOf("energie", "charge", "full", "pret")),

    // REACTIONS & FUN
    StickerData("r1", "🎉", "Félicitations !", "REACTIONS", listOf("fete", "bravo", "succes", "joie")),
    StickerData("r2", "🔥", "C'est du lourd !", "REACTIONS", listOf("feu", "flamme", "incroyable", "hot")),
    StickerData("r3", "👏", "Bravo l'équipe", "REACTIONS", listOf("applaudissements", "bravo", "merci")),
    StickerData("r4", "💡", "Idée Géniale", "REACTIONS", listOf("ampoule", "astuce", "trouve", "eureka")),
    StickerData("r5", "😎", "Géré avec classe", "REACTIONS", listOf("cool", "lunettes", "easy", "parfait")),
    StickerData("r6", "💯", "100% Validé", "REACTIONS", listOf("100", "top", "exact", "parfait")),
    StickerData("r7", "💪", "Force & Courage", "REACTIONS", listOf("muscle", "puissance", "energie", "combat")),
    StickerData("r8", "🥳", "Célébration", "REACTIONS", listOf("party", "joie", "fete", "danse")),
    StickerData("r9", "🙌", "Merci Infiniment", "REACTIONS", listOf("mains", "respect", "remerciement")),
    StickerData("r10", "⭐", "Excellence", "REACTIONS", listOf("etoile", "parfait", "brillant")),
    StickerData("r11", "🤩", "Impressionnant", "REACTIONS", listOf("yeux etoiles", "wahou", "super")),
    StickerData("r12", "☕", "Pause Café", "REACTIONS", listOf("cafe", "detente", "pause", "midi"))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomStickerPicker(
    onStickerSelected: (emoji: String, title: String, imageUrl: String?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    cloudStickers: List<StickerData> = emptyList(),
    syncStatus: StickerSyncStatus = StickerSyncStatus.Idle,
    onSyncCloudStickers: () -> Unit = {},
    onCreateAndUploadSticker: (emoji: String, title: String, category: String, pickedUri: Uri?) -> Unit = { _, _, _, _ -> },
    onDeleteCloudSticker: (StickerData) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("BUSINESS") }
    var searchQuery by remember { mutableStateOf("") }
    var recentStickers by remember {
        mutableStateOf(
            listOf(
                StickerData("b1", "🤝", "Accord Validé", "BUSINESS"),
                StickerData("tc1", "🔐", "Chiffré E2E", "TECH"),
                StickerData("t1", "🦁", "Lion du Sénégal", "TERANGA"),
                StickerData("r2", "🔥", "C'est du lourd !", "REACTIONS")
            )
        )
    }

    // Trigger initial Firebase Storage sync when opening picker
    LaunchedEffect(Unit) {
        onSyncCloudStickers()
    }

    // Custom Creator State
    var customEmojiInput by remember { mutableStateOf("🌟") }
    var customTitleInput by remember { mutableStateOf("Super Partenaire") }
    var customCategoryInput by remember { mutableStateOf("BUSINESS") }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }

    val categories = listOf(
        Triple("RECENTS", "🕒", "Récents"),
        Triple("CLOUD", "☁️", "Cloud (${cloudStickers.size})"),
        Triple("BUSINESS", "💼", "Business"),
        Triple("TERANGA", "🦁", "Teranga"),
        Triple("TECH", "⚡", "Tech & Réseau"),
        Triple("REACTIONS", "🥳", "Réactions"),
        Triple("CUSTOM", "🎨", "Créer")
    )

    val allLibraryStickers = remember(cloudStickers) {
        cloudStickers + DEFAULT_STICKERS
    }

    val filteredStickers = remember(selectedCategory, searchQuery, recentStickers, allLibraryStickers, cloudStickers) {
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            allLibraryStickers.filter {
                it.title.lowercase().contains(query) ||
                    it.emoji.contains(query) ||
                    it.category.lowercase().contains(query) ||
                    it.tags.any { tag -> tag.contains(query) }
            }
        } else {
            when (selectedCategory) {
                "RECENTS" -> (cloudStickers.take(4) + recentStickers).distinctBy { it.id }
                "CLOUD" -> cloudStickers
                "CUSTOM" -> emptyList()
                else -> allLibraryStickers.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(430.dp)
            .testTag("custom_sticker_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }

            // Header: Title + Firebase Storage Sync button + Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AdtPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column {
                        Text(
                            text = "Stickers & Assets Firebase Storage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (syncStatus) {
                                StickerSyncStatus.Syncing -> "Synchronisation Firebase Storage..."
                                is StickerSyncStatus.Uploading -> "Envoi PNG vers Firebase (${syncStatus.progressPercent}%)..."
                                is StickerSyncStatus.Success -> syncStatus.message
                                is StickerSyncStatus.Error -> syncStatus.message
                                StickerSyncStatus.Idle -> "${cloudStickers.size} asset(s) cloud • ${DEFAULT_STICKERS.size} intégrés"
                            },
                            fontSize = 10.5.sp,
                            color = when (syncStatus) {
                                is StickerSyncStatus.Error -> MaterialTheme.colorScheme.error
                                is StickerSyncStatus.Success -> AdtPrimary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSyncCloudStickers,
                        enabled = syncStatus !is StickerSyncStatus.Syncing && syncStatus !is StickerSyncStatus.Uploading,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("btn_sync_cloud_stickers")
                    ) {
                        if (syncStatus is StickerSyncStatus.Syncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AdtPrimary)
                        } else {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = "Synchroniser les stickers Firebase Storage",
                                tint = AdtPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fermer le sélecteur de stickers",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (syncStatus is StickerSyncStatus.Uploading) {
                LinearProgressIndicator(
                    progress = { (syncStatus.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    color = AdtPrimary
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher sticker, catégorie ou tag...", fontSize = 12.5.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = AdtPrimary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdtPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sticker_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Tabs Row
            if (searchQuery.isBlank()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    items(categories) { (catKey, catEmoji, catLabel) ->
                        val isSelected = selectedCategory == catKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = catKey },
                            label = {
                                Text(
                                    text = "$catEmoji $catLabel",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AdtPrimary.copy(alpha = 0.18f),
                                selectedLabelColor = AdtPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                selectedBorderColor = AdtPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("sticker_tab_$catKey")
                        )
                    }
                }
            } else {
                Text(
                    text = "Résultats de recherche (${filteredStickers.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AdtPrimary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Main Content: Grid or Custom Firebase Storage Creator
            if (selectedCategory == "CUSTOM" && searchQuery.isBlank()) {
                CustomStickerCreatorView(
                    customEmoji = customEmojiInput,
                    customTitle = customTitleInput,
                    selectedCategory = customCategoryInput,
                    pickedImageUri = pickedImageUri,
                    isUploading = syncStatus is StickerSyncStatus.Uploading,
                    onEmojiChange = { customEmojiInput = it },
                    onTitleChange = { customTitleInput = it },
                    onCategoryChange = { customCategoryInput = it },
                    onImagePicked = { pickedImageUri = it },
                    onUploadAndSend = { emoji, title, category, uri ->
                        onCreateAndUploadSticker(emoji, title, category, uri)
                        pickedImageUri = null
                    }
                )
            } else if (filteredStickers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(if (selectedCategory == "CLOUD") "☁️" else "🔍", fontSize = 34.sp)
                        Text(
                            text = when {
                                searchQuery.isNotEmpty() -> "Aucun autocollant ne correspond à « $searchQuery »"
                                selectedCategory == "CLOUD" -> "Aucun sticker personnalisé sur Firebase Storage pour l'instant."
                                else -> "Aucun autocollant dans cette catégorie"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (selectedCategory == "CLOUD") {
                            FilledTonalButton(
                                onClick = { selectedCategory = "CUSTOM" },
                                modifier = Modifier.testTag("btn_open_sticker_creator")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Créer un sticker cloud", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("sticker_grid"),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStickers, key = { it.id }) { sticker ->
                        StickerCardItem(
                            sticker = sticker,
                            onClick = {
                                recentStickers = listOf(sticker) + recentStickers.filter { it.id != sticker.id }
                                onStickerSelected(sticker.emoji, sticker.title, sticker.imageUrl)
                            },
                            onDelete = if (sticker.imageUrl != null) {
                                { onDeleteCloudSticker(sticker) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StickerCardItem(
    sticker: StickerData,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clickable { onClick() }
            .testTag("sticker_card_${sticker.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = if (sticker.isCloudAsset) {
            BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.45f))
        } else {
            CardDefaults.outlinedCardBorder(enabled = true)
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!sticker.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = sticker.imageUrl,
                        contentDescription = sticker.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Text(
                        text = sticker.emoji,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = sticker.title,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            if (sticker.isCloudAsset || !sticker.imageUrl.isNullOrBlank()) {
                Icon(
                    imageVector = if (sticker.isCloudAsset) Icons.Default.CloudDone else Icons.Default.Image,
                    contentDescription = if (sticker.isCloudAsset) "Asset Firebase Storage" else "Asset local",
                    tint = AdtPrimary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(13.dp)
                )
            }

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .testTag("btn_delete_sticker_${sticker.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Supprimer le sticker",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomStickerCreatorView(
    customEmoji: String,
    customTitle: String,
    selectedCategory: String,
    pickedImageUri: Uri?,
    isUploading: Boolean,
    onEmojiChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onImagePicked: (Uri?) -> Unit,
    onUploadAndSend: (emoji: String, title: String, category: String, pickedUri: Uri?) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImagePicked(uri)
        }
    }

    val targetCategories = listOf(
        "BUSINESS" to "💼 Business",
        "TERANGA" to "🦁 Teranga",
        "TECH" to "⚡ Tech",
        "REACTIONS" to "🥳 Réactions",
        "CLOUD" to "☁️ Cloud"
    )

    val suggestedEmojis = listOf(
        "🌟", "🤝", "🚀", "🦁", "💼", "🔥", "💡", "⚡", "👑", "🎯",
        "🎉", "🏆", "☕", "🇸🇳", "🇨🇮", "🇲🇱", "🇬🇳", "🔐", "📶", "💚"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Live Preview + Photo Picker Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(AdtPrimary.copy(alpha = 0.12f), AdtSecondary.copy(alpha = 0.12f))
                    ),
                    RoundedCornerShape(14.dp)
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                modifier = Modifier.size(66.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (pickedImageUri != null) {
                        AsyncImage(
                            model = pickedImageUri,
                            contentDescription = "Aperçu image importée",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(customEmoji.ifBlank { "⭐" }, fontSize = 32.sp)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Créer & héberger sur Firebase Storage",
                    fontSize = 11.sp,
                    color = AdtPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = customTitle.ifBlank { "Sticker personnalisé" },
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_pick_sticker_image")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (pickedImageUri != null) "Changer image" else "Choisir image", fontSize = 11.sp)
                    }
                    if (pickedImageUri != null) {
                        IconButton(
                            onClick = { onImagePicked(null) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Retirer image", modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }

        // Category assignment selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(targetCategories) { (key, label) ->
                FilterChip(
                    selected = selectedCategory == key,
                    onClick = { onCategoryChange(key) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.testTag("creator_category_$key")
                )
            }
        }

        // Emoji Selector when no external image is picked
        if (pickedImageUri == null) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(suggestedEmojis) { em ->
                    Surface(
                        shape = CircleShape,
                        color = if (customEmoji == em) AdtPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onEmojiChange(em) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(em, fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        // Title + Upload & Send Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customTitle,
                onValueChange = onTitleChange,
                placeholder = { Text("Nom du sticker...", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("custom_sticker_title_input")
            )

            Button(
                onClick = {
                    onUploadAndSend(
                        customEmoji.ifBlank { "🌟" },
                        customTitle.ifBlank { "Sticker ADT" },
                        selectedCategory,
                        pickedImageUri
                    )
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("btn_send_custom_sticker")
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sauver & Envoyer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        }
    }
}
