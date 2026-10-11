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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raiz.toc.data.Content
import com.raiz.toc.data.RitualOutcome
import com.raiz.toc.data.UrgeLog
import kotlinx.coroutines.delay

private enum class UrgeStage { TRIGGER, WAIT, OUTCOME, DONE }

/**
 * Para cuando aprieta el impulso: se nombra, se aplaza el ritual con un temporizador,
 * se surfea con respiración y frases de aceptación, y se registra lo que pasó sin juzgar.
 */
@Composable
fun UrgeFlow(onSave: (UrgeLog) -> Unit, onClose: () -> Unit) {
    var stage by remember { mutableStateOf(UrgeStage.TRIGGER) }
    var trigger by remember { mutableStateOf("") }
    var intensity by remember { mutableIntStateOf(70) }
    var delayMin by remember { mutableIntStateOf(10) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf(RitualOutcome.RESISTED) }
    val startedAt = remember { RaizViewModel.now() }

    BackHandler {
        when (stage) {
            UrgeStage.WAIT -> stage = UrgeStage.OUTCOME
            else -> onClose()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Impulso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Cerrar") }
        }

        when (stage) {
            UrgeStage.TRIGGER -> {
                Text("Para un momento. Tener el impulso no te obliga a hacer el ritual.",
                    style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("¿Qué lo ha disparado? (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                PercentSlider("¿Cómo de fuerte es el impulso?", intensity, { intensity = it }, distressHint(intensity))
                Text("¿Cuánto vas a aplazar el ritual?", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { m ->
                        FilterChip(selected = delayMin == m, onClick = { delayMin = m }, label = { Text("$m min") })
                    }
                }
                Text("Si aplazarlo te parece imposible, empieza por 5 minutos. Cada minuto cuenta.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { stage = UrgeStage.WAIT }, modifier = Modifier.fillMaxWidth()) {
                    Text("Empezar a aplazar")
                }
            }

            UrgeStage.WAIT -> {
                val total = delayMin * 60
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1000)
                        elapsedSec++
                    }
                }
                val finished = elapsedSec >= total
                val guide = Content.urgeGuide[(elapsedSec / 20) % Content.urgeGuide.size]
                val phrase = Content.uncertaintyPhrases[(elapsedSec / 30) % Content.uncertaintyPhrases.size]

                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (finished) "¡Lo has aplazado ${formatClock(elapsedSec)}!"
                        else formatClock(total - elapsedSec),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        progress = { (elapsedSec.toFloat() / total).coerceAtMost(1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BreathingCircle()
                    Text(guide, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text("\"$phrase\"", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary)
                }
                if (finished) {
                    Text("¿Y ahora? Puedes seguir aplazando o comprobar si el impulso sigue igual de fuerte. Muchas veces ya ha bajado.",
                        style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = { stage = UrgeStage.OUTCOME }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (finished) "Terminar y registrar" else "El impulso ha bajado")
                }
                OutlinedButton(
                    onClick = { outcome = RitualOutcome.DONE; stage = UrgeStage.OUTCOME },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Voy a hacer el ritual") }
            }

            UrgeStage.OUTCOME -> {
                Text("¿Qué ha pasado al final?", style = MaterialTheme.typography.titleMedium)
                Text("Sé sincero: registrar también los rituales hechos te ayuda a ver patrones. Aquí no hay fracasos.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RitualOutcome.entries.forEach { o ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = outcome == o, onClick = { outcome = o }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = outcome == o, onClick = { outcome = o })
                        Text(o.label)
                    }
                }
                Button(
                    onClick = {
                        onSave(
                            UrgeLog(
                                id = 0,
                                at = startedAt,
                                trigger = trigger.trim(),
                                intensity = intensity,
                                delayedMin = elapsedSec / 60,
                                outcome = outcome,
                            ),
                        )
                        stage = UrgeStage.DONE
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar") }
            }

            UrgeStage.DONE -> {
                val message = when (outcome) {
                    RitualOutcome.RESISTED ->
                        "Muy bien. Acabas de enseñarle a tu cerebro que puedes tener el impulso sin obedecerlo, " +
                            "y que el malestar baja solo. Así se debilita el TOC."
                    RitualOutcome.PARTIAL ->
                        "Recortarlo o retrasarlo también cuenta: cada vez que el ritual es más corto, el TOC pierde un poco de poder. " +
                            "La próxima vez, prueba a aplazarlo un poco más."
                    RitualOutcome.DONE ->
                        "No pasa nada, es parte del proceso. Fíjate en cuánto duró el alivio y si el impulso volvió. " +
                            "Si aplazaste aunque sea un minuto, ya estás entrenando."
                }
                SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                }
                if (elapsedSec >= 60) {
                    Text("Tiempo aplazado: ${elapsedSec / 60} min", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
            }
        }
    }
}
