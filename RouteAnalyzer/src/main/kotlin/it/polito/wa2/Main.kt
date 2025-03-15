package it.polito.wa2

import java.io.File
import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true) // Ignora eventuali campi extra nel file YAML
data class Config @JsonCreator constructor(
    @JsonProperty("earthRadiusKm") val earthRadiusKm: Double,
    @JsonProperty("geofenceCenterLatitude") val geofenceCenterLatitude: Double,
    @JsonProperty("geofenceCenterLongitude") val geofenceCenterLongitude: Double,
    @JsonProperty("geofenceRadiusKm") val geofenceRadiusKm: Double,
    @JsonProperty("mostFrequentedAreaRadiusKm") val mostFrequentedAreaRadiusKm: Double? = null
)


@Serializable
data class Waypoint(val timestamp: String, val latitude: Double, val longitude: Double)

@Serializable
data class MaxDistanceResult(val maxDistanceFromStart: WaypointDistance)

@Serializable
data class WaypointDistance(val waypoint: Waypoint, val distanceKm: Double)

fun loadConfig(filePath: String): Config? {
    return try {
        val file = File(filePath)
        if (!file.exists()) {
            println("Errore: Il file YAML non esiste -> $filePath")
            return null
        }

        val yamlMapper = YAMLMapper()
        yamlMapper.readValue(file, Config::class.java) // Converte il YAML in Config
    } catch (e: Exception) {
        println("Errore nella lettura del file YAML: ${e.message}")
        null
    }
}

fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double, earthRadiusKm: Double): Double {

    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaPhi = Math.toRadians(lat2 - lat1)
    val deltaLambda = Math.toRadians(lon2 - lon1)

    val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
            cos(phi1) * cos(phi2) *
            sin(deltaLambda / 2) * sin(deltaLambda / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return (earthRadiusKm * c) / 1000 // Converti in km
}

fun maxDistanceFromStart(waypoints: List<Waypoint>, earthRadiusKm: Double): MaxDistanceResult? {
    if (waypoints.isEmpty()) return null

    val start = waypoints.first()
    val farthest = waypoints.maxByOrNull { haversine(start.latitude, start.longitude, it.latitude, it.longitude,earthRadiusKm) }
    val maxDistance = farthest?.let { haversine(start.latitude, start.longitude, it.latitude, it.longitude,earthRadiusKm) } ?: 0.0

    return farthest?.let { MaxDistanceResult(WaypointDistance(it, maxDistance)) }
}
fun maxDistanceBetweenPoints(waypoints: List<Waypoint>, earthRadiusKm: Double): Double {
    var maxDistance = 0.0

    for (i in waypoints.indices) {
        for (j in i + 1 until waypoints.size) {
            val distance = haversine(
                waypoints[i].latitude, waypoints[i].longitude,
                waypoints[j].latitude, waypoints[j].longitude,
                earthRadiusKm
            )
            if (distance > maxDistance) {
                maxDistance = distance
            }
        }
    }

    return maxDistance
}

fun mostFrequentedArea(waypoints: List<Waypoint>, radius: Double, earthRadiusKm: Double): Waypoint? {
    if (waypoints.isEmpty()) return null

    var maxCount = 0
    var bestCenter: Waypoint? = null

    for (center in waypoints) {
        val count = waypoints.count { wp -> haversine(center.latitude, center.longitude, wp.latitude, wp.longitude, earthRadiusKm) <= radius }

        if (count > maxCount) {
            maxCount = count
            bestCenter = center
        }
    }

    return bestCenter
}

fun waypointsOutsideGeofence(waypoints: List<Waypoint>,centerLat: Double,centerLon: Double,radius: Double,earthRadiusKm: Double): Map<String, Any> {
    val countOutside = waypoints.count { wp ->
        haversine(wp.latitude, wp.longitude, centerLat, centerLon, earthRadiusKm) > radius
    }

    return mapOf(
        "waypointsOutsideGeofence" to mapOf(
            "centralWaypoint" to mapOf(
                "timestamp" to 0,
                "latitude" to centerLat,
                "longitude" to centerLon
            ),
            "areaRadiusKm" to radius,
            "count" to countOutside
        )
    )
}

fun readCsv(percorsoFile: String): List<Waypoint> {
    val waypointList = mutableListOf<Waypoint>()

    File(percorsoFile).bufferedReader().useLines { lines ->
        lines.forEach { line ->
                val fields = line.split(";")
                if (fields.size == 3) {
                    val waypoint = Waypoint(
                        timestamp = fields[0].trim(),
                        latitude = fields[1].trim().toDoubleOrNull() ?: 0.0,
                        longitude = fields[2].trim().toDoubleOrNull() ?: 0.0
                    )
                    waypointList.add(waypoint)
                }
            }
    }

    return waypointList
}

fun main() {
    val points = readCsv("src/main/resources/waypoints.csv")
    //points.forEach{println(it)}
    val config = loadConfig("src/main/resources/custom-parameters.yml")
    val result = maxDistanceFromStart(points, config!!.earthRadiusKm)

    if (result != null) {
        val jsonResult = Json.encodeToString(result)
        //File("max_distance_result.json").writeText(jsonResult)
        //println("Risultato salvato in max_distance_result.json")
        println(jsonResult)
    }

    val bestArea = mostFrequentedArea(points, 200.0, config.earthRadiusKm) // 200 metri di raggio
    println("Most frequented area center: $bestArea")
    println(config.earthRadiusKm)
    val outside = waypointsOutsideGeofence(points, config.geofenceCenterLatitude, config.geofenceCenterLongitude, config.geofenceRadiusKm, config.earthRadiusKm)
    println(outside)
    val max = maxDistanceBetweenPoints(points, config.earthRadiusKm)
    println(max)
}