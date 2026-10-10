package com.cielovivo.radar.data

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Familia de vehículo aéreo, derivada de la categoría de emisor ADS-B (DO-260B) y la base de datos de tipos. */
enum class VehicleType(val label: String, val icon: String, val color: Long) {
    AIRLINER("Comercial", "ic_airliner", 0xFF38BDF8),
    LIGHT("Avioneta", "ic_light", 0xFF4ADE80),
    JET("Alta perf.", "ic_jet", 0xFFF472B6),
    HELICOPTER("Helicóptero", "ic_helicopter", 0xFFFACC15),
    GLIDER("Planeador", "ic_glider", 0xFFA78BFA),
    BALLOON("Globo/dirigible", "ic_balloon", 0xFFFB923C),
    DRONE("Dron/UAV", "ic_drone", 0xFF2DD4BF),
    PARACHUTE("Paracaidista", "ic_parachute", 0xFFE879F9),
    SPACE("Vehículo espacial", "ic_space", 0xFFFFFFFF),
    MILITARY("Militar", "ic_jet", 0xFFEF4444),
    UNKNOWN("Otros", "ic_unknown", 0xFF94A3B8);
}

data class Aircraft(
    val hex: String,
    val callsign: String?,
    val registration: String?,
    val typeCode: String?,
    val description: String?,
    val operator: String?,
    val lat: Double,
    val lon: Double,
    /** Altitud barométrica en pies; null si está en tierra o se desconoce. */
    val altitudeFt: Int?,
    val onGround: Boolean,
    /** Velocidad respecto al suelo en nudos. */
    val groundSpeedKt: Double?,
    /** Rumbo verdadero en grados (0 = norte). */
    val trackDeg: Double?,
    val verticalRateFpm: Int?,
    val squawk: String?,
    val emergency: Boolean,
    val vehicleType: VehicleType,
    /** Instante (ms epoch) al que corresponde la posición recibida. */
    val positionTimestamp: Long,
) {
    val displayName: String get() = callsign?.takeIf { it.isNotBlank() } ?: registration ?: hex.uppercase()

    /**
     * Estima la posición actual por navegación a estima (dead reckoning) a partir de la última
     * posición, la velocidad y el rumbo. Permite que el mapa se mueva de forma fluida entre consultas.
     */
    fun extrapolatedTo(nowMs: Long): Aircraft {
        val speed = groundSpeedKt ?: return this
        val track = trackDeg ?: return this
        if (onGround) return this
        val dtSec = ((nowMs - positionTimestamp) / 1000.0).coerceIn(0.0, MAX_EXTRAPOLATION_SEC)
        if (dtSec <= 0.0) return this
        val distanceRad = (speed * KT_TO_MS * dtSec) / EARTH_RADIUS_M
        val brg = track.toRad()
        val lat1 = lat.toRad()
        val lon1 = lon.toRad()
        val lat2 = asin(sin(lat1) * cos(distanceRad) + cos(lat1) * sin(distanceRad) * cos(brg))
        val lon2 = lon1 + atan2(sin(brg) * sin(distanceRad) * cos(lat1), cos(distanceRad) - sin(lat1) * sin(lat2))
        return copy(lat = lat2.toDeg(), lon = ((lon2.toDeg() + 540) % 360) - 180)
    }

    companion object {
        private const val EARTH_RADIUS_M = 6_371_000.0
        private const val KT_TO_MS = 0.514444
        /** No se extrapola más allá de este tiempo para no inventar trayectorias. */
        private const val MAX_EXTRAPOLATION_SEC = 30.0
        private fun Double.toRad() = this * PI / 180
        private fun Double.toDeg() = this * 180 / PI
    }
}
