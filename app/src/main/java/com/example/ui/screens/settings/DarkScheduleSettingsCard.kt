package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AppSettingsEntity
import com.example.ui.theme.AdtPrimary
import com.example.ui.theme.AdtPrimaryDark
import com.example.ui.util.TranslationStrings
import com.example.util.ThemeScheduleManager
import kotlinx.coroutines.delay

/**
 * Composant de configuration et contrôle du mode sombre automatique selon l'heure
 * ou la planification définie par l'utilisateur.
 */
@Composable
fun DarkScheduleSettingsCard(
    appSettings: AppSettingsEntity?,
    strings: TranslationStrings,
    onUpdateThemeMode: (String) -> Unit,
    onUpdateSchedule: (type: String, startH: Int, startM: Int, endH: Int, endM: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentThemeMode = appSettings?.themeMode ?: ThemeScheduleManager.MODE_DARK
    val scheduleType = appSettings?.darkScheduleType ?: ThemeScheduleManager.SCHEDULE_SUNSET_TO_SUNRISE
    val startHour = appSettings?.darkScheduleStartHour ?: ThemeScheduleManager.DEFAULT_SUNSET_HOUR
    val startMinute = appSettings?.darkScheduleStartMinute ?: ThemeScheduleManager.DEFAULT_SUNSET_MINUTE
    val endHour = appSettings?.darkScheduleEndHour ?: ThemeScheduleManager.DEFAULT_SUNRISE_HOUR
    val endMinute = appSettings?.darkScheduleEndMinute ?: ThemeScheduleManager.DEFAULT_SUNRISE_MINUTE

    // Rafraîchissement périodique de l'état en direct
    var timeTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(currentThemeMode) {
        if (currentThemeMode == ThemeScheduleManager.MODE_AUTO_SCHEDULE) {
            while (true) {
                delay(15_000L) // toutes les 15 secondes
                timeTick = System.currentTimeMillis()
            }
        }
    }

    val scheduleInfo = remember(scheduleType, startHour, startMinute, endHour, endMinute, timeTick) {
        ThemeScheduleManager.computeTransitionInfo(
            scheduleType = scheduleType,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute
        )
    }

    var editingTimeDialogType by remember { mutableStateOf<TimeDialogType?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_dark_schedule_settings"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // En-tête : Thème d'affichage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AdtPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (currentThemeMode) {
                            ThemeScheduleManager.MODE_LIGHT -> Icons.Default.LightMode
                            ThemeScheduleManager.MODE_DARK -> Icons.Default.DarkMode
                            ThemeScheduleManager.MODE_SYSTEM -> Icons.Default.Smartphone
                            else -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = AdtPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Thème & Mode Nuit Automatique",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = when (currentThemeMode) {
                            ThemeScheduleManager.MODE_DARK -> "Mode Sombre actif permanent"
                            ThemeScheduleManager.MODE_LIGHT -> "Mode Clair actif permanent"
                            ThemeScheduleManager.MODE_SYSTEM -> "Suit les paramètres système"
                            ThemeScheduleManager.MODE_AUTO_SCHEDULE -> "Bascule automatique selon l'heure"
                            else -> "Mode Sombre"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Sélecteur des 4 modes de thème
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Mode Sombre
            ThemeModeChip(
                label = strings.darkMode,
                icon = Icons.Default.DarkMode,
                isSelected = currentThemeMode == ThemeScheduleManager.MODE_DARK,
                onClick = { onUpdateThemeMode(ThemeScheduleManager.MODE_DARK) },
                modifier = Modifier.weight(1f).testTag("chip_theme_dark")
            )

            // Mode Clair
            ThemeModeChip(
                label = strings.lightMode,
                icon = Icons.Default.LightMode,
                isSelected = currentThemeMode == ThemeScheduleManager.MODE_LIGHT,
                onClick = { onUpdateThemeMode(ThemeScheduleManager.MODE_LIGHT) },
                modifier = Modifier.weight(1f).testTag("chip_theme_light")
            )

            // Système
            ThemeModeChip(
                label = "Système",
                icon = Icons.Default.Smartphone,
                isSelected = currentThemeMode == ThemeScheduleManager.MODE_SYSTEM,
                onClick = { onUpdateThemeMode(ThemeScheduleManager.MODE_SYSTEM) },
                modifier = Modifier.weight(1f).testTag("chip_theme_system")
            )

            // Auto Horaire
            ThemeModeChip(
                label = "Auto Nuit",
                icon = Icons.Default.Schedule,
                isSelected = currentThemeMode == ThemeScheduleManager.MODE_AUTO_SCHEDULE,
                onClick = { onUpdateThemeMode(ThemeScheduleManager.MODE_AUTO_SCHEDULE) },
                modifier = Modifier.weight(1f).testTag("chip_theme_auto_schedule")
            )
        }

        // Section dépliable de programmation horaire si "Auto Nuit" est sélectionné
        AnimatedVisibility(
            visible = currentThemeMode == ThemeScheduleManager.MODE_AUTO_SCHEDULE,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Indicateur d'état en direct
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (scheduleInfo.isCurrentlyDark) AdtPrimaryDark.copy(alpha = 0.25f) else Color(0xFFFFB300).copy(alpha = 0.18f),
                        border = BorderStroke(
                            1.dp,
                            if (scheduleInfo.isCurrentlyDark) AdtPrimary.copy(alpha = 0.5f) else Color(0xFFFFB300).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (scheduleInfo.isCurrentlyDark) Icons.Default.NightlightRound else Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = if (scheduleInfo.isCurrentlyDark) AdtPrimary else Color(0xFFFF9800),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scheduleInfo.statusSummary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = scheduleInfo.nextActionSummary,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Sélection du mode de programmation (Crépuscule vs Personnalisé)
                    Text(
                        text = "Mode de planification :",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = scheduleType == ThemeScheduleManager.SCHEDULE_SUNSET_TO_SUNRISE,
                            onClick = {
                                onUpdateSchedule(
                                    ThemeScheduleManager.SCHEDULE_SUNSET_TO_SUNRISE,
                                    ThemeScheduleManager.DEFAULT_SUNSET_HOUR,
                                    ThemeScheduleManager.DEFAULT_SUNSET_MINUTE,
                                    ThemeScheduleManager.DEFAULT_SUNRISE_HOUR,
                                    ThemeScheduleManager.DEFAULT_SUNRISE_MINUTE
                                )
                            },
                            label = {
                                Text("Crépuscule à l'aube (20h - 07h)", fontSize = 11.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.WbTwilight, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f).testTag("chip_schedule_sunset")
                        )

                        FilterChip(
                            selected = scheduleType == ThemeScheduleManager.SCHEDULE_CUSTOM,
                            onClick = {
                                onUpdateSchedule(
                                    ThemeScheduleManager.SCHEDULE_CUSTOM,
                                    startHour,
                                    startMinute,
                                    endHour,
                                    endMinute
                                )
                            },
                            label = {
                                Text("Horaires sur-mesure", fontSize = 11.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f).testTag("chip_schedule_custom")
                        )
                    }

                    // Configuration des heures personnalisées
                    if (scheduleType == ThemeScheduleManager.SCHEDULE_CUSTOM) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Plage horaire nocturne personnalisée :",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Boîte Heure de Début (Mode Sombre)
                                TimePickerBox(
                                    title = "Activer (Sombre)",
                                    timeFormatted = ThemeScheduleManager.formatTime(startHour, startMinute),
                                    icon = Icons.Default.DarkMode,
                                    onClick = { editingTimeDialogType = TimeDialogType.START },
                                    modifier = Modifier.weight(1f).testTag("btn_pick_start_time")
                                )

                                // Boîte Heure de Fin (Mode Clair)
                                TimePickerBox(
                                    title = "Désactiver (Clair)",
                                    timeFormatted = ThemeScheduleManager.formatTime(endHour, endMinute),
                                    icon = Icons.Default.LightMode,
                                    onClick = { editingTimeDialogType = TimeDialogType.END },
                                    modifier = Modifier.weight(1f).testTag("btn_pick_end_time")
                                )
                            }

                            // Raccourcis / Presets rapides
                            Text(
                                text = "Raccourcis d'horaires fréquents :",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(ThemeScheduleManager.PRESET_SCHEDULES) { preset ->
                                    val isCurrentPreset = startHour == preset.startHour &&
                                            startMinute == preset.startMinute &&
                                            endHour == preset.endHour &&
                                            endMinute == preset.endMinute

                                    AssistChip(
                                        onClick = {
                                            onUpdateSchedule(
                                                ThemeScheduleManager.SCHEDULE_CUSTOM,
                                                preset.startHour,
                                                preset.startMinute,
                                                preset.endHour,
                                                preset.endMinute
                                            )
                                        },
                                        label = {
                                            Text(preset.label, fontSize = 11.sp, fontWeight = if (isCurrentPreset) FontWeight.Bold else FontWeight.Normal)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isCurrentPreset) Icons.Default.Check else Icons.Default.AccessTime,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isCurrentPreset) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (isCurrentPreset) AdtPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = if (isCurrentPreset) BorderStroke(1.dp, AdtPrimary) else null
                                    )
                                }
                            }
                        }
                    } else {
                        // Explication du mode Crépuscule à l'aube
                        Text(
                            text = "Le mode sombre s'active automatiquement à 20h00 chaque soir pour réduire la fatigue oculaire et repasse en mode clair à 07h00 au réveil.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Dialogue de réglage d'heure (Start ou End)
    if (editingTimeDialogType != null) {
        val isStart = editingTimeDialogType == TimeDialogType.START
        val initialHour = if (isStart) startHour else endHour
        val initialMinute = if (isStart) startMinute else endMinute

        CustomTimePickerDialog(
            title = if (isStart) "Heure d'activation (Mode Sombre)" else "Heure de désactivation (Mode Clair)",
            initialHour = initialHour,
            initialMinute = initialMinute,
            onDismiss = { editingTimeDialogType = null },
            onConfirm = { h, m ->
                if (isStart) {
                    onUpdateSchedule(ThemeScheduleManager.SCHEDULE_CUSTOM, h, m, endHour, endMinute)
                } else {
                    onUpdateSchedule(ThemeScheduleManager.SCHEDULE_CUSTOM, startHour, startMinute, h, m)
                }
                editingTimeDialogType = null
            }
        )
    }
}

private enum class TimeDialogType {
    START, END
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) AdtPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.dp,
            if (isSelected) AdtPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = modifier.height(58.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TimePickerBox(
    title: String,
    timeFormatted: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeFormatted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdtPrimary
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AdtPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Dialogue élégant et réactif de réglage d'heure et minute
 */
@Composable
private fun CustomTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Affichage grand format de l'heure sélectionnée
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AdtPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = ThemeScheduleManager.formatTime(selectedHour, selectedMinute),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AdtPrimary,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                // Ajusteurs d'heure (+/-)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Colonne Heure
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Heures", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { selectedHour = if (selectedHour <= 0) 23 else selectedHour - 1 },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-1 heure")
                            }
                            Text(
                                text = "%02d".format(selectedHour),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = { selectedHour = if (selectedHour >= 23) 0 else selectedHour + 1 },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+1 heure")
                            }
                        }
                    }

                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                    // Colonne Minute
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minutes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { selectedMinute = if (selectedMinute <= 0) 55 else (selectedMinute - 5) / 5 * 5 },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-5 min")
                            }
                            Text(
                                text = "%02d".format(selectedMinute),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = { selectedMinute = if (selectedMinute >= 55) 0 else (selectedMinute + 5) / 5 * 5 },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+5 min")
                            }
                        }
                    }
                }

                // Puces rapides de minutes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(0, 15, 30, 45).forEach { m ->
                        FilterChip(
                            selected = selectedMinute == m,
                            onClick = { selectedMinute = m },
                            label = { Text(":%02d".format(m), fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedHour, selectedMinute) },
                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
            ) {
                Text("Valider", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
