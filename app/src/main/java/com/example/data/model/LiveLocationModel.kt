package com.example.data.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

data class DurationOption(
    val minutes: Int,
    val label: String,
    val badge: String
)

object LocationDurationPresets {
    val options = listOf(
        DurationOption(minutes = 15, label = "15 minutes", badge = "15 min"),
        DurationOption(minutes = 60, label = "1 heure", badge = "1 h"),
        DurationOption(minutes = 480, label = "8 heures", badge = "8 h")
    )
}

data class SampleLocationPoint(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

object SampleLocations {
    val presets = listOf(
        SampleLocationPoint(
            name = "Dakar Plateau (Siège ADT)",
            address = "Avenue Léopold Sédar Senghor, Dakar, Sénégal",
            latitude = 14.6928,
            longitude = -17.4467
        ),
        SampleLocationPoint(
            name = "Almadies / Ngor",
            address = "Route des Almadies, Dakar, Sénégal",
            latitude = 14.7452,
            longitude = -17.5186
        ),
        SampleLocationPoint(
            name = "Abidjan Plateau",
            address = "Boulevard de la République, Abidjan, Côte d'Ivoire",
            latitude = 5.3203,
            longitude = -4.0161
        ),
        SampleLocationPoint(
            name = "Bamako Centre",
            address = "Avenue Modibo Keïta, Bamako, Mali",
            latitude = 12.6392,
            longitude = -8.0029
        )
    )
}

data class LocationWaypoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val speedKmh: Float = 0f,
    val bearing: Float = 0f,
    val address: String = ""
) {
    fun serialize(): JSONObject {
        val json = JSONObject()
        json.put("lat", latitude)
        json.put("lng", longitude)
        json.put("time", timestamp)
        json.put("speed", speedKmh.toDouble())
        json.put("bearing", bearing.toDouble())
        json.put("addr", address)
        return json
    }

    companion object {
        fun deserialize(json: JSONObject): LocationWaypoint {
            return LocationWaypoint(
                latitude = json.optDouble("lat", 0.0),
                longitude = json.optDouble("lng", 0.0),
                timestamp = json.optLong("time", System.currentTimeMillis()),
                speedKmh = json.optDouble("speed", 0.0).toFloat(),
                bearing = json.optDouble("bearing", 0.0).toFloat(),
                address = json.optString("addr", "")
            )
        }
    }
}

