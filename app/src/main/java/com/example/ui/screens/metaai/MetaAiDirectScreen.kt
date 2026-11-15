package com.example.ui.screens.metaai

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MetaAiMessage
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MetaAiUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Couleurs emblématiques du dégradé Meta AI
val MetaAiBlue = Color(0xFF0084FF)
val MetaAiPurple = Color(0xFF7928CA)
val MetaAiCyan = Color(0xFF00DFD8)
val MetaAiPink = Color(0xFFFF0080)

val MetaAiGradient = Brush.linearGradient(
    colors = listOf(MetaAiCyan, MetaAiBlue, MetaAiPurple, MetaAiPink)
)

/**
 * Écran officiel Meta AI Direct intégrant l'assistant Llama 3.3 et Gemini
 * avec génération d'images /imagine, réponses contextuelles, synthèse vocale et suggestions riches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaAiDirectScreen(viewModel: AdtViewModel) {
    BackHandler {
        viewModel.navigateTo(AppScreen.MAIN_TABS)
    }

    val messages by viewModel.metaAiMessages.collectAsState()
    val isThinking by viewModel.isMetaAiThinking.collectAsState()
    val metaAiUiState by viewModel.metaAiUiState.collectAsState()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Auto-scroll vers le dernier message
    LaunchedEffect(messages.size, isThinking, metaAiUiState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Avatar anneau iridescent animé Meta AI
                        MetaAiGlowingRing(size = 40.dp)

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Meta AI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Vérifié",
                                    tint = MetaAiBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = if (isThinking) "Meta AI est en train d'écrire..." else "Llama 3.3 & Gemini • En ligne",
                                fontSize = 11.5.sp,
                                color = if (isThinking) MetaAiBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.MAIN_TABS) },
                        modifier = Modifier.testTag("btn_meta_ai_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour aux discussions"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("btn_meta_ai_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Effacer l'historique"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Quick Action Chips (/imagine, Idée, Traduire)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MetaAiBlue.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MetaAiBlue.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clickable {
                                    textInput = "/imagine "
                                }
                                .testTag("chip_quick_imagine")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🎨", fontSize = 12.sp)
                                Text("/imagine", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MetaAiBlue)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    textInput = "Donne-moi 3 idées créatives pour "
                                }
                                .testTag("chip_quick_idea")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("💡", fontSize = 12.sp)
                                Text("Idée", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    textInput = "Traduis en Bambara et en Wolof : "
                                }
                                .testTag("chip_quick_translate")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🌍", fontSize = 12.sp)
                                Text("Traduire", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // Input Text Field & Send Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    "Demandez à Meta AI ou /imagine...",
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MetaAiBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MetaAiPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_meta_ai_prompt")
                        )

                        // Send Button with Meta AI Gradient
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier
                                .size(48.dp)
                                .background(MetaAiGradient, CircleShape)
                                .clickable(enabled = textInput.isNotBlank()) {
                                    val query = textInput
                                    textInput = ""
                                    viewModel.sendMetaAiMessage(query)
                                }
                                .testTag("btn_send_meta_ai")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Envoyer",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Suggestion Prompts Carousel at the top
            item {
                MetaAiSuggestedPromptsHeader(
                    onSelectPrompt = { prompt ->
                        viewModel.sendMetaAiMessage(prompt)
                    }
                )
            }

            // Message Bubbles
            items(messages) { msg ->
                MetaAiMessageBubble(
                    message = msg,
                    onPromptClick = { prompt -> viewModel.sendMetaAiMessage(prompt) },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(msg.text))
                        Toast.makeText(context, "Texte copié", Toast.LENGTH_SHORT).show()
                    },
                    onSpeak = {
                        viewModel.speakMessage(msg.id, msg.text)
                    },
                    onRegenerate = {
                        viewModel.regenerateLastMetaAiResponse()
                    }
                )
            }

            // Streaming / Loading / Error states from Firebase AI Logic
            when (val uiState = metaAiUiState) {
                is MetaAiUiState.Loading -> {
                    item {
                        MetaAiThinkingBubble()
                    }
                }
                is MetaAiUiState.Streaming -> {
                    item {
                        MetaAiMessageBubble(
                            message = MetaAiMessage(text = uiState.text, isUser = false),
                            onPromptClick = {},
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(uiState.text))
                                Toast.makeText(context, "Texte copié", Toast.LENGTH_SHORT).show()
                            },
                            onSpeak = {},
                            onRegenerate = {}
                        )
                    }
                }
                is MetaAiUiState.Error -> {
                    item {
                        MetaAiErrorBanner(
                            message = uiState.message,
                            onRetry = { viewModel.regenerateLastMetaAiResponse() }
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    // Confirmation Dialog to Clear Conversation
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Effacer la discussion Meta AI") },
            text = { Text("Voulez-vous réinitialiser l'historique de vos conversations avec Meta AI ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearMetaAiMessages()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Effacer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

/**
 * Avatar en anneau animé officiel avec le dégradé iridescent de Meta AI.
 */
