package com.globalair.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/** El servidor pide bajar el ritmo (HTTP 429). [retryAfterSec] viene de la cabecera Retry-After. */
class RateLimitedException(val retryAfterSec: Long?) : IOException("Límite de peticiones (429)")

/**
 * Cliente para los agregadores ADS-B comunitarios compatibles con el formato readsb
 * (adsb.lol, adsb.fi). Ambos son gratuitos, sin API key, y cubren todo el mundo gracias
 * a miles de receptores voluntarios. Limitan a ~1 petición/s por IP.
 */
class AdsbApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
) {
    enum class Provider(val label: String, private val urlTemplate: String) {
        ADSB_LOL("adsb.lol", "https://api.adsb.lol/v2/lat/%.4f/lon/%.4f/dist/%d"),
        ADSB_FI("adsb.fi", "https://opendata.adsb.fi/api/v3/lat/%.4f/lon/%.4f/dist/%d");

        fun url(lat: Double, lon: Double, radiusNm: Int) = String.format(Locale.US, urlTemplate, lat, lon, radiusNm)
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    /** Devuelve todas las aeronaves en un radio (millas náuticas, máx. 250) alrededor de un punto. */
    suspend fun aircraftAround(provider: Provider, lat: Double, lon: Double, radiusNm: Int): List<Aircraft> =
        withContext(Dispatchers.IO) {
            val url = provider.url(lat, lon, radiusNm.coerceIn(1, MAX_RADIUS_NM))
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GlobalAir-Android/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitedException(response.header("Retry-After")?.toLongOrNull())
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body?.string() ?: throw IOException("Respuesta vacía")
                val parsed = json.decodeFromString(ReadsbResponse.serializer(), body)
                // "now" llega en ms en la API v2, pero en segundos en el aircraft.json clásico de readsb.
                val nowMs = parsed.now?.let { if (it < 1e11) (it * 1000).toLong() else it.toLong() }
                    ?: System.currentTimeMillis()
                (parsed.ac ?: parsed.aircraft).orEmpty().mapNotNull { it.toAircraft(nowMs) }
            }
        }

    companion object {
        const val MAX_RADIUS_NM = 250
    }
}

@Serializable
private data class ReadsbResponse(
    val ac: List<ReadsbAircraft>? = null,
    /** Nombre del array en las APIs más antiguas (adsb.fi v2). */
    val aircraft: List<ReadsbAircraft>? = null,
    val now: Double? = null,
)

@Serializable
private data class ReadsbAircraft(
    val hex: String,
    val flight: String? = null,
    val r: String? = null,
    val t: String? = null,
    val desc: String? = null,
    val ownOp: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("alt_baro") val altBaro: JsonElement? = null,
    @SerialName("alt_geom") val altGeom: Int? = null,
    val gs: Double? = null,
    val track: Double? = null,
    @SerialName("true_heading") val trueHeading: Double? = null,
    @SerialName("baro_rate") val baroRate: Int? = null,
    @SerialName("geom_rate") val geomRate: Int? = null,
    val squawk: String? = null,
    val emergency: String? = null,
    val category: String? = null,
    val dbFlags: Int? = null,
    @SerialName("seen_pos") val seenPos: Double? = null,
    val type: String? = null,
) {
    fun toAircraft(nowMs: Long): Aircraft? {
        // Categorías C* = vehículos de superficie y obstáculos (torres, grúas): no vuelan.
        if (category?.startsWith("C", ignoreCase = true) == true) return null
        if (t in GROUND_TYPES) return null
        val la = lat ?: return null
        val lo = lon ?: return null
        // alt_baro es un número de pies o la cadena "ground".
        val altPrim = altBaro as? JsonPrimitive
        val onGround = altPrim?.isString == true && altPrim.content == "ground"
        val altitude = if (onGround) null else altPrim?.intOrNull ?: altPrim?.doubleOrNull?.toInt() ?: altGeom
        return Aircraft(
            hex = hex.removePrefix("~"),
            callsign = flight?.trim()?.ifEmpty { null },
            registration = r,
            typeCode = t,
            description = desc,
            operator = ownOp,
            lat = la,
            lon = lo,
            altitudeFt = altitude,
            onGround = onGround,
            groundSpeedKt = gs,
            trackDeg = track ?: trueHeading,
            verticalRateFpm = baroRate ?: geomRate,
            squawk = squawk,
            emergency = (emergency != null && emergency != "none") || squawk in EMERGENCY_SQUAWKS,
            vehicleType = classify(category, dbFlags, t, desc),
            positionTimestamp = nowMs - ((seenPos ?: 0.0) * 1000).toLong(),
        )
    }

    companion object {
        private val EMERGENCY_SQUAWKS = setOf("7500", "7600", "7700")
        private val GROUND_TYPES = setOf("TWR", "GND", "SERV", "EMER", "OBST")
    }
}

/**
 * Clasifica el vehículo. Prioridad: bandera militar de la BD → categoría ADS-B → pistas del tipo.
 * Categorías ADS-B: A1 ligero, A2 pequeño, A3 grande, A4 B757, A5 pesado, A6 alta performance,
 * A7 helicóptero, B1 planeador, B2 más ligero que el aire, B3 paracaidista, B4 ultraligero,
 * B6 UAV, B7 vehículo espacial.
 */
internal fun classify(category: String?, dbFlags: Int?, typeCode: String?, desc: String?): VehicleType {
    if (dbFlags != null && dbFlags and 1 == 1) return VehicleType.MILITARY
    when (category?.uppercase()) {
        "A1", "A2", "B4" -> return VehicleType.LIGHT
        "A3", "A4", "A5" -> return VehicleType.AIRLINER
        "A6" -> return VehicleType.JET
        "A7" -> return VehicleType.HELICOPTER
        "B1" -> return VehicleType.GLIDER
        "B2" -> return VehicleType.BALLOON
        "B3" -> return VehicleType.PARACHUTE
        "B6" -> return VehicleType.DRONE
        "B7" -> return VehicleType.SPACE
    }
    val d = desc?.uppercase().orEmpty()
    val t = typeCode?.uppercase().orEmpty()
    return when {
        "HELICOPTER" in d || "ROTOR" in d || t.startsWith("EC") || t.startsWith("AS3") || t.startsWith("R44") || t.startsWith("R22") -> VehicleType.HELICOPTER
        "GLIDER" in d || t == "GLID" -> VehicleType.GLIDER
        "BALLOON" in d || "AIRSHIP" in d || t == "BALL" -> VehicleType.BALLOON
        "DRONE" in d || "UAV" in d -> VehicleType.DRONE
        t.isNotEmpty() -> VehicleType.AIRLINER.takeIf { t.startsWith("A3") || t.startsWith("B7") || t.startsWith("E1") || t.startsWith("CRJ") } ?: VehicleType.LIGHT
        else -> VehicleType.UNKNOWN
    }
}
