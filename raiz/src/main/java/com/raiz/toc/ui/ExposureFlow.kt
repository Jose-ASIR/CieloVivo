package com.raiz.toc.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raiz.toc.data.Content
import com.raiz.toc.data.ExposureSession
import com.raiz.toc.data.ExposureStep
import com.raiz.toc.data.RitualOutcome
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ExpStage { PREP, RUN, REVIEW, DONE }

/** Cada cuánto se anota automáticamente el malestar durante la práctica. */
private const val SAMPLE_EVERY_SEC = 60

/** Duración orientativa a partir de la cual se sugiere que ya se puede valorar la predicción. */
private const val SUGGESTED_MIN_SEC = 10 * 60

@Composable
fun ExposureFlow(
    step: ExposureStep,
    onSave: (ExposureSession) -> Unit,
    onClose: () -> Unit,
    suggestPrediction: (suspend () -> Result<String>)? = null,
    onCoach: ((String?) -> Unit)? = null,
) {
    var stage by remember { mutableStateOf(ExpStage.PREP) }
    var prediction by remember { mutableStateOf(step.prediction) }
    var suggesting by remember { mutableStateOf(false) }
    var suggestError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var belief by remember { mutableIntStateOf(70) }
    var distress by remember { mutableIntStateOf(step.expectedDistress) }
    val samples = remember { mutableStateListOf<Int>() }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var startedAt by remember { mutableStateOf(0L) }
    var outcome by remember { mutableStateOf(RitualOutcome.RESISTED) }
    var happened by remember { mutableStateOf<Boolean?>(null) }
    var beliefAfter by remember { mutableIntStateOf(30) }
    var learning by remember { mutableStateOf("") }
    var confirmExit by remember { mutableStateOf(false) }

    BackHandler { if (stage == ExpStage.PREP || stage == ExpStage.DONE) onClose() else confirmExit = true }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Exposición", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            IconButton(onClick = { if (stage == ExpStage.PREP || stage == ExpStage.DONE) onClose() else confirmExit = true }) {
                Icon(Icons.Filled.Close, "Cerrar")
            }
        }
        SectionCard {
            Text(step.situation, style = MaterialTheme.typography.titleMedium)
            Text("Ritual que no vas a hacer: ${step.ritual}", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when (stage) {
            ExpStage.PREP -> {
                Text("1. Tu predicción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = prediction,
                    onValueChange = { prediction = it },
                    label = { Text("¿Qué temes que pase si no haces el ritual?") },
                    placeholder = { Text("Ej.: me pondré enfermo / no podré soportar la ansiedad") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                if (suggestPrediction != null) {
                    TextButton(
                        onClick = {
                            suggesting = true
                            suggestError = null
                            scope.launch {
                                suggestPrediction().onSuccess { prediction = it }.onFailure { suggestError = it.message }
                                suggesting = false
                            }
                        },
                        enabled = !suggesting,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null)
                        Text(if (suggesting) "  Pensando…" else "  Ayúdame a formularla (IA)")
                    }
                    suggestError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
                PercentSlider("¿Cuánto te lo crees ahora mismo? (%)", belief, { belief = it })
                Text("2. Las reglas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Content.exposureRules.forEach { Text("• $it") }
                PercentSlider("Malestar ahora mismo (0-100)", distress, { distress = it }, distressHint(distress))
                Button(
                    onClick = {
                        startedAt = RaizViewModel.now()
                        samples.add(distress)
                        stage = ExpStage.RUN
                    },
                    enabled = prediction.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Empezar la exposición") }
                if (prediction.isBlank()) {
                    Text("Escribe una predicción concreta: es lo que vas a poner a prueba.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            ExpStage.RUN -> {
                val currentDistress by rememberUpdatedState(distress)
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1000)
                        elapsedSec++
                        if (elapsedSec % SAMPLE_EVERY_SEC == 0) samples.add(currentDistress)
                    }
                }
                Text(formatClock(elapsedSec), style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(
                    "Estás poniendo a prueba: \"$prediction\"",
                    style = MaterialTheme.typography.bodyLarge,
                )
                PercentSlider("¿Cuánto malestar sientes ahora?", distress, { distress = it }, distressHint(distress))
                Text("Se anota cada minuto. No hace falta que baje: lo importante es no hacer el ritual y ver qué pasa.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (samples.size > 1) {
                    LineChart(listOf(samples.toList()), listOf(MaterialTheme.colorScheme.secondary))
                }
                SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                    Text("\"${Content.uncertaintyPhrases[(elapsedSec / 45) % Content.uncertaintyPhrases.size]}\"")
                }
                if (elapsedSec < SUGGESTED_MIN_SEC) {
                    Text("Intenta seguir al menos hasta los ${SUGGESTED_MIN_SEC / 60} minutos, o hasta que puedas comprobar tu predicción.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (onCoach != null) {
                    FilledTonalButton(
                        onClick = {
                            onCoach(
                                "Estoy en una exposición: \"${step.situation}\", sin ${step.ritual}. " +
                                    "Mi malestar ahora es $distress/100 y tengo ganas de hacer el ritual. Ayúdame a aguantar.",
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Hablar con el acompañante IA") }
                }
                Button(
                    onClick = {
                        if (elapsedSec % SAMPLE_EVERY_SEC != 0) samples.add(distress)
                        stage = ExpStage.REVIEW
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Terminar y revisar") }
            }

            ExpStage.REVIEW -> {
                Text("¿Hiciste el ritual (o parte de él, o en la cabeza)?", style = MaterialTheme.typography.titleMedium)
                RitualOutcome.entries.forEach { o ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = outcome == o, onClick = { outcome = o }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = outcome == o, onClick = { outcome = o })
                        Text(o.label)
                    }
                }
                Text("Predijiste: \"$prediction\"", style = MaterialTheme.typography.bodyLarge)
                Text("¿Ha ocurrido?", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = happened == false, onClick = { happened = false }, label = { Text("No") })
                    FilterChip(selected = happened == true, onClick = { happened = true }, label = { Text("Sí") })
                }
                PercentSlider("¿Cuánto te la crees ahora? (antes: $belief %)", beliefAfter, { beliefAfter = it })
                OutlinedTextField(
                    value = learning,
                    onValueChange = { learning = it },
                    label = { Text("¿Qué has aprendido?") },
                    placeholder = { Text("Ej.: el malestar subió pero pude aguantarlo y no pasó nada") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Button(
                    onClick = {
                        onSave(
                            ExposureSession(
                                id = 0,
                                stepId = step.id,
                                startedAt = startedAt,
                                durationSec = elapsedSec,
                                prediction = prediction.trim(),
                                predictionBelief = belief,
                                distress = samples.toList(),
                                ritualDone = outcome,
                                predictionHappened = happened == true,
                                beliefAfter = beliefAfter,
                                learning = learning.trim(),
                            ),
                        )
                        stage = ExpStage.DONE
                    },
                    enabled = happened != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar") }
            }

            ExpStage.DONE -> {
                val peak = samples.maxOrNull() ?: 0
                SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Práctica guardada", fontWeight = FontWeight.Bold)
                    Text("Duración: ${formatClock(elapsedSec)} · Malestar máximo: $peak · Al final: ${samples.lastOrNull() ?: 0}")
                    Text("Creencia en la predicción: $belief % → $beliefAfter %")
                    Text(
                        when {
                            happened == true ->
                                "Lo temido ha pasado esta vez. Comenta con tu terapeuta si la predicción era realista " +
                                    "y si el peldaño necesita ajustes. Aun así, has demostrado que puedes afrontarlo."
                            outcome == RitualOutcome.DONE ->
                                "Has dado el paso de exponerte, y eso ya es aprendizaje. La próxima vez prueba un peldaño " +
                                    "un poco más fácil o retrasar el ritual."
                            beliefAfter < belief ->
                                "Tu predicción no se cumplió y ahora te la crees menos. Eso es exactamente lo que reentrena el cerebro."
                            else ->
                                "Has tolerado el malestar sin ritual. Repite este peldaño en otros contextos para afianzarlo."
                        },
                    )
                }
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("¿Salir sin guardar?") },
            text = { Text("Se perderá el registro de esta práctica.") },
            confirmButton = { TextButton(onClick = onClose) { Text("Salir") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Seguir") } },
        )
    }
}
