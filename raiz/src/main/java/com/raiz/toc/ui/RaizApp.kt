package com.raiz.toc.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raiz.toc.data.AppData
import com.raiz.toc.data.ExposureStep

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Hoy", Icons.Filled.Home),
    LADDER("Escalera", Icons.Filled.Stairs),
    LEARN("Aprender", Icons.Filled.School),
    PROGRESS("Progreso", Icons.Filled.Insights),
}

/** Flujos a pantalla completa que se abren por encima de las pestañas. */
sealed interface Flow {
    data object Urge : Flow
    data class Exposure(val step: ExposureStep) : Flow
    data object Check : Flow
}

@Composable
fun RaizApp(vm: RaizViewModel = viewModel()) {
    val data by vm.data.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val d = data
        when {
            d == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            !d.acceptedIntro -> IntroScreen(onAccept = vm::acceptIntro)
            else -> MainScreen(d, vm)
        }
    }
}

@Composable
private fun MainScreen(data: AppData, vm: RaizViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var flow by remember { mutableStateOf<Flow?>(null) }
    var showCrisis by remember { mutableStateOf(false) }

    val current = flow
    if (current != null) {
        val close = { flow = null }
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (current) {
                Flow.Urge -> UrgeFlow(onSave = { vm.saveUrge(it) }, onClose = close)
                is Flow.Exposure -> ExposureFlow(
                    step = current.step,
                    onSave = { vm.saveSession(it) },
                    onClose = close,
                )
                Flow.Check -> CheckFlow(onSave = { vm.saveCheck(it) }, onClose = close)
            }
        }
        return
    }

    BackHandler(enabled = tab != Tab.TODAY) { tab = Tab.TODAY }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, null) },
                        label = { Text(it.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.TODAY -> TodayScreen(
                    data = data,
                    onUrge = { flow = Flow.Urge },
                    onExpose = { flow = Flow.Exposure(it) },
                    onCheck = { flow = Flow.Check },
                    onOpenLadder = { tab = Tab.LADDER },
                    onCrisis = { showCrisis = true },
                )
                Tab.LADDER -> LadderScreen(
                    data = data,
                    onAdd = vm::addStep,
                    onDelete = vm::deleteStep,
                    onExpose = { flow = Flow.Exposure(it) },
                )
                Tab.LEARN -> LearnScreen(onCrisis = { showCrisis = true })
                Tab.PROGRESS -> ProgressScreen(
                    data = data,
                    onCheck = { flow = Flow.Check },
                    onReset = vm::resetAll,
                )
            }
        }
    }
    if (showCrisis) CrisisDialog { showCrisis = false }
}

@Composable
private fun IntroScreen(onAccept: () -> Unit) {
    var understood by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Raíz", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text("Entrenamiento para salir del círculo del TOC", style = MaterialTheme.typography.titleMedium)
        Text(
            "El TOC no se mantiene por los pensamientos, sino por lo que hacemos para quitárnoslos de encima: " +
                "los rituales, las comprobaciones, las evitaciones y las preguntas para quedarnos tranquilos. " +
                "Cada ritual alivia un momento y enseña al cerebro que el peligro era real.",
        )
        Text(
            "Raíz te ayuda a romper ese círculo con la técnica que más evidencia tiene: la exposición con " +
                "prevención de respuesta (EPR). Construirás una escalera de retos, practicarás sin rituales, " +
                "aprenderás a aplazar y surfear los impulsos y verás tu progreso.",
        )
        SectionCard(container = MaterialTheme.colorScheme.secondaryContainer) {
            Text("Antes de empezar, con honestidad", fontWeight = FontWeight.Bold)
            Text(
                "• Ninguna app cura el TOC por sí sola. La EPR funciona mejor guiada por un profesional " +
                    "(psicólogo clínico o psiquiatra), a veces junto con medicación.\n" +
                    "• Úsala como complemento a la terapia o como primer paso mientras buscas ayuda.\n" +
                    "• Las exposiciones generan malestar a propósito. Si el TOC es muy intenso, empieza con un profesional.\n" +
                    "• Si tienes pensamientos de hacerte daño, llama al 024 o al 112.\n" +
                    "• Todo lo que escribas se queda en tu teléfono. La app no usa internet.",
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = understood, onCheckedChange = { understood = it })
            Text("Lo entiendo")
        }
        Button(onClick = onAccept, enabled = understood, modifier = Modifier.fillMaxWidth()) {
            Text("Empezar")
        }
    }
}
