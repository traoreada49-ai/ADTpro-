package com.example.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.WallpaperCategory
import com.example.data.model.WallpaperData
import com.example.data.model.WallpaperItem
import com.example.ui.components.ChatWallpaperBackground
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPickerScreen(viewModel: AdtViewModel) {
    val appSettings by viewModel.appSettings.collectAsState()
    val currentActiveTheme = appSettings?.wallpaperTheme ?: "EMERALD"

    var selectedCategory by remember { mutableStateOf(WallpaperCategory.ALL) }
    var previewItem by remember { mutableStateOf<WallpaperItem?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val filteredList = remember(selectedCategory) {
        if (selectedCategory == WallpaperCategory.ALL) {
            WallpaperData.allWallpapers
        } else {
            WallpaperData.allWallpapers.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fonds d'écran", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Personnalisez l'arrière-plan de vos discussions",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("btn_back_wallpaper")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Tabs Row
            ScrollableTabRow(
                selectedTabIndex = WallpaperCategory.entries.indexOf(selectedCategory),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = AdtPrimary,
                divider = { HorizontalDivider() }
            ) {
                WallpaperCategory.entries.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(category.iconEmoji, fontSize = 14.sp)
                                Text(
                                    category.displayName,
                                    fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }

            // Summary Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val activeItem = WallpaperData.findById(currentActiveTheme)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(activeItem.badgeEmoji, fontSize = 18.sp)
                        Column {
                            Text("Thème actuel :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(activeItem.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AdtPrimary)
                        }
                    }
                    Button(
                        onClick = { previewItem = activeItem },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aperçu", fontSize = 12.sp)
                    }
                }
            }

            // Wallpapers Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { item ->
                    val isSelected = currentActiveTheme == item.id
                    WallpaperCard(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            previewItem = item
                        },
                        onApply = {
                            viewModel.updateWallpaper(item.id)
                            snackbarMessage = "Fond d'écran « ${item.title} » appliqué à toutes les discussions !"
                        }
                    )
                }
            }
        }
    }

    // Fullscreen Preview Dialog
    previewItem?.let { item ->
        WallpaperPreviewDialog(
            item = item,
            isCurrent = currentActiveTheme == item.id,
            onDismiss = { previewItem = null },
            onApply = {
                viewModel.updateWallpaper(item.id)
                snackbarMessage = "Fond d'écran « ${item.title} » appliqué !"
                previewItem = null
            }
        )
    }
}

@Composable
fun WallpaperCard(
    item: WallpaperItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onApply: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, AdtPrimary, RoundedCornerShape(14.dp))
                else Modifier
            )
    ) {
        Column {
            // Preview Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Brush.verticalGradient(item.gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                // Background artistic accent
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(item.accentColor.copy(alpha = 0.18f), radius = size.width * 0.35f, center = Offset(size.width * 0.5f, size.height * 0.5f))
                }

                // Emoji Badge
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(item.badgeEmoji, fontSize = 28.sp)
                    }
                }

                // Active badge
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AdtPrimary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text("Actif", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Text info & button
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = if (isSelected) {
                        ButtonDefaults.outlinedButtonColors(contentColor = AdtPrimary)
                    } else {
                        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    }
                ) {
                    Text(if (isSelected) "Actuel" else "Appliquer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun WallpaperPreviewDialog(
    item: WallpaperItem,
    isCurrent: Boolean,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Live Chat Wallpaper background preview
                ChatWallpaperBackground(
                    wallpaperKey = item.id,
                    isDark = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top bar inside preview
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1F2C34).copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(item.badgeEmoji, fontSize = 22.sp)
                                    Column {
                                        Text(item.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                        Text(item.description, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    }
                                }
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                                }
                            }
                        }

                        // Mock Chat Messages
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            // Incoming bubble
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                                    color = Color(0xFF1F2C34),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Amina Traoré", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AdtPrimary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "Bonjour ! Que penses-tu du nouveau fond d'écran « ${item.title} » ?",
                                            color = Color.White,
                                            fontSize = 13.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("14:20", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.End))
                                    }
                                }
                            }

                            // Outgoing bubble
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                                    color = Color(0xFF005C4B),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Magnifique ! Le contraste est impeccable et le chiffrement reste sécurisé. ✨",
                                            color = Color.White,
                                            fontSize = 13.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.align(Alignment.End),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("14:21", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF53BDEB), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1F2C34).copy(alpha = 0.95f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onApply,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Wallpaper, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isCurrent) "Déjà défini comme fond d'écran" else "Définir comme fond d'écran",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Annuler", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
