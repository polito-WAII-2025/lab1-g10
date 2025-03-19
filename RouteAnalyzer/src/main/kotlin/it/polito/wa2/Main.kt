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
data class Waypoint(val timestamp: Long, val latitude: Double, val longitude: Double)

@Serializable
data class MaxDistanceResult(val waypoint: Waypoint, val distanceKm: Double)

@Serializable
data class MostFrequentedAreaResult(val centralWaypoint: Waypoint, val areaRadiusKm: Double, val entriesCount: Int)

@Serializable data class WaypointsOutsideGeofence(val centralWaypoint: Waypoint,
                                                  val areaRadiusKm: Double, val count: Int, val waypoints: List<Waypoint>)

@Serializable
data class AnalysisResult(val maxDistanceFromStart: MaxDistanceResult?, val mostFrequentedArea: MostFrequentedAreaResult?, val waypointsOutsideGeofence: WaypointsOutsideGeofence)

data class HandlerFile(val waypoints: File, val custom: File)

fun loadConfig(file: File): Config {
    val yamlMapper = YAMLMapper()
    return yamlMapper.readValue(file, Config::class.java) // Converte il YAML in Config


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

    return (earthRadiusKm * c)
}

fun maxDistanceFromStart(waypoints: List<Waypoint>, earthRadiusKm: Double): MaxDistanceResult? {
    if (waypoints.isEmpty()) return null

    val start = waypoints.first()
    val farthest = waypoints.maxByOrNull { haversine(start.latitude, start.longitude, it.latitude, it.longitude, earthRadiusKm) }
    val maxDistance = farthest?.let { haversine(start.latitude, start.longitude, it.latitude, it.longitude, earthRadiusKm) } ?: 0.0

    return farthest?.let { MaxDistanceResult(it, maxDistance) }
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

fun mostFrequentedArea(waypoints: List<Waypoint>, radius: Double, earthRadiusKm: Double): MostFrequentedAreaResult? {
    if (waypoints.isEmpty()) return null

    var maxCount = 0
    var bestCenter: Waypoint? = null

    for (center in waypoints) {
        val count = waypoints.count { wp ->
            haversine(center.latitude, center.longitude, wp.latitude, wp.longitude, earthRadiusKm) <= radius
        }

        if (count > maxCount) {
            maxCount = count
            bestCenter = center
        }
    }

    return bestCenter?.let { MostFrequentedAreaResult(it, radius, maxCount) }
}

fun waypointsOutsideGeofence(
    waypoints: List<Waypoint>,
    centerLat: Double,
    centerLon: Double,
    radius: Double,
    earthRadiusKm: Double
): WaypointsOutsideGeofence {
    val outsideWaypoints = waypoints.filter { wp ->
        haversine(wp.latitude, wp.longitude, centerLat, centerLon, earthRadiusKm) > radius
    }

    val centralWaypoint = Waypoint(0, centerLat, centerLon)

    return WaypointsOutsideGeofence(
        centralWaypoint = centralWaypoint,
        areaRadiusKm = radius,
        count = outsideWaypoints.size,
        waypoints = outsideWaypoints
    )
}

fun readCsv(file: File): List<Waypoint> {

    val waypointList = mutableListOf<Waypoint>()

    file.bufferedReader().useLines { lines ->
        lines.forEach { line ->
            val fields = line.split(";")
            if (fields.size == 3) {
                val waypoint = Waypoint(
                    timestamp = fields[0].trim().toLongOrNull() ?: 0L,
                    latitude = fields[1].trim().toDoubleOrNull() ?: 0.0,
                    longitude = fields[2].trim().toDoubleOrNull() ?: 0.0
                )
                waypointList.add(waypoint)
            } else {
                println("Warning: Line with incorrect format-> $line")
            }
        }
    }

    return waypointList
}

fun mountFiles(): HandlerFile {
    val directory= File("evaluation")
    if (!directory.exists()) {
        println("Error: Folder not found\nMount standard folder")

        return HandlerFile(File("resources/waypoints.csv"),File("resources/custom-parameters.yml"))
    }

    val waypointsFile = File(directory, "waypoints.csv")
    val customFile = File(directory, "custom-parameters.yml")

    val checkWaypoint= waypointsFile.exists() && waypointsFile.length()> 0
    val checkCustom= customFile.exists() && customFile.length()> 0

    if(checkWaypoint){
        if(checkCustom){
            return HandlerFile(waypointsFile, customFile)
        }
        println("Error: custom-parameters.yml not found\nMount standard custom-parameters.yml")
        return HandlerFile(waypointsFile, File("resources/custom-parameters.yml"))
    }

    if(checkCustom){
        println("Error: waypoints.csv not found\nMount standard waypoints.csv")
        return HandlerFile(File("resources/waypoints.csv"), customFile)
    }

    println("Error: waypoints.csv and custom-parameters.yml not found\nMount standard waypoints.csv and custom-parameters.yml")
    return HandlerFile(File("resources/waypoints.csv"), File("resources/custom-parameters.yml"))
}

fun main() {
    val files = mountFiles()

    val points = readCsv(files.waypoints)
    val config = loadConfig(files.custom)
    val maxDistance = maxDistanceFromStart(points, config.earthRadiusKm)

    val mostFrequentedRadius = config.mostFrequentedAreaRadiusKm ?: (maxDistanceBetweenPoints(points, config.earthRadiusKm) * 0.1)
    val bestArea = mostFrequentedArea(points, mostFrequentedRadius, config.earthRadiusKm)
    println("Most frequented area center: $bestArea")
    println(config.earthRadiusKm)
    val outside = waypointsOutsideGeofence(points, config.geofenceCenterLatitude, config.geofenceCenterLongitude, config.geofenceRadiusKm, config.earthRadiusKm)
    println(outside)
    val max = maxDistanceBetweenPoints(points, config.earthRadiusKm)
    println(max)
    val analysisResult = AnalysisResult(maxDistance, bestArea, outside)
    val jsonFormatter = Json { prettyPrint = true }

    val jsonResult = jsonFormatter.encodeToString(analysisResult)
    File("src/main/resources/output.json").writeText(jsonResult)

    val directory= File("evaluation")
    File(directory, "output.json").writeText(jsonResult)

    println("Result saved in output.json")

}