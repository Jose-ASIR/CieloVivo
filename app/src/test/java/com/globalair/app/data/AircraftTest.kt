package com.globalair.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AircraftTest {
    private fun sample(speed: Double?, track: Double?) = Aircraft(
        hex = "abc123", callsign = "IBE123", registration = null, typeCode = "A320", description = null,
        operator = null, lat = 40.0, lon = -3.0, altitudeFt = 35000, onGround = false,
        groundSpeedKt = speed, trackDeg = track, verticalRateFpm = 0, squawk = null, emergency = false,
        vehicleType = VehicleType.AIRLINER, positionTimestamp = 0L,
    )

    @Test
    fun `extrapola hacia el norte segun velocidad`() {
        // 360 kt durante 10 s = 1 NM = 1/60 de grado de latitud.
        val moved = sample(360.0, 0.0).extrapolatedTo(10_000)
        assertEquals(40.0 + 1.0 / 60, moved.lat, 1e-3)
        assertEquals(-3.0, moved.lon, 1e-6)
    }

    @Test
    fun `sin velocidad no se mueve`() {
        assertEquals(40.0, sample(null, 90.0).extrapolatedTo(10_000).lat, 0.0)
    }

    @Test
    fun `clasifica por categoria ADS-B y bandera militar`() {
        assertEquals(VehicleType.HELICOPTER, classify("A7", 0, null, null))
        assertEquals(VehicleType.BALLOON, classify("B2", null, null, null))
        assertEquals(VehicleType.DRONE, classify("B6", null, null, null))
        assertEquals(VehicleType.MILITARY, classify("A3", 1, "A400", null))
        assertEquals(VehicleType.UNKNOWN, classify(null, null, null, null))
    }
}
