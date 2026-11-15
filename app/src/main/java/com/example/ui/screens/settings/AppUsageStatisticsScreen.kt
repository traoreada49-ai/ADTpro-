package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUsageStatistics
import com.example.data.model.DailyUsageStat
import com.example.data.model.DataUsageCategory
import com.example.data.model.MediaNetworkUsageDetail
import com.example.data.model.UsagePeriod
import com.example.ui.theme.AdtAccentGreen
import com.example.ui.theme.AdtPrimary
import com.example.ui.theme.AdtSecondary
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

private val StatScreenTimeColor = Color(0xFF34B7F1)
private val StatMessagesColor = Color(0xFF00A884)
private val StatDataColor = Color(0xFFFF9800)
private val StatCallsColor = Color(0xFF9C27B0)

/**
 * Écran complet de statistiques d'utilisation : temps d'écran, messages échangés et consommation de données.
 * Intègre des graphiques Canvas natifs réactifs (histogrammes, courbes interactives et diagrammes de flux)
 * reproduisant fidèlement le style et la fluidité des graphiques Recharts / D3.js.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageStatisticsScreen(viewModel: AdtViewModel) {
    val stats by viewModel.appUsageStats.collectAsState()
    val selectedPeriod by viewModel.selectedUsagePeriod.collectAsState()
    val isLoading by viewModel.isUsageStatsLoading.collectAsState()

    var activeMetricTab by remember { mutableStateOf(0) } // 0: Temps d'écran, 1: Messages, 2: Données
    var useAreaChart by remember { mutableStateOf(false) } // Barres vs Courbe d'aire
    var hoveredDayIndex by remember { mutableStateOf<Int?>(null) }
    var selectedMediaSliceIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler {
        viewModel.navigateTo(AppScreen.SETTINGS)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshUsageStatistics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Statistiques d'utilisation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Temps d'écran, messages & données consommées",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("btn_back_usage_stats")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour aux paramètres"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshUsageStatistics() },
                        modifier = Modifier.testTag("btn_refresh_usage_stats")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualiser les statistiques",
                            tint = AdtPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("usage_statistics_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sélecteur de période (7 jours, 30 jours, Global)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UsagePeriod.values().forEach { period ->
                        val isSelected = selectedPeriod == period
                        val chipTag = when (period) {
                            UsagePeriod.DAY_7 -> "chip_period_7d"
                            UsagePeriod.DAY_30 -> "chip_period_30d"
                            UsagePeriod.ALL_TIME -> "chip_period_all"
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setUsagePeriod(period) }
                                .testTag(chipTag)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = period.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Cartes KPI de résumé rapide (3 métriques clés)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // KPI 1: Temps d'écran
                    SummaryKpiCard(
                        title = "Temps passé",
                        value = stats.formattedTotalScreenTime,
                        subtitle = "Moy. ${stats.formattedAverageDailyScreenTime}/j",
                        icon = Icons.Default.Schedule,
                        iconTint = StatScreenTimeColor,
                        isSelected = activeMetricTab == 0,
                        onClick = { activeMetricTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_card_screen_time")
                    )

                    // KPI 2: Messages
                    SummaryKpiCard(
                        title = "Messages",
                        value = "${stats.totalMessages}",
                        subtitle = "${stats.totalMessagesSent} env. • ${stats.totalMessagesReceived} reç.",
                        icon = Icons.Default.Chat,
                        iconTint = StatMessagesColor,
                        isSelected = activeMetricTab == 1,
                        onClick = { activeMetricTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_card_messages")
                    )

                    // KPI 3: Données
                    SummaryKpiCard(
                        title = "Données",
                        value = stats.formattedTotalData,
                        subtitle = "${stats.formattedDataSent} ↑ • ${stats.formattedDataReceived} ↓",
                        icon = Icons.Default.DataUsage,
                        iconTint = StatDataColor,
                        isSelected = activeMetricTab == 2,
                        onClick = { activeMetricTab = 2 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_card_data")
                    )
                }
            }

            // Grand Graphique Interactif (Style Recharts / D3.js avec barres dynamiques ou courbe spline)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("interactive_chart_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when (activeMetricTab) {
                                        0 -> "Évolution du temps passé sur ADT Pro"
                                        1 -> "Volume quotidien des messages échangés"
                                        else -> "Trafic réseau quotidien (Envois + Réceptions)"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Touchez une barre pour inspecter le détail journalier",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Switch Barres vs Courbe d'aire (Recharts AreaChart / BarChart)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(modifier = Modifier.padding(2.dp)) {
                                        IconButton(
                                            onClick = { useAreaChart = false },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (!useAreaChart) AdtPrimary.copy(alpha = 0.16f) else Color.Transparent)
                                                .testTag("btn_chart_mode_bar")
                                        ) {
                                            Icon(
                                                Icons.Default.BarChart,
                                                contentDescription = "Graphique en barres",
                                                tint = if (!useAreaChart) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { useAreaChart = true },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (useAreaChart) AdtPrimary.copy(alpha = 0.16f) else Color.Transparent)
                                                .testTag("btn_chart_mode_area")
                                        ) {
                                            Icon(
                                                Icons.Default.ShowChart,
                                                contentDescription = "Graphique en courbe d'aire",
                                                tint = if (useAreaChart) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (activeMetricTab) {
                                        0 -> StatScreenTimeColor.copy(alpha = 0.15f)
                                        1 -> StatMessagesColor.copy(alpha = 0.15f)
                                        else -> StatDataColor.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = when (activeMetricTab) {
                                            0 -> "Minutes"
                                            1 -> "Messages"
                                            else -> "Données"
                                        },
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (activeMetricTab) {
                                            0 -> StatScreenTimeColor
                                            1 -> StatMessagesColor
                                            else -> StatDataColor
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Composant Graphique Canvas (BarChart ou Area/SplineChart)
                        if (useAreaChart) {
                            InteractiveUsageAreaChart(
                                dailyStats = stats.dailyStats,
                                metricTab = activeMetricTab,
                                hoveredIndex = hoveredDayIndex,
                                onSelectDay = { hoveredDayIndex = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(195.dp)
                                    .testTag("usage_area_chart")
                            )
                        } else {
                            InteractiveUsageBarChart(
                                dailyStats = stats.dailyStats,
                                metricTab = activeMetricTab,
                                hoveredIndex = hoveredDayIndex,
                                onSelectDay = { hoveredDayIndex = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(195.dp)
                                    .testTag("usage_bar_chart")
                            )
                        }

                        // Info-bulle / Tooltip interactif quand un jour est sélectionné
                        val selectedDay = hoveredDayIndex?.let { idx -> stats.dailyStats.getOrNull(idx) }
                        AnimatedVisibility(
                            visible = selectedDay != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            if (selectedDay != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${selectedDay.dayLabel} (${selectedDay.fullDateLabel})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = AdtPrimary
                                            )
                                            Text(
                                                text = "Temps d'écran : ${selectedDay.formattedScreenTime}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${selectedDay.totalMessages} msgs (${selectedDay.messagesSent} env. / ${selectedDay.messagesReceived} reç.)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Données : ${selectedDay.formattedData}",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Répartition des Données par Catégorie (Donut / Barre de progression segmentée D3-style)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("data_breakdown_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.PieChart, contentDescription = null, tint = StatDataColor)
                                Text(
                                    text = "Répartition de la bande passante",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Text(
                                text = stats.formattedTotalData,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AdtPrimary
                            )
                        }

                        // Diagramme Donut Interactif par Type de Média + Jauge Segmentée
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            MediaDataDonutChart(
                                categories = stats.dataCategories,
                                selectedIndex = selectedMediaSliceIndex,
                                totalFormatted = stats.formattedTotalData,
                                onSelectSlice = { idx ->
                                    selectedMediaSliceIndex = if (selectedMediaSliceIndex == idx) null else idx
                                },
                                modifier = Modifier
                                    .size(138.dp)
                                    .testTag("media_data_donut_chart")
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val focused = selectedMediaSliceIndex?.let { stats.dataCategories.getOrNull(it) }
                                if (focused != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(focused.colorHex).copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, Color(focused.colorHex).copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = focused.category,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(focused.colorHex)
                                            )
                                            Text(
                                                text = "${focused.formattedBytes} (${String.format(java.util.Locale.US, "%.1f%%", focused.percentage)})",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Consommation par média",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Touchez une ligne ci-dessous ou un segment pour isoler son poids sur votre forfait.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        // Jauge segmentée multicolore proportionnelle
                        SegmentedDataUsageBar(
                            categories = stats.dataCategories,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )

                        // Liste interactive des catégories avec pourcentage et volume
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            stats.dataCategories.forEachIndexed { idx, cat ->
                                val isHighlighted = selectedMediaSliceIndex == idx
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isHighlighted) Color(cat.colorHex).copy(alpha = 0.12f) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedMediaSliceIndex = if (isHighlighted) null else idx
                                        }
                                        .testTag("legend_media_category_$idx")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(Color(cat.colorHex), CircleShape)
                                            )
                                            Text(
                                                text = cat.category,
                                                fontSize = 13.sp,
                                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = String.format(java.util.Locale.US, "%.1f%%", cat.percentage),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isHighlighted) Color(cat.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = cat.formattedBytes,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 'Utilisation Réseau' Détaillée par Type de Média (Messages, Photos, Vidéos, Appels)
            item {
                NetworkUsageDetailsCard(
                    mediaDetails = stats.mediaNetworkDetails,
                    totalDataSent = stats.formattedDataSent,
                    totalDataReceived = stats.formattedDataReceived,
                    totalDataSaved = stats.formattedDataSaved,
                    onManageDataThrottles = { viewModel.navigateTo(AppScreen.SETTINGS) }
                )
            }

            // Faits marquants et Insights d'utilisation (Heure de pointe, Discussion favorite, Appels)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Aperçu de vos habitudes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AdtPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InsightMiniCard(
                                icon = Icons.Default.AccessTime,
                                title = "Pic d'activité",
                                value = "${stats.peakActivityHour}h00 - ${stats.peakActivityHour + 1}h00",
                                subtitle = "Heure la plus connectée",
                                modifier = Modifier.weight(1f)
                            )

                            InsightMiniCard(
                                icon = Icons.Default.Forum,
                                title = "Discussion phare",
                                value = stats.mostActiveChatTitle,
                                subtitle = "${stats.mostActiveChatMessages} messages échangés",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InsightMiniCard(
                                icon = Icons.Default.PhoneInTalk,
                                title = "Temps d'appels",
                                value = "${stats.callsDurationMinutes} min",
                                subtitle = "Audio & Vidéo E2EE",
                                modifier = Modifier.weight(1f)
                            )

                            InsightMiniCard(
                                icon = Icons.Default.Security,
                                title = "Chiffrement",
                                value = "100%",
                                subtitle = "Toutes sessions Signal E2EE",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Bouton de navigation vers gestion détaillée du stockage
            item {
                Button(
                    onClick = { viewModel.navigateTo(AppScreen.STORAGE_MANAGEMENT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_goto_storage_management"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gérer l'espace disque & vider le cache", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Carte KPI stylisée pour un indicateur clé.
 */
