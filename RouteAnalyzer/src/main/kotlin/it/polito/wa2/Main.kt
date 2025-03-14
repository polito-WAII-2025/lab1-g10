package it.polito.wa2

import java.io.File
import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Waypoint(val timestamp: String, val latitude: Double, val longitude: Double)

@Serializable
data class MaxDistanceResult(val maxDistanceFromStart: WaypointDistance)

@Serializable
data class WaypointDistance(val waypoint: Waypoint, val distanceKm: Double)

fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371e3 // Raggio della Terra in metri
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaPhi = Math.toRadians(lat2 - lat1)
    val deltaLambda = Math.toRadians(lon2 - lon1)

    val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
            cos(phi1) * cos(phi2) *
            sin(deltaLambda / 2) * sin(deltaLambda / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return (R * c) / 1000 // Converti in km
}

fun maxDistanceFromStart(waypoints: List<Waypoint>): MaxDistanceResult? {
    if (waypoints.isEmpty()) return null

    val start = waypoints.first()
    val farthest = waypoints.maxByOrNull { haversine(start.latitude, start.longitude, it.latitude, it.longitude) }
    val maxDistance = farthest?.let { haversine(start.latitude, start.longitude, it.latitude, it.longitude) } ?: 0.0

    return farthest?.let { MaxDistanceResult(WaypointDistance(it, maxDistance)) }
}

fun readCsv(percorsoFile: String): List<Waypoint> {
    val waypointList = mutableListOf<Waypoint>()

    File(percorsoFile).bufferedReader().useLines { lines ->
        lines.drop(1)
            .forEach { line ->
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

    val result = maxDistanceFromStart(points)

    if (result != null) {
        val jsonResult = Json.encodeToString(result)
        //File("max_distance_result.json").writeText(jsonResult)
        //println("Risultato salvato in max_distance_result.json")
        println(jsonResult)
    }

    //val bestArea = mostFrequentedArea(points, 200.0) // 200 metri di raggio
    //println("Most frequented area center: $bestArea")
}