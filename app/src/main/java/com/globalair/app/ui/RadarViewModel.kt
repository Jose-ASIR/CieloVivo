package com.globalair.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.globalair.app.data.AdsbApi
import com.globalair.app.data.Aircraft
import com.globalair.app.data.VehicleType
import com.globalair.app.data.RateLimitedException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.ceil

data class Viewport(val lat: Double, val lon: Double, val radiusNm: Int)

data class RadarState(
    /** Aeronaves visibles (ya filtradas y extrapoladas al instante actual). */
    val aircraft: List<Aircraft> = emptyList(),
    val totalReceived: Int = 0,
    val enabledTypes: Set<VehicleType> = VehicleType.entries.toSet(),
    val showOnGround: Boolean = false,
    val query: String = "",
    val selected: Aircraft? = null,
    val following: Boolean = false,
    /** Fuente preferida por el usuario. */
    val provider: AdsbApi.Provider = AdsbApi.Provider.ADSB_LOL,
    /** Fuente que dio los últimos datos (puede ser la de reserva). */
    val activeProvider: AdsbApi.Provider = AdsbApi.Provider.ADSB_LOL,
    val lastUpdateMs: Long = 0,
    val error: String? = null,
    val loading: Boolean = true,
)

/**
 * Orquesta el "directo":
 *  - Consulta la API cada [POLL_INTERVAL_MS] para la zona visible del mapa (nunca más de una
 *    petición cada [MIN_GAP_MS]).
 *  - Cada [FRAME_INTERVAL_MS] extrapola las posiciones para que los iconos se desplacen
 *    de forma continua (sensación de tiempo real aunque los datos lleguen cada pocos segundos).
 *  - Si la fuente elegida falla, prueba la de reserva en esa misma consulta.
 */
class RadarViewModel(private val api: AdsbApi = AdsbApi()) : ViewModel() {

    private val _state = MutableStateFlow(RadarState())
    val state: StateFlow<RadarState> = _state.asStateFlow()

    private var raw: Map<String, Aircraft> = emptyMap()
    @Volatile private var viewport: Viewport? = null
    /** Despierta el bucle de consulta antes de tiempo (al mover el mapa o cambiar de fuente). */
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var lastRequestMs = 0L
    private var lastSuccessMs = 0L

    init {
        viewModelScope.launch {
            while (isActive) {
                publish()
                delay(FRAME_INTERVAL_MS)
            }
        }
        viewModelScope.launch { pollLoop() }
    }

    /** Llamado cuando la cámara del mapa se detiene. [spanKm] = diagonal visible aproximada. */
    fun onViewportChanged(lat: Double, lon: Double, spanKm: Double) {
        val radius = ceil(spanKm / 2 / 1.852).toInt().coerceIn(5, AdsbApi.MAX_RADIUS_NM)
        val newVp = Viewport(lat, lon, radius)
        if (viewport != newVp) {
            viewport = newVp
            wake.trySend(Unit)
        }
    }

    /**
     * Un único bucle hace todas las peticiones, así nunca se solapan. Entre dos peticiones pasan
     * al menos [MIN_GAP_MS] aunque el usuario mueva el mapa sin parar: las APIs devuelven 429
     * si se les pregunta más de ~1 vez por segundo.
     */
    private suspend fun pollLoop() {
        var waitMs = 0L
        while (currentCoroutineContext().isActive) {
            withTimeoutOrNull(waitMs) { wake.receive() }
            val sinceLast = System.currentTimeMillis() - lastRequestMs
            if (sinceLast < MIN_GAP_MS) delay(MIN_GAP_MS - sinceLast)
            waitMs = if (viewport == null) POLL_INTERVAL_MS else fetchOnce()
        }
    }

    /** Hace una consulta (con la fuente de reserva si falla la principal) y devuelve cuánto esperar. */
    private suspend fun fetchOnce(): Long {
        val vp = viewport ?: return POLL_INTERVAL_MS
        val primary = _state.value.provider
        val providers = listOf(primary) + AdsbApi.Provider.entries.filter { it != primary }
        var backoffMs = POLL_INTERVAL_MS
        var lastError: Exception? = null
        lastRequestMs = System.currentTimeMillis()
        for (provider in providers) {
            try {
                val list = api.aircraftAround(provider, vp.lat, vp.lon, vp.radiusNm)
                raw = list.associateBy { it.hex }
                lastSuccessMs = System.currentTimeMillis()
                _state.update { it.copy(lastUpdateMs = lastSuccessMs, error = null, loading = false, activeProvider = provider) }
                return POLL_INTERVAL_MS
            } catch (e: CancellationException) {
                throw e
            } catch (e: RateLimitedException) {
                lastError = e
                backoffMs = maxOf(backoffMs, (e.retryAfterSec ?: 0) * 1000, RATE_LIMIT_BACKOFF_MS)
            } catch (e: Exception) {
                lastError = e
            }
        }
        // Solo se avisa si llevamos un rato sin datos: un fallo suelto no se nota gracias a la extrapolación.
        if (System.currentTimeMillis() - lastSuccessMs > STALE_MS) {
            val reason = when (lastError) {
                is RateLimitedException -> "servidores saturados, reintentando…"
                is java.net.UnknownHostException -> "sin conexión a Internet"
                is java.net.SocketTimeoutException -> "la red va lenta, reintentando…"
                else -> "error de datos (${lastError?.message ?: "desconocido"})"
            }
            _state.update { it.copy(error = reason, loading = false) }
        }
        return backoffMs
    }

    private fun publish() {
        val now = System.currentTimeMillis()
        _state.update { s ->
            val q = s.query.trim().uppercase()
            val visible = raw.values.asSequence()
                .filter { it.vehicleType in s.enabledTypes }
                .filter { s.showOnGround || !it.onGround }
                .filter { q.isEmpty() || matches(it, q) }
                .map { it.extrapolatedTo(now) }
                .toList()
            val selected = s.selected?.let { sel -> raw[sel.hex]?.extrapolatedTo(now) ?: sel }
            s.copy(aircraft = visible, totalReceived = raw.size, selected = selected)
        }
    }

    private fun matches(a: Aircraft, q: String) =
        listOfNotNull(a.callsign, a.registration, a.typeCode, a.hex, a.operator).any { q in it.uppercase() }

    fun toggleType(type: VehicleType) = _state.update {
        val set = if (type in it.enabledTypes) it.enabledTypes - type else it.enabledTypes + type
        it.copy(enabledTypes = set)
    }

    fun setShowOnGround(show: Boolean) = _state.update { it.copy(showOnGround = show) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun select(hex: String?) = _state.update {
        it.copy(selected = hex?.let { h -> raw[h] }, following = it.following && hex != null)
    }
    fun setFollowing(follow: Boolean) = _state.update { it.copy(following = follow) }
    fun setProvider(p: AdsbApi.Provider) {
        _state.update { it.copy(provider = p) }
        wake.trySend(Unit)
    }

    companion object {
        const val POLL_INTERVAL_MS = 5_000L
        const val MIN_GAP_MS = 2_500L
        const val RATE_LIMIT_BACKOFF_MS = 10_000L
        const val STALE_MS = 15_000L
        const val FRAME_INTERVAL_MS = 500L
    }
}
