package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.MessageEntity
import com.example.ui.theme.AdtPrimary
import com.example.ui.theme.AdtSecondary
import com.example.ui.theme.DarkBg
import kotlinx.coroutines.launch

/**
 * Standard list of quick reactions, identical to top messaging apps (WhatsApp, Telegram, Signal).
 */
val QUICK_REACTION_EMOJIS = listOf(
    "👍", // Pouce levé
    "❤️", // Cœur rouge
    "😂", // Rire aux larmes
    "😮", // Surpris
    "😢", // Triste
    "🙏", // Prière / Merci
    "🔥"  // Feu / Top
)

/**
 * Categorized Emojis for extended picker
 */
data class EmojiCategory(
    val title: String,
    val iconEmoji: String,
    val emojis: List<String>
)

val EMOJI_CATEGORIES = listOf(
    EmojiCategory(
        title = "Populaires",
        iconEmoji = "⭐",
        emojis = listOf(
            "👍", "❤️", "😂", "😮", "😢", "🙏", "🔥", "👏", "🎉", "🚀",
            "💯", "🤝", "✨", "🤩", "😎", "💪", "💡", "🎯", "👌", "🙌"
        )
    ),
    EmojiCategory(
        title = "Sourires",
        iconEmoji = "😃",
        emojis = listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃",
            "😉", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😙",
            "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔",
            "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "😮‍💨",
            "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢",
            "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "🥸",
            "😎", "🤓", "🧐", "😕", "😟", "🙁", "☹️", "😮", "😯", "😲",
            "😳", "🥺", "😦", "😧", "😨", "😰", "😥", "😢", "😭", "😱"
        )
    ),
    EmojiCategory(
        title = "Gestes",
        iconEmoji = "👍",
        emojis = listOf(
            "👋", "🤚", "🖐️", "✋", "🖖", "🫱", "🫲", "🫳", "🫴", "👌",
            "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉",
            "👆", "🖕", "👇", "☝️", "🫵", "👍", "👎", "✊", "👊", "🤛",
            "🤜", "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍️", "💅",
            "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃", "🫀"
        )
    ),
    EmojiCategory(
        title = "Cœurs & Symboles",
        iconEmoji = "❤️",
        emojis = listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
            "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝",
            "💟", "✨", "⭐", "🌟", "💫", "⚡", "💥", "🔥", "💯", "🎉",
            "🎊", "🎈", "🎁", "🏆", "🥇", "🥈", "🥉", "👑", "💎", "🎯"
        )
    ),
    EmojiCategory(
        title = "Objets & Pro",
        iconEmoji = "💼",
        emojis = listOf(
            "💼", "📱", "💻", "🖥️", "🖨️", "⌨️", "🖱️", "📞", "☎️", "📡",
            "💡", "🔦", "🔋", "🔌", "🔒", "🔓", "🔑", "🗝️", "🔨", "🛠️",
            "🛡️", "⚙️", "📎", "📌", "📍", "📝", "✏️", "📊", "📈", "📉",
            "✉️", "📧", "📦", "📫", "🏷️", "💳", "💰", "🧾", "⏰", "⏱️"
        )
    )
)

/**
 * Floating WhatsApp-style Quick Reaction Pill.
 * Shows emojis with bouncy animations and quick toggle on active reaction.
 */
@Composable
fun QuickReactionFloatingBar(
    currentReaction: String?,
    onSelectEmoji: (String) -> Unit,
    onOpenExtendedPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 8.dp,
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier.testTag("quick_reaction_floating_bar")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            QUICK_REACTION_EMOJIS.forEach { emoji ->
                val isSelected = currentReaction == emoji
                ReactionEmojiButton(
                    emoji = emoji,
                    isSelected = isSelected,
                    onClick = { onSelectEmoji(emoji) }
                )
            }

            // Plus button to open full emoji library
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .clickable { onOpenExtendedPicker() }
                    .testTag("reaction_open_picker_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Plus d'émojis",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Single emoji button in the reaction bar with animated scale on press
 */
@Composable
private fun ReactionEmojiButton(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.35f else if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "emojiScale"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) AdtPrimary.copy(alpha = 0.18f) else Color.Transparent
            )
            .then(
                if (isSelected) Modifier.border(1.5.dp, AdtPrimary, CircleShape) else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.scale(scale)
        )
    }
}

/**
 * Badge displayed on a message bubble showing the reaction emoji.
 * Overlaps slightly the bottom corner of the message bubble (WhatsApp style).
 */
@Composable
fun MessageReactionBadge(
    reaction: String,
    isOutgoing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(0f) }

    LaunchedEffect(reaction) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
            .scale(scaleAnim.value)
            .clickable { onClick() }
            .testTag("message_reaction_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = reaction,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Heart pop burst animation shown on double-tap of message bubble
 */
@Composable
fun DoubleTapHeartBurst(
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.2f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(tween(150)),
        exit = scaleOut(
            targetScale = 1.6f,
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(250))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "❤️",
                        fontSize = 38.sp
                    )
                }
            }
        }
    }
}

/**
 * Extended Categorized Emoji Picker Bottom Sheet.
 * Allows choosing from a full library of emojis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtendedEmojiPickerSheet(
    currentReaction: String?,
    onDismiss: () -> Unit,
    onSelectEmoji: (String) -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choisir une réaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }

            // Quick Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filtrer les émojis...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdtPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("emoji_search_input")
            )

            // Category Tab Row (only shown when not searching)
            if (searchQuery.isBlank()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    edgePadding = 0.dp,
                    divider = {},
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    EMOJI_CATEGORIES.forEachIndexed { index, category ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(category.iconEmoji, fontSize = 16.sp)
                                    Text(category.title, fontSize = 12.5.sp, fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                    }
                }
            }

            // Filtered or Categorized List of Emojis
            val displayEmojis = remember(searchQuery, selectedCategoryIndex) {
                if (searchQuery.isNotBlank()) {
                    val all = EMOJI_CATEGORIES.flatMap { it.emojis }.distinct()
                    // Filter matching emojis
                    all.filter { it.contains(searchQuery.trim()) }
                } else {
                    EMOJI_CATEGORIES.getOrNull(selectedCategoryIndex)?.emojis ?: emptyList()
                }
            }

            // Current Reaction Info and removal option
            if (currentReaction != null) {
                Surface(
                    color = AdtPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(currentReaction, fontSize = 22.sp)
                            Text("Votre réaction actuelle", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        Text(
                            text = "Supprimer",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelectEmoji(currentReaction) // Toggles off
                                    onDismiss()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Emojis Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 44.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .testTag("emoji_picker_grid")
            ) {
                items(displayEmojis) { emoji ->
                    val isSelected = currentReaction == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) AdtPrimary.copy(alpha = 0.2f) else Color.Transparent
                            )
                            .then(
                                if (isSelected) Modifier.border(1.5.dp, AdtPrimary, RoundedCornerShape(10.dp)) else Modifier
                            )
                            .clickable {
                                onSelectEmoji(emoji)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