data class LiveLocationPayload(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val durationMinutes: Int,
    val startedAt: Long,
    val expiresAt: Long,
    val isActive: Boolean = true,
    val comment: String = "",
    val accuracyMeters: Float = 4.5f,
    val batteryPercent: Int = 88,
    val currentSpeedKmh: Float = 28f,
    val bearingDegrees: Float = 142f,
    val transportMode: String = "DRIVING", // DRIVING, WALKING, STATIONARY
    val waypoints: List<LocationWaypoint> = emptyList(),
    val contactName: String = "",
    val contactPhone: String = ""
) {
    fun isExpired(): Boolean {
        if (!isActive) return true
        return System.currentTimeMillis() > expiresAt
    }

    fun remainingTimeFormatted(): String {
        if (!isActive) return "Partage arrêté"
        val remainingMillis = expiresAt - System.currentTimeMillis()
        if (remainingMillis <= 0) return "Terminé"

        val minutes = (remainingMillis / (1000 * 60)).toInt()
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 -> "${hours}h ${remainingMinutes}min"
            minutes > 0 -> "${minutes} min"
            else -> "Moins d'une minute"
        }
    }

    fun progressFraction(): Float {
        val total = (expiresAt - startedAt).toFloat().coerceAtLeast(1f)
        val elapsed = (System.currentTimeMillis() - startedAt).toFloat().coerceIn(0f, total)
        return (elapsed / total).coerceIn(0f, 1f)
    }

    fun serialize(): String {
        val json = JSONObject()
        json.put("latitude", latitude)
        json.put("longitude", longitude)
        json.put("address", address)
        json.put("durationMinutes", durationMinutes)
        json.put("startedAt", startedAt)
        json.put("expiresAt", expiresAt)
        json.put("isActive", isActive)
        json.put("comment", comment)
        json.put("accuracyMeters", accuracyMeters.toDouble())
        json.put("batteryPercent", batteryPercent)
        json.put("currentSpeedKmh", currentSpeedKmh.toDouble())
        json.put("bearingDegrees", bearingDegrees.toDouble())
        json.put("transportMode", transportMode)
        json.put("contactName", contactName)
        json.put("contactPhone", contactPhone)

        val waypointsArray = JSONArray()
        waypoints.forEach { waypointsArray.put(it.serialize()) }
        json.put("waypoints", waypointsArray)

        return json.toString()
    }

    companion object {
        fun deserialize(raw: String): LiveLocationPayload? {
            return try {
                if (raw.isBlank() || !raw.trim().startsWith("{")) return null
                val json = JSONObject(raw)

                val waypointsList = mutableListOf<LocationWaypoint>()
                val wpArray = json.optJSONArray("waypoints")
                if (wpArray != null) {
                    for (i in 0 until wpArray.length()) {
                        val item = wpArray.optJSONObject(i)
                        if (item != null) {
                            waypointsList.add(LocationWaypoint.deserialize(item))
                        }
                    }
                }

                LiveLocationPayload(
                    latitude = json.optDouble("latitude", 14.6928),
                    longitude = json.optDouble("longitude", -17.4467),
                    address = json.optString("address", "Position actuelle"),
                    durationMinutes = json.optInt("durationMinutes", 60),
                    startedAt = json.optLong("startedAt", System.currentTimeMillis()),
                    expiresAt = json.optLong("expiresAt", System.currentTimeMillis() + 60 * 60 * 1000),
                    isActive = json.optBoolean("isActive", true),
                    comment = json.optString("comment", ""),
                    accuracyMeters = json.optDouble("accuracyMeters", 4.5).toFloat(),
                    batteryPercent = json.optInt("batteryPercent", 85),
                    currentSpeedKmh = json.optDouble("currentSpeedKmh", 28.0).toFloat(),
                    bearingDegrees = json.optDouble("bearingDegrees", 142.0).toFloat(),
                    transportMode = json.optString("transportMode", "DRIVING"),
                    waypoints = waypointsList,
                    contactName = json.optString("contactName", ""),
                    contactPhone = json.optString("contactPhone", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Google Maps API Helper for ADT Pro Live Tracking
 * Generates official Google Maps API endpoints (Static Maps, Street View, Directions)
 * and dispatches intents to native Google Maps.
 */
object GoogleMapsHelper {

    /**
     * Builds a Google Maps Static API request URL.
     * Renders high-resolution map with styled markers and polyline route trajectory.
     */
    fun buildStaticMapUrl(
        latitude: Double,
        longitude: Double,
        zoom: Int = 15,
        width: Int = 640,
        height: Int = 360,
        mapType: String = "roadmap", // roadmap, satellite, hybrid, terrain
        apiKey: String = "",
        waypoints: List<LocationWaypoint> = emptyList(),
        showTraffic: Boolean = false
    ): String {
        val baseUrl = "https://maps.googleapis.com/maps/api/staticmap"
        val params = StringBuilder()
        params.append("center=$latitude,$longitude")
        params.append("&zoom=$zoom")
        params.append("&size=${width}x$height")
        params.append("&scale=2")
        params.append("&maptype=$mapType")
        params.append("&format=png")

        // Custom ADT Pro emerald pin on current position
        params.append("&markers=color:0x00A884%7Clabel:P%7C$latitude,$longitude")

        // Draw movement trail / polyline if waypoints are available
        if (waypoints.isNotEmpty()) {
            val pathCoords = StringBuilder("&path=color:0x00A884EE%7Cweight:5")
            val sampledWaypoints = if (waypoints.size > 15) {
                waypoints.filterIndexed { index, _ -> index % (waypoints.size / 10 + 1) == 0 } + waypoints.last()
            } else {
                waypoints
            }
            for (wp in sampledWaypoints) {
                pathCoords.append("%7C${wp.latitude},${wp.longitude}")
            }
            params.append(pathCoords)
        }

        if (apiKey.isNotBlank() && apiKey != "YOUR_GOOGLE_MAPS_API_KEY") {
            params.append("&key=$apiKey")
        }

        return "$baseUrl?$params"
    }

    /**
     * Builds Google Maps Street View preview image URL.
     */
    fun buildStreetViewUrl(
        latitude: Double,
        longitude: Double,
        heading: Float = 0f,
        pitch: Int = 10,
        width: Int = 600,
        height: Int = 300,
        apiKey: String = ""
    ): String {
        val baseUrl = "https://maps.googleapis.com/maps/api/streetview"
        val params = StringBuilder("size=${width}x$height&location=$latitude,$longitude&heading=$heading&pitch=$pitch&fov=90")
        if (apiKey.isNotBlank() && apiKey != "YOUR_GOOGLE_MAPS_API_KEY") {
            params.append("&key=$apiKey")
        }
        return "$baseUrl?$params"
    }

    /**
     * Builds Google Maps Web directions URL.
     */
    fun buildDirectionsWebUrl(
        destLat: Double,
        destLng: Double,
        originLat: Double? = null,
        originLng: Double? = null,
        travelMode: String = "driving"
    ): String {
        return if (originLat != null && originLng != null) {
            "https://www.google.com/maps/dir/?api=1&origin=$originLat,$originLng&destination=$destLat,$destLng&travelmode=$travelMode"
        } else {
            "https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng&travelmode=$travelMode"
        }
    }

    /**
     * Launches Google Maps native app to display the coordinates.
     */
    fun openInGoogleMapsApp(context: Context, latitude: Double, longitude: Double, label: String = "Contact ADT Pro") {
        try {
            val encoded = Uri.encode(label)
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encoded)")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val genericUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, genericUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Coordonnées : $latitude, $longitude", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Launches turn-by-turn Navigation in Google Maps app.
     */
    fun openNavigationInGoogleMaps(context: Context, destLat: Double, destLng: Double, mode: String = "d") {
        try {
            // mode: 'd' for driving, 'w' for walking, 'b' for bicycling
            val uri = Uri.parse("google.navigation:q=$destLat,$destLng&mode=$mode")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    /**
     * Launches Street View in Google Maps app.
     */
    fun openStreetViewInGoogleMaps(context: Context, latitude: Double, longitude: Double) {
        try {
            val uri = Uri.parse("google.streetview:cbll=$latitude,$longitude")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Street View indisponible sur ce terminal", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Calculates distance between two coordinates in meters using Haversine formula.
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Estimates ETA in minutes given distance and speed.
     */
    fun estimateEtaMinutes(distanceMeters: Double, speedKmh: Float = 30f): Int {
        val effectiveSpeed = speedKmh.coerceAtLeast(10f)
        val speedMetersPerMinute = (effectiveSpeed * 1000.0) / 60.0
        return (distanceMeters / speedMetersPerMinute).roundToInt().coerceAtLeast(1)
    }

    /**
     * Generates a realistic progression route with realistic waypoints
     * simulating actual road displacement along city arteries.
     */
    fun generateRealisticMovementRoute(
        startLat: Double,
        startLng: Double,
        count: Int = 12
    ): List<LocationWaypoint> {
        val list = mutableListOf<LocationWaypoint>()
        val now = System.currentTimeMillis()

        // Vectors following city streets with realistic turns
        var currentLat = startLat
        var currentLng = startLng
        var bearing = 135f
        var speed = 32f

        val stepSeconds = 45L * 1000L

        for (i in 0 until count) {
            val time = now - (count - 1 - i) * stepSeconds
            val deltaLat = (cos(Math.toRadians(bearing.toDouble())) * 0.0018 * (speed / 30f))
            val deltaLng = (sin(Math.toRadians(bearing.toDouble())) * 0.0022 * (speed / 30f))

            currentLat += deltaLat
            currentLng += deltaLng

            // Occasional turns
            if (i == 3) bearing += 45f
            if (i == 7) bearing -= 35f
            if (i == 9) bearing += 20f

            speed = when (i % 4) {
                0 -> 24f
                1 -> 36f
                2 -> 45f
                else -> 30f
            }

            list.add(
                LocationWaypoint(
                    latitude = currentLat,
                    longitude = currentLng,
                    timestamp = time,
                    speedKmh = speed,
                    bearing = bearing,
                    address = "Étape ${i + 1}"
                )
            )
        }
        return list
    }
}

