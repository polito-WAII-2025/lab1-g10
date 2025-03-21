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

@Serializable
data class AdvancedAnalysisResult(val totalPathLength: PathLength, val intersections: Intersections)

@Serializable
data class PathLength(val km: Double)

@Serializable
data class Intersections(val waypoints : List<Waypoint>)

data class HandlerFile(val waypoints: File, val custom: File)

// Load configuration parameters from a YAML file
fun loadConfig(file: File): Config {
    val yamlMapper = YAMLMapper()
    return yamlMapper.readValue(file, Config::class.java) // Converte il YAML in Config


}

// Compute the great-circle distance between two points using the Haversine formula
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

// Find the farthest waypoint from the starting point
fun maxDistanceFromStart(waypoints: List<Waypoint>, earthRadiusKm: Double): MaxDistanceResult? {
    if (waypoints.isEmpty()) return null

    val start = waypoints.first()
    val farthest = waypoints.maxByOrNull { haversine(start.latitude, start.longitude, it.latitude, it.longitude, earthRadiusKm) }
    val maxDistance = farthest?.let { haversine(start.latitude, start.longitude, it.latitude, it.longitude, earthRadiusKm) } ?: 0.0

    return farthest?.let { MaxDistanceResult(it, maxDistance) }
}

// Compute the maximum distance between any two waypoints
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

// Find the most frequently visited area within a specified radius
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

// Identify waypoints that fall outside the defined geofence
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

fun calculatePathLength(waypoints: List<Waypoint>, earthRadiusKm: Double): Double {
    if (waypoints.size < 2) return 0.0

    var totalDistance = 0.0
    for (i in 0 until waypoints.size - 1) {
        totalDistance += haversine(
            waypoints[i].latitude, waypoints[i].longitude,
            waypoints[i + 1].latitude, waypoints[i + 1].longitude,
            earthRadiusKm
        )
    }
    return totalDistance
}

// Function to find intersections during the path
fun findIntersections(waypoints: List<Waypoint>): List<Waypoint> {
    val intersections = mutableListOf<Waypoint>()
    val visited = mutableSetOf<Pair<Double, Double>>()

    for (waypoint in waypoints) {
        val location = Pair(waypoint.latitude, waypoint.longitude)
        if (location in visited) {
            intersections.add(waypoint)
        } else {
            visited.add(location)
        }
    }

    return intersections
}

// Read waypoints from a CSV file
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
    val directory= File("files")
    if (!directory.exists()) {
        println("Error: Folder not found\nMount standard folder")

        return HandlerFile(File("src/main/resources/waypoints.csv"),File("src/main/resources/custom-parameters.yml"))
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
        return HandlerFile(waypointsFile, File("src/main/resources/custom-parameters.yml"))
    }

    if(checkCustom){
        println("Error: waypoints.csv not found\nMount standard waypoints.csv")
        return HandlerFile(File("src/main/resources/waypoints.csv"), customFile)
    }

    println("Error: waypoints.csv and custom-parameters.yml not found\nMount standard waypoints.csv and custom-parameters.yml")
    return HandlerFile(File("src/main/resources/waypoints.csv"), File("src/main/resources/custom-parameters.yml"))
}

fun main() {
    val files = mountFiles()

    val points = readCsv(files.waypoints)
    val config = loadConfig(files.custom)
    val maxDistance = maxDistanceFromStart(points, config.earthRadiusKm)

    val maxDistBetweenPoints = maxDistanceBetweenPoints(points, config.earthRadiusKm)
    val mostFrequentedRadius = config.mostFrequentedAreaRadiusKm ?: if (maxDistBetweenPoints < 1) 0.1 else maxDistBetweenPoints * 0.1
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

    val directory = File("files")
    val subDirectory = File(directory, "output")
    if (!subDirectory.exists()) {
        subDirectory.mkdirs()
    }

    val outfile = File(subDirectory, "output.json")
    if (!outfile.exists()) {
        outfile.createNewFile()
    }
    outfile.writeText(jsonResult)
    println("Result saved in output.json")

    // Calculate the total path length
    val totalPathLength = PathLength(calculatePathLength(points, config.earthRadiusKm))
    println("Total path length: $totalPathLength km")

    // Find intersections during the path
    val intersections = Intersections(findIntersections(points))
    println(intersections)
    // Save the path length and intersections in output_advanced.json
    val advancedOutfile = File(subDirectory, "output_advanced.json")
    if (!advancedOutfile.exists()) {
        advancedOutfile.createNewFile()
    }

    val analysisResultAdvanced = AdvancedAnalysisResult(totalPathLength, intersections)
    val jsonResultAdvanced = jsonFormatter.encodeToString(analysisResultAdvanced)
    advancedOutfile.writeText(jsonResultAdvanced)
    println("Path length and intersections saved in output_advanced.json")
}