@Composable
fun MetaAiGlowingRing(
    size: androidx.compose.ui.unit.Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MetaAiRing")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        // Glowing Outer Ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
                .background(MetaAiGradient, CircleShape)
        )
        // Inner Circle to create ring effect
        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MetaAiBlue,
                modifier = Modifier.size(size * 0.42f)
            )
        }
    }
}

/**
 * Carousel de suggestions de prompts au début de la conversation.
 */
@Composable
fun MetaAiSuggestedPromptsHeader(
    onSelectPrompt: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MetaAiPurple,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Suggestions rapides pour commencer :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val suggestions = listOf(
                "🎨 /imagine Coucher de soleil sur le fleuve Niger à Bamako",
                "💡 5 idées de startups rentables en Afrique de l'Ouest",
                "🌍 Traduis « Bienvenue chez nous » en Bambara et Wolof",
                "✍️ Rédige un message professionnel de remerciement",
                "⚡ Explique la cryptographie quantique simplement"
            )

            suggestions.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPrompt(prompt) }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bulle de message Meta AI avec support de /imagine (images générées), TTS, copie et actions.
 */
@Composable
fun MetaAiMessageBubble(
    message: MetaAiMessage,
    onPromptClick: (String) -> Unit,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.isUser
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
            ) {
                MetaAiGlowingRing(size = 18.dp)
                Text(
                    text = "Meta AI",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MetaAiBlue
                )
                Text(
                    text = "• $timeFormatted",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) {
                AdtPrimary
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            },
            border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) else null,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // If this is an AI Image generation (/imagine)
                if (message.isImageGeneration && !message.imageUrl.isNullOrBlank()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MetaAiPurple.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "🎨 META AI • /imagine GENERATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MetaAiPurple,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Generated Image Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = message.imagePrompt,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text(
                            text = message.text,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    // Standard Text Message
                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Action Bar for AI response (Listen aloud, Copy, Regenerate)
                if (!isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Écouter la réponse vocale",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copier",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Régénérer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Suggested Follow-up Prompts
        if (!isUser && message.suggestedPrompts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                items(message.suggestedPrompts) { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MetaAiBlue.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, MetaAiBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.clickable { onPromptClick(suggestion) }
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 11.sp,
                            color = MetaAiBlue,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bulle d'attente animée indiquant que Meta AI réfléchit.
 */
@Composable
fun MetaAiThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingDots")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotScale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.widthIn(max = 240.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetaAiGlowingRing(size = 20.dp)
            Text(
                text = "Meta AI réfléchit...",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MetaAiBlue
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .scale(dotScale)
                        .background(MetaAiBlue, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .scale(dotScale * 0.9f)
                        .background(MetaAiPurple, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .scale(dotScale * 0.8f)
                        .background(MetaAiPink, CircleShape)
                )
            }
        }
    }
}

@Composable
fun MetaAiErrorBanner(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meta_ai_error_banner")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Erreur Firebase AI",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onRetry,
                modifier = Modifier.testTag("btn_meta_ai_retry")
            ) {
                Text("Réessayer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
