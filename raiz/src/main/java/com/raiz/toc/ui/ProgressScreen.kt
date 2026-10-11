package com.raiz.toc.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raiz.toc.data.AppData
import com.raiz.toc.data.Content
import com.raiz.toc.data.Progress
import com.raiz.toc.data.RitualOutcome
import com.raiz.toc.data.WeeklyCheck

@Composable
fun ProgressScreen(data: AppData, onCheck: () -> Unit, onReset: () -> Unit) {
    val now = remember(data) { RaizViewModel.now() }
    val week = remember(data) { Progress.lastWeek(data, now) }
    val checks = remember(data) { data.checks.sortedBy { it.at } }
    val sessions = remember(data) { data.sessions.sortedBy { it.startedAt } }
    var confirmReset by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Progreso", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

        item {
            SectionCard {
                Text("Escalera", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val total = data.steps.size
                val done = Progress.consolidatedCount(data)
                Text("$done de $total peldaños consolidados")
                if (total > 0) {
                    LinearProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item {
            SectionCard {
                Text("Última semana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Exposiciones", "${week.exposures}")
                    Stat("Predicciones\ndesmentidas", "${week.predictionsDisconfirmed}")
                    Stat("Impulsos\nfrenados", week.resistRate?.let { "${(it * 100).toInt()} %" } ?: "–")
                }
            }
        }

        item {
            SectionCard {
                Text("Chequeo semanal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (checks.isEmpty()) {
                    Text("Aún no has hecho ninguno. Hazlo hoy para tener un punto de partida.")
                } else {
                    val last = checks.last()
                    Text("Último: ${last.total}/40 · ${Progress.severityLabel(last.total)} (${formatDate(last.at)})")
                    Progress.checkTrend(data)?.let { diff ->
                        Text(
                            when {
                                diff < 0 -> "Has bajado ${-diff} puntos desde el primero. ¡Sigue así!"
                                diff > 0 -> "Has subido $diff puntos desde el primero. Los altibajos son normales; si persiste, coméntalo con un profesional."
                                else -> "Sin cambios desde el primero."
                            },
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (checks.size > 1) LineChart(listOf(checks.map { it.total }), listOf(MaterialTheme.colorScheme.primary), max = 40)
                }
                OutlinedButton(onClick = onCheck) { Text("Hacer chequeo") }
                Text("Orientativo: no es un diagnóstico ni sustituye a una evaluación profesional.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (sessions.size > 1) {
            item {
                SectionCard {
                    Text("Creencia en tus predicciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Antes y después de cada exposición. Cuando la línea de \"después\" baja, el cerebro está aprendiendo.",
                        style = MaterialTheme.typography.bodySmall)
                    val recent = sessions.takeLast(15)
                    val before = MaterialTheme.colorScheme.secondary
                    val after = MaterialTheme.colorScheme.primary
                    LineChart(listOf(recent.map { it.predictionBelief }, recent.map { it.beliefAfter }), listOf(before, after))
                    Legend(listOf("Antes" to before, "Después" to after))
                }
            }
        }

        if (sessions.isNotEmpty()) {
            item { Text("Lo que has aprendido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(sessions.reversed().take(20), key = { it.id }) { s ->
                val step = data.steps.firstOrNull { it.id == s.stepId }
                SectionCard {
                    Text(formatDate(s.startedAt), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    step?.let { Text(it.situation, fontWeight = FontWeight.Bold) }
                    Text("Predicción: ${s.prediction} (${s.predictionBelief} % → ${s.beliefAfter} %)")
                    Text("${if (s.predictionHappened) "Ocurrió" else "No ocurrió"} · ${s.ritualDone.label} · máx. ${s.peakDistress}",
                        style = MaterialTheme.typography.bodySmall)
                    if (s.learning.isNotBlank()) Text("\"${s.learning}\"", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        val urges = data.urges.sortedByDescending { it.at }.take(10)
        if (urges.isNotEmpty()) {
            item { Text("Impulsos recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(urges, key = { it.id }) { u ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(u.trigger.ifBlank { "Sin descripción" })
                        Text("${formatDate(u.at)} · intensidad ${u.intensity} · aplazado ${u.delayedMin} min",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        when (u.outcome) {
                            RitualOutcome.RESISTED -> "Resistido"
                            RitualOutcome.PARTIAL -> "Recortado"
                            RitualOutcome.DONE -> "Hecho"
                        },
                        color = if (u.outcome == RitualOutcome.DONE) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item {
            TextButton(onClick = { confirmReset = true }) { Text("Borrar todos mis datos") }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Borrar todo?") },
            text = { Text("Se borrarán tu escalera, tus prácticas, tus impulsos y tus chequeos. No se puede deshacer.") },
            confirmButton = { TextButton(onClick = { onReset(); confirmReset = false }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
        )
    }
}

/** Diez preguntas: las cinco dimensiones, primero para obsesiones y luego para compulsiones. */
@Composable
fun CheckFlow(onSave: (WeeklyCheck) -> Unit, onClose: () -> Unit) {
    val questions = remember {
        listOf("las obsesiones (pensamientos intrusivos)", "los rituales o compulsiones (también los mentales)")
            .flatMap { subject -> Content.checkQuestions.map { (q, options) -> q.format(subject) to options } }
    }
    val answers = remember { mutableStateListOf<Int>() }
    var index by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<WeeklyCheck?>(null) }

    BackHandler {
        if (index > 0 && result == null) { index--; answers.removeAt(answers.lastIndex) } else onClose()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Chequeo semanal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Cerrar") }
        }
        val r = result
        if (r == null) {
            Text("Piensa en los últimos 7 días.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { index / questions.size.toFloat() }, modifier = Modifier.fillMaxWidth())
            val (question, options) = questions[index]
            Text("${index + 1}/${questions.size}. $question", style = MaterialTheme.typography.titleMedium)
            options.forEachIndexed { value, label ->
                val pick: () -> Unit = {
                    answers.add(value)
                    if (index == questions.lastIndex) {
                        val check = WeeklyCheck(RaizViewModel.now(), answers.take(5), answers.drop(5))
                        onSave(check)
                        result = check
                    } else {
                        index++
                    }
                }
                Row(
                    Modifier.fillMaxWidth().selectable(selected = false, onClick = pick).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = false, onClick = pick)
                    Text(label)
                }
            }
        } else {
            SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                Text("${r.total}/40", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("Nivel orientativo: ${Progress.severityLabel(r.total)}")
                Text("Obsesiones ${r.obsessions.sum()}/20 · Compulsiones ${r.compulsions.sum()}/20")
            }
            Text(
                "Lo importante es la tendencia semana a semana, no una cifra suelta. " +
                    "Si la puntuación es alta (24 o más) o el TOC te impide hacer tu vida, busca ayuda profesional: " +
                    "la EPR con un terapeuta y/o la medicación funcionan.",
            )
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Hecho") }
        }
    }
}