@Composable
fun SummaryKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) iconTint.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) iconTint else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Graphique en barres interactif avec rendu Canvas sur mesure inspiré de D3.js et Recharts.
 * Prend en charge l'animation des barres, l'ombrage dynamique, les axes et l'état sélectionné.
 */
@Composable
fun InteractiveUsageBarChart(
    dailyStats: List<DailyUsageStat>,
    metricTab: Int,
    hoveredIndex: Int?,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = when (metricTab) {
        0 -> StatScreenTimeColor
        1 -> StatMessagesColor
        else -> StatDataColor
    }

    if (dailyStats.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Aucune donnée pour cette période", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        return
    }

    // Calcul des valeurs maximales pour l'échelle
    val values = remember(dailyStats, metricTab) {
        dailyStats.map { stat ->
            when (metricTab) {
                0 -> stat.screenTimeMinutes.toFloat()
                1 -> stat.totalMessages.toFloat()
                else -> (stat.totalDataBytes / (1024f * 1024f)) // en Mo
            }
        }
    }
    val maxValue = remember(values) {
        (values.maxOrNull() ?: 10f).coerceAtLeast(10f)
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val count = dailyStats.size
                if (count == 0) return@Canvas

                val barWidth = (canvasWidth / count) * 0.55f
                val spacing = canvasWidth / count

                // Lignes de repère horizontales (grille D3 style)
                val gridLines = 3
                for (g in 0..gridLines) {
                    val y = canvasHeight * (g.toFloat() / gridLines)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Dessin des barres
                dailyStats.forEachIndexed { index, _ ->
                    val value = values.getOrElse(index) { 0f }
                    val barHeight = (value / maxValue) * (canvasHeight * 0.88f)
                    val x = (index * spacing) + (spacing - barWidth) / 2f
                    val y = canvasHeight - barHeight

                    val isHovered = hoveredIndex == index
                    val currentBarBrush = Brush.verticalGradient(
                        colors = listOf(
                            if (isHovered) Color.White else barColor,
                            if (isHovered) barColor else barColor.copy(alpha = 0.65f)
                        )
                    )

                    // Ombre / effet glow pour la barre sélectionnée
                    if (isHovered) {
                        drawRoundRect(
                            color = barColor.copy(alpha = 0.25f),
                            topLeft = Offset(x - 3.dp.toPx(), y - 3.dp.toPx()),
                            size = Size(barWidth + 6.dp.toPx(), barHeight + 3.dp.toPx()),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }

                    // Barre principale
                    drawRoundRect(
                        brush = currentBarBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            // Zone tactile transparente pour interagir avec chaque barre
            Row(modifier = Modifier.fillMaxSize()) {
                dailyStats.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelectDay(index) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Axe X avec les étiquettes de jour
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dailyStats.forEachIndexed { index, stat ->
                val isSelected = hoveredIndex == index
                Text(
                    text = stat.dayLabel,
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) barColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable { onSelectDay(index) }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Graphique en courbe d'aire lissée (Spline AreaChart style Recharts) avec dégradé sous courbe,
 * ligne moyenne pointillée et sélection interactive des points journaliers.
 */
@Composable
fun InteractiveUsageAreaChart(
    dailyStats: List<DailyUsageStat>,
    metricTab: Int,
    hoveredIndex: Int?,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = when (metricTab) {
        0 -> StatScreenTimeColor
        1 -> StatMessagesColor
        else -> StatDataColor
    }

    if (dailyStats.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Aucune donnée pour cette période", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        return
    }

    val values = remember(dailyStats, metricTab) {
        dailyStats.map { stat ->
            when (metricTab) {
                0 -> stat.screenTimeMinutes.toFloat()
                1 -> stat.totalMessages.toFloat()
                else -> (stat.totalDataBytes / (1024f * 1024f))
            }
        }
    }
    val maxValue = remember(values) { (values.maxOrNull() ?: 10f).coerceAtLeast(10f) }
    val avgValue = remember(values) { if (values.isNotEmpty()) values.average().toFloat() else 0f }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val count = values.size
                if (count < 2) return@Canvas

                // Grille horizontale
                for (g in 0..3) {
                    val y = h * (g / 3f)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.14f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Ligne de moyenne en pointillés
                val avgY = h - ((avgValue / maxValue) * (h * 0.84f)) - (h * 0.06f)
                drawLine(
                    color = lineColor.copy(alpha = 0.45f),
                    start = Offset(0f, avgY),
                    end = Offset(w, avgY),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )

                val stepX = w / (count - 1).coerceAtLeast(1)
                val points = values.mapIndexed { idx, v ->
                    val x = idx * stepX
                    val y = h - ((v / maxValue) * (h * 0.84f)) - (h * 0.06f)
                    Offset(x, y)
                }

                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val ctrlX = (prev.x + curr.x) / 2f
                        cubicTo(ctrlX, prev.y, ctrlX, curr.y, curr.x, curr.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, h)
                    lineTo(points.first().x, h)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.02f))
                    )
                )

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { idx, pt ->
                    val isSelected = hoveredIndex == idx
                    if (isSelected) {
                        drawLine(
                            color = lineColor.copy(alpha = 0.4f),
                            start = Offset(pt.x, 0f),
                            end = Offset(pt.x, h),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(color = lineColor.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = pt)
                    }
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 5.5.dp.toPx() else 3.8.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = lineColor,
                        radius = if (isSelected) 4.dp.toPx() else 2.6.dp.toPx(),
                        center = pt
                    )
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                dailyStats.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelectDay(index) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dailyStats.forEachIndexed { index, stat ->
                val isSelected = hoveredIndex == index
                Text(
                    text = stat.dayLabel,
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) lineColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable { onSelectDay(index) }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Diagramme Donut interactif de la répartition de consommation data par type de média.
 */
@Composable
fun MediaDataDonutChart(
    categories: List<DataUsageCategory>,
    selectedIndex: Int?,
    totalFormatted: String,
    onSelectSlice: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCategory = selectedIndex?.let { categories.getOrNull(it) }

    Box(
        modifier = modifier.clickable {
            if (categories.isNotEmpty()) {
                val next = if (selectedIndex == null) 0 else (selectedIndex + 1) % categories.size
                onSelectSlice(next)
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalPct = categories.sumOf { it.percentage.toDouble() }.toFloat().coerceAtLeast(1f)
            var startAngle = -90f
            val baseStroke = 22.dp.toPx()

            categories.forEachIndexed { index, cat ->
                val sweep = (cat.percentage / totalPct) * 360f
                if (sweep > 0f) {
                    val isSelected = selectedIndex == index
                    val strokeWidth = if (isSelected) baseStroke + 5.dp.toPx() else baseStroke
                    val inset = strokeWidth / 2f + 2.dp.toPx()
                    drawArc(
                        color = Color(cat.colorHex).copy(
                            alpha = if (selectedIndex == null || isSelected) 1f else 0.35f
                        ),
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2.5f).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2f, size.height - inset * 2f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (activeCategory != null) {
                    String.format(java.util.Locale.US, "%.0f%%", activeCategory.percentage)
                } else {
                    totalFormatted
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = activeCategory?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = activeCategory?.category?.substringBefore(" ") ?: "Total Data",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * Barre de progression segmentée proportionnelle représentant le trafic par catégorie.
 */
@Composable
fun SegmentedDataUsageBar(
    categories: List<DataUsageCategory>,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        categories.forEach { cat ->
            if (cat.percentage > 0f) {
                Box(
                    modifier = Modifier
                        .weight(cat.percentage.coerceAtLeast(1f))
                        .fillMaxHeight()
                        .background(Color(cat.colorHex))
                )
            }
        }
    }
}

/**
 * Carte miniature présentant un insight ou comportement d'utilisation.
 */
@Composable
fun InsightMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(15.dp))
                Text(title, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Section complète d'Utilisation Réseau détaillée par type de média (messages, photos, vidéos, appels),
 * conçue pour permettre à l'utilisateur de surveiller et maîtriser son forfait data mobile.
 */
@Composable
fun NetworkUsageDetailsCard(
    mediaDetails: List<MediaNetworkUsageDetail>,
    totalDataSent: String,
    totalDataReceived: String,
    totalDataSaved: String,
    onManageDataThrottles: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedItemTitle by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_network_usage_details"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // En-tête de section
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
                            .background(AdtPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = null,
                            tint = AdtPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Utilisation Réseau & Forfait Data",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Consommation détaillée par média (envois & réceptions)",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bannière synthèse data (Envois vs Réceptions + Données économisées)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AdtAccentGreen, modifier = Modifier.size(13.dp))
                            Text("Envoyées", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(totalDataSent, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = StatScreenTimeColor, modifier = Modifier.size(13.dp))
                            Text("Reçues", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(totalDataReceived, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(13.dp))
                            Text("Économisées", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(totalDataSaved, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AdtPrimary)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            // Liste accordéon interactive des 5 types de trafic
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                mediaDetails.forEach { item ->
                    val isExpanded = expandedItemTitle == item.mediaTypeTitle
                    MediaNetworkDetailRow(
                        detail = item,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedItemTitle = if (isExpanded) null else item.mediaTypeTitle
                        }
                    )
                }
            }

            // Conseil gestion forfait & raccourci vers économiseur de données
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AdtPrimary.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onManageDataThrottles() }
                    .testTag("banner_manage_data_saver")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = AdtPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Optimiser mon forfait data",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = AdtPrimary
                        )
                        Text(
                            text = "Ajuster les seuils de téléchargement automatique et activer le mode Économie d'énergie",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AdtPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Fiche détaillée pour un média réseau (photos, vidéos, appels, messages).
 */
@Composable
fun MediaNetworkDetailRow(
    detail: MediaNetworkUsageDetail,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val icon = when (detail.iconKey) {
        "VIDEOS" -> Icons.Default.Videocam
        "PHOTOS" -> Icons.Default.Photo
        "CALLS" -> Icons.Default.PhoneInTalk
        "DOCS" -> Icons.Default.Description
        else -> Icons.Default.Chat
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) Color(detail.colorHex) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .testTag("row_media_network_${detail.iconKey.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ligne principale : icône, titre, volume total & pourcentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(detail.colorHex).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(detail.colorHex),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = detail.mediaTypeTitle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${detail.itemsCount} éléments échangés",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = detail.formattedTotalBytes,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f%% du total", detail.percentageOfTotal),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(detail.colorHex)
                    )
                }
            }

            // Jauge de progression proportionnelle de la part du média
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (detail.percentageOfTotal / 100f).coerceIn(0.04f, 1f))
                        .fillMaxHeight()
                        .background(Color(detail.colorHex))
                )
            }

            // Détail étendu animé : Envois vs Réceptions + Conseil Data Saver
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = AdtAccentGreen, modifier = Modifier.size(13.dp))
                            Text("Émis : ", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(detail.formattedSentBytes, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = StatScreenTimeColor, modifier = Modifier.size(13.dp))
                            Text("Reçus : ", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(detail.formattedReceivedBytes, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Conseil d'économie
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(detail.colorHex),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = detail.dataSaverAdvice,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
