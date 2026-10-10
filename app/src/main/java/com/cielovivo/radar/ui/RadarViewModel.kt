package com.cielovivo.radar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cielovivo.radar.data.AdsbApi
import com.cielovivo.radar.data.Aircraft
import com.cielovivo.radar.data.VehicleType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    val provider: AdsbApi.Provider = AdsbApi.Provider.ADSB_LOL,
    val lastUpdateMs: Long = 0,
    val error: String? = null,
    val loading: Boolean = true,
)

/**
 * Orquesta el "directo":
 *  - Consulta la API cada [POLL_INTERVAL_MS] para la zona visible del mapa.
 *  - Cada [FRAME_INTERVAL_MS] extrapola las posiciones para que los iconos se desplacen
 *    de forma continua (sensación de tiempo real aunque los datos lleguen cada pocos segundos).
 *  - Si un proveedor falla, conmuta automáticamente al otro.
 */
class RadarViewModel(private val api: AdsbApi = AdsbApi()) : ViewModel() {

    private val _state = MutableStateFlow(RadarState())
    val state: StateFlow<RadarState> = _state.asStateFlow()

    private var raw: Map<String, Aircraft> = emptyMap()
    private var viewport: Viewport? = null
    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                publish()
                delay(FRAME_INTERVAL_MS)
            }
        }
    }

    /** Llamado cuando la cámara del mapa se detiene. [spanKm] = diagonal visible aproximada. */
    fun onViewportChanged(lat: Double, lon: Double, spanKm: Double) {
        val radius = ceil(spanKm / 2 / 1.852).toInt().coerceIn(5, AdsbApi.MAX_RADIUS_NM)
        val newVp = Viewport(lat, lon, radius)
        val changed = viewport != newVp
        viewport = newVp
        if (changed) restartPolling()
    }

    private fun restartPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                fetchOnce()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun fetchOnce() {
        val vp = viewport ?: return
        val provider = _state.value.provider
        try {
            val list = api.aircraftAround(provider, vp.lat, vp.lon, vp.radiusNm)
            raw = list.associateBy { it.hex }
            _state.update { it.copy(lastUpdateMs = System.currentTimeMillis(), error = null, loading = false) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val fallback = AdsbApi.Provider.entries.first { it != provider }
            _state.update {
                it.copy(error = "Sin datos de ${provider.label}, probando ${fallback.label}…", provider = fallback, loading = false)
            }
        }
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
        restartPolling()
    }

    companion object {
        /** airplanes.live y adsb.lol piden ≤1 petición/s; 4 s equilibra frescura y cortesía. */
        const val POLL_INTERVAL_MS = 4_000L
        const val FRAME_INTERVAL_MS = 500L
    }
}
