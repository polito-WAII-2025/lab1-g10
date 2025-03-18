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

fun loadConfig(filePath: String): Config? {
    return try {
        val file = File(filePath)
        if (!file.exists()) {
            println("Error: The YAML file does not exist -> $filePath")
            return null
        }

        val yamlMapper = YAMLMapper()
        yamlMapper.readValue(file, Config::class.java) // Converte il YAML in Config
    } catch (e: Exception) {
        println("Error reading the YAML file: ${e.message}")
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

fun readCsv(percorsoFile: String): List<Waypoint> {
    if (!percorsoFile.endsWith(".csv")) {
        println("Error: The file $percorsoFile is not a valid CSV file.")
        throw IllegalArgumentException("The file must have a .csv extension")
    }

    val file = File(percorsoFile)
    if (!file.exists()) {
        println("Error: The file $percorsoFile doesn't exist.")
        throw IllegalStateException("The CSV file was not found.")
    }

    if (file.length() == 0L) {
        println("Error: The file $percorsoFile is empty.")
        throw IllegalStateException("The CSV file is empty.")
    }

    val waypointList = mutableListOf<Waypoint>()

    try {
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
                    println("Warning: Line witch incorrect format -> $line")
                }
            }
        }
    } catch (e: Exception) {
        println("The program was interrupted due to an error: ${e.message}")
        throw e
    }

    return waypointList
}

fun main() {

    /* DOCKER */

    //val points = File("evaluation/waypoints.csv").readText() --> apertura file waypoints.csv montato su docker

    // Creazioni e scrittura file output.json su docker
    //val directory= File("evaluation")
    //val file= File(directory, "output.json")
    //file.writeText("ciao")

    // val points = File("resources/waypoints.csv").readText() --> apertura file waypoints.csv standard (inserito su docker)


    try { val points = readCsv("src/main/resources/waypoints.csv")
        //points.forEach{println(it)}
        val config = loadConfig("src/main/resources/custom-parameters.yml")
        val result = maxDistanceFromStart(points, config!!.earthRadiusKm)

        if (result != null) {
            val jsonResult = Json.encodeToString(result)
            //File("max_distance_result.json").writeText(jsonResult)
            //println("Result saved in max_distance_result.json")
            println(jsonResult)
        }

        val mostFrequentedRadius = config.mostFrequentedAreaRadiusKm ?: (maxDistanceBetweenPoints(points, config.earthRadiusKm) * 0.1)
        val bestArea = mostFrequentedArea(points, mostFrequentedRadius, config.earthRadiusKm)
        println("Most frequented area center: $bestArea")
        println(config.earthRadiusKm)
        val outside = waypointsOutsideGeofence(points, config.geofenceCenterLatitude, config.geofenceCenterLongitude, config.geofenceRadiusKm, config.earthRadiusKm)
        println(outside)
        val max = maxDistanceBetweenPoints(points, config.earthRadiusKm)
        println(max)
        val analysisResult = AnalysisResult(result, bestArea, outside)
        val jsonResult = Json.encodeToString(analysisResult)
        File("src/main/resources/output.json").writeText(jsonResult)

        println("Result saved in output.json")
    } catch (e: Exception) {
        println("The program was interrupted due to an error: ${e.message}")    }

}