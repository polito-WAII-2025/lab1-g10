package it.polito.wa2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class AnalysisTest {

    @Test
    fun testHaversine() {
        val earthRadiusKm = 6371.0
        val lat1 = 45.0
        val lon1 = 7.0
        val lat2 = 45.1
        val lon2 = 7.1

        val distance = haversine(lat1, lon1, lat2, lon2, earthRadiusKm)

        assertEquals(13.95, distance, 0.5, "The calculated distance is incorrect")
    }

    @Test
    fun testMaxDistanceFromStart() {
        val earthRadiusKm = 6371.0
        val waypoints = listOf(
            Waypoint(1, 45.0, 7.0),
            Waypoint(2, 45.1, 7.1),
            Waypoint(3, 45.2, 7.2)
        )

        val result = maxDistanceFromStart(waypoints, earthRadiusKm)

        assertNotNull(result, "The result should not be null")
        assertEquals(waypoints.last(), result!!.waypoint, "The farthest waypoint is incorrect")
        assertTrue(result.distanceKm > 0, "The distance should not be zero")
    }

    @Test
    fun testMaxDistanceBetweenPoints() {
        val earthRadiusKm = 6371.0
        val waypoints = listOf(
            Waypoint(1, 45.0, 7.0),
            Waypoint(2, 45.1, 7.1),
            Waypoint(3, 45.2, 7.2)
        )

        val maxDistance = maxDistanceBetweenPoints(waypoints, earthRadiusKm)

        assertTrue(maxDistance > 0, "The maximum distance should be greater than zero")
    }

    @Test
    fun testMostFrequentedArea() {
        val earthRadiusKm = 6371.0
        val waypoints = listOf(
            Waypoint(1, 45.0, 7.0),
            Waypoint(2, 45.0001, 7.0001),
            Waypoint(3, 45.0002, 7.0002),
            Waypoint(4, 46.0, 8.0)
        )

        val radius = 100.0 // 100 metri

        val result = mostFrequentedArea(waypoints, radius, earthRadiusKm)

        assertNotNull(result, "The most frequented area should not be null")
        assertTrue(result!!.entriesCount > 1, "There should be at least 2 points in the most frequented area")
    }

    @Test
    fun testWaypointsOutsideGeofence() {
        val earthRadiusKm = 6371.0
        val centerLat = 45.0
        val centerLon = 7.0
        val radius = 5.0

        val waypoints = listOf(
            Waypoint(1, 45.0, 7.0),  // Dentro il geofence
            Waypoint(2, 46.0, 8.0)   // Fuori dal geofence
        )

        val result = waypointsOutsideGeofence(waypoints, centerLat, centerLon, radius, earthRadiusKm)

        assertEquals(1, result.count, "There should be only one waypoint outside the geofence")
    }

    @Test
    fun testLoadConfig() {
        val file = File("src/test/resources/test-config.yml")
        val config = loadConfig(file)

        assertNotNull(config, "The yml file should not be null")
        assertEquals(6371.0, config.earthRadiusKm, "The Earth radius should be correct")
    }

    @Test
    fun testReadCsv() {
        val file = File("src/test/resources/test-waypoints.csv")
        val waypoints = readCsv(file)

        assertEquals(3, waypoints.size, "There should be 3 waypoints in the CSV")
        assertEquals(45.0, waypoints[0].latitude, "The latitude of the first waypoint is incorrect")
    }

}
