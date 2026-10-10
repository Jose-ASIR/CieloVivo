package com.cielovivo.radar.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.location.LocationManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cielovivo.radar.data.Aircraft
import com.cielovivo.radar.data.VehicleType
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.interpolate
import org.maplibre.android.style.expressions.Expression.linear
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.step
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.expressions.Expression.toBool
import org.maplibre.android.style.expressions.Expression.toColor
import org.maplibre.android.style.expressions.Expression.zoom
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconColor
import org.maplibre.android.style.layers.PropertyFactory.iconHaloColor
import org.maplibre.android.style.layers.PropertyFactory.iconHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textOptional
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/** Estilo vectorial gratuito y sin API key (OpenFreeMap, datos © OpenStreetMap). */
private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"
private const val SOURCE_ID = "aircraft-src"
private const val LAYER_ID = "aircraft-layer"

/** Controlador mínimo que la UI usa para mover la cámara desde fuera del mapa. */
class MapController {
    internal var map: MapLibreMap? = null

    fun flyTo(lat: Double, lon: Double, zoom: Double? = null) {
        val m = map ?: return
        val builder = CameraPosition.Builder().target(LatLng(lat, lon))
        zoom?.let { builder.zoom(it) }
        m.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()), 800)
    }

    @SuppressLint("MissingPermission")
    fun flyToMyLocation(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time } ?: return false
        flyTo(loc.latitude, loc.longitude, 8.5)
        return true
    }
}

@Composable
fun AircraftMap(
    aircraft: List<Aircraft>,
    selected: Aircraft?,
    following: Boolean,
    controller: MapController,
    onViewportChanged: (lat: Double, lon: Double, spanKm: Double) -> Unit,
    onAircraftClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onViewport by rememberUpdatedState(onViewportChanged)
    val onClick by rememberUpdatedState(onAircraftClick)
    var style by remember { mutableStateOf<Style?>(null) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            controller.map = null
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            controller.map = map
            map.cameraPosition = CameraPosition.Builder().target(LatLng(40.4168, -3.7038)).zoom(6.0).build()
            map.uiSettings.isRotateGesturesEnabled = true
            map.uiSettings.isCompassEnabled = true

            map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { loaded ->
                setupAircraftLayer(context, loaded)
                style = loaded
                reportViewport(map, onViewport)
            }
            map.addOnCameraIdleListener { reportViewport(map, onViewport) }
            map.addOnMapClickListener { latLng ->
                val p = map.projection.toScreenLocation(latLng)
                val touch = 28f * context.resources.displayMetrics.density
                val hit = map.queryRenderedFeatures(RectF(p.x - touch, p.y - touch, p.x + touch, p.y + touch), LAYER_ID)
                onClick(hit.firstOrNull()?.getStringProperty("hex"))
                true
            }
        }
    }

    // Cada fotograma del ViewModel (≈2 por segundo) se vuelca como un único GeoJSON: MapLibre
    // lo renderiza en GPU, de modo que miles de aeronaves se mueven sin bloquear la UI.
    LaunchedEffect(style, aircraft, selected?.hex) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(SOURCE_ID)?.setGeoJson(aircraft.toFeatureCollection(selected?.hex))
    }

    LaunchedEffect(following, selected?.lat, selected?.lon) {
        if (following && selected != null) {
            controller.map?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(selected.lat, selected.lon)), 450)
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun reportViewport(map: MapLibreMap, cb: (Double, Double, Double) -> Unit) {
    val bounds = map.projection.visibleRegion.latLngBounds
    val center = map.cameraPosition.target ?: return
    val spanKm = bounds.northEast.distanceTo(bounds.southWest) / 1000.0
    cb(center.latitude, center.longitude, spanKm)
}

private fun setupAircraftLayer(context: Context, style: Style) {
    // Los iconos son blancos y se registran como SDF para poder teñirlos por tipo con iconColor.
    VehicleType.entries.map { it.icon }.distinct().forEach { name ->
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        ContextCompat.getDrawable(context, resId)?.let { style.addImage(name, it.toBitmap(), true) }
    }
    style.addSource(GeoJsonSource(SOURCE_ID))
    style.addLayer(
        SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
            iconImage(get("icon")),
            iconRotate(get("track")),
            iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
            iconColor(toColor(get("color"))),
            iconSize(interpolate(linear(), zoom(), stop(2, 0.45f), stop(7, 0.8f), stop(12, 1.1f))),
            iconHaloColor("#FFFFFFFF"),
            iconHaloWidth(switchCase(toBool(get("sel")), literal(2.5f), literal(0f))),
            // Etiquetas (indicativo + altitud) solo a partir de zoom 8 para no saturar.
            textField(step(zoom(), literal(""), stop(8, get("label")))),
            textSize(11f),
            textColor("#FFE2E8F0"),
            textHaloColor("#CC0B1220"),
            textHaloWidth(1.5f),
            textOffset(arrayOf(0f, 1.6f)),
            textAnchor(Property.TEXT_ANCHOR_TOP),
            textOptional(true),
        ),
    )
}

private fun List<Aircraft>.toFeatureCollection(selectedHex: String?): FeatureCollection =
    FeatureCollection.fromFeatures(map { a ->
        Feature.fromGeometry(Point.fromLngLat(a.lon, a.lat)).apply {
            addStringProperty("hex", a.hex)
            addStringProperty("icon", a.vehicleType.icon)
            addStringProperty("color", if (a.emergency) "#FF0000" else a.vehicleType.color.toHexColor())
            addNumberProperty("track", a.trackDeg ?: 0.0)
            addBooleanProperty("sel", a.hex == selectedHex)
            addStringProperty("label", buildString {
                append(a.displayName)
                a.altitudeFt?.let { append("\n").append(formatFlightLevel(it)) }
            })
        }
    })

/** ARGB → "#RRGGBB" (formato CSS que entiende la expresión to-color de MapLibre). */
private fun Long.toHexColor() = "#%06X".format(this and 0xFFFFFF)

internal fun formatFlightLevel(ft: Int) = if (ft >= 10_000) "FL%03d".format(ft / 100) else "$ft ft"
