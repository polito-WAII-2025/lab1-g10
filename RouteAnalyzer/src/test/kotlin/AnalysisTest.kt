package it.polito.wa2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AnalysisTest {

    @Test
    fun testHaversine() {
        val earthRadiusKm = 6371.0
        val lat1 = 45.0
        val lon1 = 7.0
        val lat2 = 45.1
        val lon2 = 7.1

        val distance = haversine(lat1, lon1, lat2, lon2, earthRadiusKm)

        assertEquals(13.95, distance, 0.5, "La distanza calcolata è errata")
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

        assertNotNull(result, "Il risultato non dovrebbe essere nullo")
        assertEquals(waypoints.last(), result!!.waypoint, "Il waypoint più distante non è corretto")
        assertTrue(result.distanceKm > 0, "La distanza non dovrebbe essere zero")
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

        assertTrue(maxDistance > 0, "La distanza massima dovrebbe essere maggiore di zero")
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

        assertNotNull(result, "L'area più frequentata non dovrebbe essere nulla")
        assertTrue(result!!.entriesCount > 1, "Ci dovrebbero essere almeno 2 punti nell'area più frequentata")
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

        assertEquals(1, result.count, "Dovrebbe esserci un solo waypoint fuori dalla geofence")
    }

    @Test
    fun testLoadConfig() {
        val config = loadConfig("src/test/resources/test-config.yml")

        assertNotNull(config, "Il file di configurazione non dovrebbe essere nullo")
        assertEquals(6371.0, config!!.earthRadiusKm, "Il raggio terrestre dovrebbe essere corretto")
    }

    @Test
    fun testReadCsv() {
        val waypoints = readCsv("src/test/resources/test-waypoints.csv")

        assertEquals(3, waypoints.size, "Dovrebbero esserci 3 waypoint nel CSV")
        assertEquals(45.0, waypoints[0].latitude, "La latitudine del primo waypoint non è corretta")
    }

}
