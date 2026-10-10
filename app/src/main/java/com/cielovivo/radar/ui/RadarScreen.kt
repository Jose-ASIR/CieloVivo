package com.cielovivo.radar.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cielovivo.radar.data.AdsbApi
import com.cielovivo.radar.data.Aircraft
import com.cielovivo.radar.data.VehicleType
import kotlin.math.roundToInt

private val Panel = Color(0xE60F172A)

@Composable
fun RadarScreen(vm: RadarViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val controller = remember { MapController() }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it } && !controller.flyToMyLocation(context)) {
            Toast.makeText(context, "Ubicación aún no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AircraftMap(
            aircraft = state.aircraft,
            selected = state.selected,
            following = state.following,
            controller = controller,
            onViewportChanged = vm::onViewportChanged,
            onAircraftClick = vm::select,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            Modifier
                .statusBarsPadding()
                .padding(top = 8.dp)
                .fillMaxWidth(),
        ) {
            TopBar(state, vm)
            Spacer(Modifier.height(6.dp))
            TypeFilters(state.enabledTypes, state.aircraft, vm::toggleType)
        }

        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SmallFloatingActionButton(onClick = {
                locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }) { Icon(Icons.Default.MyLocation, "Mi ubicación") }
            LayersMenu(state, vm)
        }

        AnimatedVisibility(
            visible = state.selected != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            state.selected?.let {
                AircraftDetails(
                    aircraft = it,
                    following = state.following,
                    onFollow = vm::setFollowing,
                    onClose = { vm.select(null) },
                )
            }
        }

        if (state.selected == null) {
            Text(
                "Datos ADS-B: ${state.provider.label} (ODbL) · Mapa © OpenStreetMap",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun TopBar(state: RadarState, vm: RadarViewModel) {
    Row(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            placeholder = { Text("Vuelo, matrícula, tipo…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Default.Close, "Borrar") }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Panel),
        )
        Spacer(Modifier.width(8.dp))
        LiveBadge(state)
    }
}

@Composable
private fun LiveBadge(state: RadarState) {
    val stale = state.lastUpdateMs == 0L || System.currentTimeMillis() - state.lastUpdateMs > 15_000
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (stale) Color(0xFFF59E0B) else Color(0xFF22C55E)),
            )
            Spacer(Modifier.width(6.dp))
            Text(if (stale) "CONECTANDO" else "EN DIRECTO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text("${state.aircraft.size} en vuelo", color = Color(0xFFCBD5E1), fontSize = 12.sp)
        state.error?.let { Text(it, color = Color(0xFFFCA5A5), fontSize = 9.sp, maxLines = 2) }
    }
}

@Composable
private fun TypeFilters(enabled: Set<VehicleType>, visible: List<Aircraft>, onToggle: (VehicleType) -> Unit) {
    val counts = remember(visible) { visible.groupingBy { it.vehicleType }.eachCount() }
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(VehicleType.entries) { type ->
            val on = type in enabled
            FilterChip(
                selected = on,
                onClick = { onToggle(type) },
                label = { Text(if (on) "${type.label} ${counts[type] ?: 0}" else type.label, fontSize = 12.sp) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(type.color)),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Panel,
                    selectedContainerColor = Color(0xFF1E293B),
                    labelColor = Color(0xFF64748B),
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun LayersMenu(state: RadarState, vm: RadarViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        SmallFloatingActionButton(onClick = { open = true }) { Icon(Icons.Default.Layers, "Capas") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Mostrar aeronaves en tierra") },
                trailingIcon = { Switch(checked = state.showOnGround, onCheckedChange = vm::setShowOnGround) },
                onClick = { vm.setShowOnGround(!state.showOnGround) },
            )
            HorizontalDivider()
            Text("Fuente de datos", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(12.dp, 8.dp))
            AdsbApi.Provider.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text((if (p == state.provider) "● " else "○ ") + p.label) },
                    onClick = { vm.setProvider(p); open = false },
                )
            }
        }
    }
}

@Composable
private fun AircraftDetails(aircraft: Aircraft, following: Boolean, onFollow: (Boolean) -> Unit, onClose: () -> Unit) {
    val a = aircraft
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(a.vehicleType.color)),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.displayName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(a.description ?: a.typeCode, a.vehicleType.label).joinToString(" · "),
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                    )
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Cerrar", tint = Color.White) }
            }
            if (a.emergency) {
                Text(
                    "⚠ EMERGENCIA${a.squawk?.let { " (squawk $it)" } ?: ""}",
                    color = Color(0xFFF87171),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Altitud", a.altitudeFt?.let { "${formatFlightLevel(it)}\n${(it * 0.3048).roundToInt()} m" } ?: if (a.onGround) "En tierra" else "—")
                Metric("Velocidad", a.groundSpeedKt?.let { "${it.roundToInt()} kt\n${(it * 1.852).roundToInt()} km/h" } ?: "—")
                Metric("Rumbo", a.trackDeg?.let { "${it.roundToInt()}°\n${cardinal(it)}" } ?: "—")
                Metric("V/S", a.verticalRateFpm?.let { "${if (it > 0) "+" else ""}$it\nft/min" } ?: "—")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                listOfNotNull(
                    a.registration?.let { "Matrícula $it" },
                    a.operator,
                    "ICAO ${a.hex.uppercase()}",
                    a.squawk?.let { "Squawk $it" },
                ).joinToString("  ·  "),
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onFollow(!following) }) {
                Icon(Icons.Default.FlightTakeoff, null)
                Spacer(Modifier.width(6.dp))
                Text(if (following) "Dejar de seguir" else "Seguir en el mapa")
            }
        }
    }
}

@Composable
private fun Metric(title: String, value: String) {
    Column {
        Text(title.uppercase(), color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 14.sp, lineHeight = 17.sp)
    }
}

private fun cardinal(deg: Double): String {
    val dirs = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    return dirs[(((deg % 360) + 360) % 360 / 45.0).roundToInt() % 8]
}
