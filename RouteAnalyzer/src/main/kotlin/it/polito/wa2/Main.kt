package it.polito.wa2

import java.io.File
import kotlin.math.*


data class Waypoint(val timestamp: String, val latitude: Double, val longitude: Double)

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
    points.forEach{println(it)}

    //val bestArea = mostFrequentedArea(points, 200.0) // 200 metri di raggio
    //println("Most frequented area center: $bestArea")
}