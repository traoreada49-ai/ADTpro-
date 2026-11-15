package com.example.ui.screens.tracking

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.BuildConfig
import com.example.data.model.GoogleMapsHelper
import com.example.data.model.LiveLocationPayload
import com.example.data.model.LocationWaypoint
import com.example.ui.components.AdtAvatar
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.LiveTrackingState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

enum class GoogleMapType(val apiName: String, val label: String) {
    ROADMAP("roadmap", "Plan"),
    SATELLITE("satellite", "Satellite"),
    HYBRID("hybrid", "Hybride"),
    TERRAIN("terrain", "Relief")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(viewModel: AdtViewModel) {
    val context = LocalContext.current
    val trackingState by viewModel.activeTrackingState.collectAsState()

    if (trackingState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AdtPrimary)
        }
        return
    }

    val state = trackingState!!
    val payload = state.payload
    val isLiveActive = payload.isActive && !payload.isExpired()

    var selectedMapType by remember { mutableStateOf(GoogleMapType.ROADMAP) }
    var showTraffic by remember { mutableStateOf(true) }
    var zoomLevel by remember { mutableStateOf(16) }
    var showWaypointsSheet by remember { mutableStateOf(false) }
    var showStreetViewDialog by remember { mutableStateOf(false) }

    // Countdown & progress ticker every 1 second
    var remainingFormatted by remember(payload) { mutableStateOf(payload.remainingTimeFormatted()) }
    var progressFraction by remember(payload) { mutableStateOf(payload.progressFraction()) }

    LaunchedEffect(payload, isLiveActive) {
        while (isLiveActive) {
            remainingFormatted = payload.remainingTimeFormatted()
            progressFraction = payload.progressFraction()
            delay(1000L)
        }
    }

    // Automatic movement simulation loop if enabled
    LaunchedEffect(state.isSimulating, state.simulationSpeedMultiplier, isLiveActive) {
        while (state.isSimulating && isLiveActive) {
            val stepDelay = (3500L / state.simulationSpeedMultiplier).coerceAtLeast(800L)
            delay(stepDelay)
            viewModel.simulateNextMovementStep()
        }
    }

    val apiKey = remember {
        try {
            BuildConfig.GOOGLE_MAPS_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    val staticMapUrl = remember(payload.latitude, payload.longitude, zoomLevel, selectedMapType, payload.waypoints.size, apiKey) {
        GoogleMapsHelper.buildStaticMapUrl(
            latitude = payload.latitude,
            longitude = payload.longitude,
            zoom = zoomLevel,
            width = 800,
            height = 600,
            mapType = selectedMapType.apiName,
            apiKey = apiKey,
            waypoints = payload.waypoints,
            showTraffic = showTraffic
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AdtAvatar(name = state.contactName, avatarKey = state.contactAvatar, sizeDp = 40)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = state.contactName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isLiveActive) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF00A884).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, Color(0xFF00A884))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color(0xFF00A884), CircleShape)
                                            )
                                            Text(
                                                "DIRECT",
                                                color = Color(0xFF00A884),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = if (isLiveActive) "Reste $remainingFormatted • Google Maps" else "Partage expiré",
                                fontSize = 11.5.sp,
                                color = if (isLiveActive) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("btn_back_live_tracking")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            GoogleMapsHelper.openInGoogleMapsApp(
                                context,
                                payload.latitude,
                                payload.longitude,
                                state.contactName
                            )
                        },
                        modifier = Modifier.testTag("btn_open_google_maps_app")
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = "Ouvrir dans Google Maps",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // MAP VIEWPORT
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("box_google_map_viewport")
            ) {
                // Background Static Map or fallback imagery from Google Maps API
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(staticMapUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Carte Google Maps",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // High-performance dynamic vector route overlay
                LiveMovementVectorCanvas(
                    waypoints = payload.waypoints,
                    currentLat = payload.latitude,
                    currentLng = payload.longitude,
                    bearing = payload.bearingDegrees,
                    isLive = isLiveActive,
                    zoom = zoomLevel,
                    modifier = Modifier.fillMaxSize()
                )

                // Contact Marker Pin
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLiveActive) {
                        LiveTrackingPulseRings()
                    }
                    ContactTrackingMarker(
                        contactName = state.contactName,
                        speedKmh = payload.currentSpeedKmh,
                        bearing = payload.bearingDegrees,
                        isLive = isLiveActive
                    )
                }

                // TOP FLOATING OVERLAY: Limited Duration Progress Bar & Google Maps Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Limited Duration Banner
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (isLiveActive) AdtPrimary else Color.Gray
                                    )
                                    Text(
                                        text = if (isLiveActive) "Partage limité : $remainingFormatted" else "Partage terminé",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }
                                Text(
                                    text = "${payload.durationMinutes} min au total",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (isLiveActive) AdtPrimary else Color.Gray,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    // Google Maps Type Switcher Chips
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GoogleMapType.values().forEach { type ->
                                val isSelected = selectedMapType == type
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) AdtPrimary else Color.Transparent,
                                    modifier = Modifier.clickable { selectedMapType = type }
                                ) {
                                    Text(
                                        text = type.label,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            VerticalDivider(modifier = Modifier.height(18.dp))
                            IconButton(
                                onClick = { showTraffic = !showTraffic },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Traffic,
                                    contentDescription = "Trafic",
                                    tint = if (showTraffic) Color(0xFFE53935) else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // RIGHT FLOATING MAP CONTROLS (Zoom +, Zoom -, Center, Street View)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { zoomLevel = (zoomLevel + 1).coerceAtMost(20) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("btn_zoom_in")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom +")
                    }
                    SmallFloatingActionButton(
                        onClick = { zoomLevel = (zoomLevel - 1).coerceAtLeast(10) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("btn_zoom_out")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom -")
                    }
                    SmallFloatingActionButton(
                        onClick = { zoomLevel = 16 },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("btn_center_contact")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Centrer", tint = AdtPrimary)
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            GoogleMapsHelper.openStreetViewInGoogleMaps(context, payload.latitude, payload.longitude)
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("btn_street_view")
                    ) {
                        Icon(Icons.Default.Streetview, contentDescription = "Street View", tint = Color(0xFFF57C00))
                    }
                }

                // BOTTOM SHEET CONTROLS: Telemetry, Simulation & Navigation Actions
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .testTag("card_live_telemetry_controls"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    ),
                    elevation = CardDefaults.cardElevation(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Contact Movement Telemetry Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speed Metric
                            TelemetryBadge(
                                icon = Icons.Default.Speed,
                                label = "Vitesse",
                                value = "%.0f km/h".format(payload.currentSpeedKmh),
                                color = AdtPrimary
                            )
                            // Bearing / Cap
                            TelemetryBadge(
                                icon = Icons.Default.Explore,
                                label = "Cap",
                                value = "${payload.bearingDegrees.toInt()}°",
                                color = Color(0xFF0288D1)
                            )
                            // ETA
                            val distance = GoogleMapsHelper.calculateDistanceMeters(
                                14.6928, -17.4467,
                                payload.latitude, payload.longitude
                            )
                            val etaMinutes = GoogleMapsHelper.estimateEtaMinutes(distance, payload.currentSpeedKmh)
                            TelemetryBadge(
                                icon = Icons.Default.DirectionsCar,
                                label = "Distance / ETA",
                                value = "%.1f km • %d min".format(distance / 1000.0, etaMinutes),
                                color = Color(0xFF7B1FA2)
                            )
                            // Battery
                            TelemetryBadge(
                                icon = Icons.Default.BatteryChargingFull,
                                label = "Batterie",
                                value = "${payload.batteryPercent}%",
                                color = Color(0xFF388E3C)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Current Address Info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = payload.address,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Lat: %.5f, Lng: %.5f • ${payload.waypoints.size} étapes enregistrées"
                                        .format(payload.latitude, payload.longitude),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // View Waypoint History button
                            FilledTonalButton(
                                onClick = { showWaypointsSheet = !showWaypointsSheet },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Trajet", fontSize = 11.sp)
                            }
                        }

                        // Real-time Movement Simulation Player Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleTrackingSimulation() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (state.isSimulating) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                            contentDescription = "Simulateur de déplacement",
                                            tint = AdtPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (state.isSimulating) "Simulation en cours..." else "Simulation en pause",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Génération des déplacements GPS",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(1, 2, 5).forEach { mult ->
                                        val isMult = state.simulationSpeedMultiplier == mult
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isMult) AdtPrimary else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.clickable { viewModel.setSimulationMultiplier(mult) }
                                        ) {
                                            Text(
                                                text = "${mult}x",
                                                color = if (isMult) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.simulateNextMovementStep() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                                    ) {
                                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text("+1 Pas", fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        // Bottom Actions: Google Maps Navigation & Stop Sharing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    GoogleMapsHelper.openNavigationInGoogleMaps(
                                        context,
                                        payload.latitude,
                                        payload.longitude,
                                        mode = "d"
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_start_google_navigation"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Itinéraire Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (isLiveActive) {
                                OutlinedButton(
                                    onClick = { viewModel.stopCurrentLiveTracking() },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                    modifier = Modifier.testTag("btn_stop_live_tracking")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Arrêter", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Expandable Waypoints History Bottom Sheet
            if (showWaypointsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showWaypointsSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Historique du trajet (${payload.waypoints.size} points)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            IconButton(onClick = { showWaypointsSheet = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer")
                            }
                        }

                        val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(payload.waypoints.reversed()) { wp ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
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
                                                    .background(AdtPrimary, CircleShape)
                                            )
                                            Column {
                                                Text(
                                                    wp.address.ifBlank { "Étape enregistrée" },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    "%.5f, %.5f".format(wp.latitude, wp.longitude),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                timeFormat.format(Date(wp.timestamp)),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AdtPrimary
                                            )
                                            Text(
                                                "%.0f km/h".format(wp.speedKmh),
                                                fontSize = 10.sp,
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
        }
    }
}

@Composable
fun TelemetryBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Text(label, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ContactTrackingMarker(
    contactName: String,
    speedKmh: Float,
    bearing: Float,
    isLive: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            // Orientation Arrow Compass Needle
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Direction",
                tint = Color(0xFF00A884),
                modifier = Modifier
                    .size(52.dp)
                    .rotate(bearing)
            )

            // Center Contact Avatar Pin
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .background(Color(0xFF00A884), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contactName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Beacon blinker dot
            if (isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(12.dp)
                        .background(Color(0xFF00E676), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
fun LiveTrackingPulseRings() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = (size.minDimension / 2f) * scale
        drawCircle(
            color = Color(0xFF00A884).copy(alpha = alpha),
            radius = radius,
            center = center
        )
    }
}

@Composable
fun LiveMovementVectorCanvas(
    waypoints: List<LocationWaypoint>,
    currentLat: Double,
    currentLng: Double,
    bearing: Float,
    isLive: Boolean,
    zoom: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Draw animated scanning grid lines representing GPS triangulation
        val gridSpacing = 40f
        var x = 0f
        while (x < w) {
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
            x += gridSpacing
        }
        var y = 0f
        while (y < h) {
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
            y += gridSpacing
        }

        // Draw displacement path polyline from waypoints
        if (waypoints.size >= 2) {
            val path = Path()
            val scaleFactor = (zoom * 2200f)

            // Convert coordinates to canvas offsets relative to center
            val points = waypoints.map { wp ->
                val dx = ((wp.longitude - currentLng) * scaleFactor).toFloat()
                val dy = -((wp.latitude - currentLat) * scaleFactor).toFloat()
                Offset(cx + dx, cy + dy)
            }

            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }

            // Glow path background
            drawPath(
                path = path,
                color = Color(0xFF00A884).copy(alpha = 0.35f),
                style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Main path line
            drawPath(
                path = path,
                color = Color(0xFF00A884),
                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw individual waypoint dots
            points.forEachIndexed { index, pt ->
                if (index == 0) {
                    // Origin point
                    drawCircle(color = Color(0xFF1E88E5), radius = 6f, center = pt)
                    drawCircle(color = Color.White, radius = 3f, center = pt)
                } else if (index < points.size - 1) {
                    drawCircle(color = Color(0xFF00A884), radius = 4f, center = pt)
                    drawCircle(color = Color.White, radius = 2f, center = pt)
                }
            }
        }
    }
}
