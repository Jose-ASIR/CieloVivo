package com.raiz.toc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raiz.toc.data.AppData
import com.raiz.toc.data.Content
import com.raiz.toc.data.ExposureStep
import com.raiz.toc.data.Progress
import com.raiz.toc.data.RitualOutcome
import com.raiz.toc.data.Theme

@Composable
fun LadderScreen(
    data: AppData,
    onAdd: (ExposureStep) -> Unit,
    onDelete: (Long) -> Unit,
    onExpose: (ExposureStep) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ExposureStep?>(null) }
    val steps = remember(data) { data.steps.sortedWith(compareByDescending<ExposureStep> { it.expectedDistress }.thenBy { it.id }) }
    val next = remember(data) { Progress.suggestNext(data) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Mi escalera", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Apunta situaciones que te disparan el TOC, de más fáciles a más difíciles. " +
                        "Arriba, lo más difícil; empieza por abajo (malestar 30-50). Un peldaño se consolida tras " +
                        "${Progress.SESSIONS_TO_CONSOLIDATE} prácticas sin ritual en las que ya no crees la predicción.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (steps.isEmpty()) {
                item {
                    SectionCard {
                        Text("Tu escalera está vacía", fontWeight = FontWeight.Bold)
                        Text("Pulsa \"Añadir peldaño\". Al elegir un tema verás ideas de ejemplo. Intenta tener entre 8 y 15 peldaños con malestares variados.")
                    }
                }
            }
            items(steps, key = { it.id }) { step ->
                StepRow(
                    step = step,
                    data = data,
                    isNext = step.id == next?.id,
                    onClick = { onExpose(step) },
                    onDelete = { toDelete = step },
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Añadir peldaño") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (adding) AddStepDialog(onDismiss = { adding = false }, onAdd = { onAdd(it); adding = false })

    toDelete?.let { step ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Borrar este peldaño?") },
            text = { Text("También se borrarán sus prácticas registradas.") },
            confirmButton = { TextButton(onClick = { onDelete(step.id); toDelete = null }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun StepRow(step: ExposureStep, data: AppData, isNext: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val sessions = Progress.sessionsFor(data, step.id)
    val resisted = sessions.count { it.ritualDone == RitualOutcome.RESISTED }
    val consolidated = Progress.isConsolidated(data, step)
    SectionCard(
        modifier = Modifier.clickable(onClick = onClick),
        container = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${step.expectedDistress}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(step.situation, style = MaterialTheme.typography.bodyLarge)
                Text("Sin: ${step.ritual}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    when {
                        consolidated -> "Consolidado"
                        isNext -> "Siguiente · $resisted/${Progress.SESSIONS_TO_CONSOLIDATE} prácticas sin ritual"
                        sessions.isEmpty() -> step.theme.label
                        else -> "$resisted/${Progress.SESSIONS_TO_CONSOLIDATE} prácticas sin ritual"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (consolidated) Icon(Icons.Filled.CheckCircle, "Consolidado", tint = MaterialTheme.colorScheme.primary)
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Borrar", Modifier.size(20.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddStepDialog(onDismiss: () -> Unit, onAdd: (ExposureStep) -> Unit) {
    var situation by remember { mutableStateOf("") }
    var ritual by remember { mutableStateOf("") }
    var distress by remember { mutableIntStateOf(40) }
    var theme by remember { mutableStateOf(Theme.OTHER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo peldaño") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tema", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Theme.entries.forEach {
                        FilterChip(selected = theme == it, onClick = { theme = it }, label = { Text(it.label) })
                    }
                }
                Content.stepIdeas[theme]?.let { ideas ->
                    Text("Ideas (toca para usar):", style = MaterialTheme.typography.labelMedium)
                    ideas.forEach { idea -> AssistChip(onClick = { situation = idea }, label = { Text(idea) }) }
                }
                OutlinedTextField(
                    value = situation, onValueChange = { situation = it },
                    label = { Text("Situación a la que te expondrás") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = ritual, onValueChange = { ritual = it },
                    label = { Text("Ritual que no harás") },
                    placeholder = { Text("Ej.: lavarme las manos, comprobar, rezar mentalmente") },
                    modifier = Modifier.fillMaxWidth(),
                )
                PercentSlider("Malestar que esperas (0-100)", distress, { distress = it }, distressHint(distress))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(ExposureStep(0, situation.trim(), ritual.trim(), distress, theme))
                },
                enabled = situation.isNotBlank() && ritual.isNotBlank(),
            ) { Text("Añadir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
