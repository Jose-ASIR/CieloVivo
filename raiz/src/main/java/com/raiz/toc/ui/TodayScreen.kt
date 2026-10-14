package com.raiz.toc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raiz.toc.data.AppData
import com.raiz.toc.data.Content
import com.raiz.toc.data.ExposureStep
import com.raiz.toc.data.Progress
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TodayScreen(
    data: AppData,
    onUrge: () -> Unit,
    onExpose: (ExposureStep) -> Unit,
    onCheck: () -> Unit,
    onOpenLadder: () -> Unit,
    onCrisis: () -> Unit,
    aiEnabled: Boolean = false,
    onCoach: () -> Unit = {},
    onRitualPlan: () -> Unit = {},
    onAiSettings: () -> Unit = {},
) {
    val now = remember(data) { RaizViewModel.now() }
    val week = remember(data) { Progress.lastWeek(data, now) }
    val streak = remember(data) { Progress.streakDays(data, now, ZoneId.systemDefault()) }
    val next = remember(data) { Progress.suggestNext(data) }
    val phrase = remember { Content.uncertaintyPhrases[LocalDate.now().dayOfYear % Content.uncertaintyPhrases.size] }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hoy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onAiSettings) { Icon(Icons.Filled.Settings, "Ajustes de IA") }
                IconButton(onClick = onCrisis) { Icon(Icons.Filled.Phone, "Ayuda urgente") }
            }
        }
        item {
            Button(
                onClick = onUrge,
                modifier = Modifier.fillMaxWidth().height(72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) {
                Icon(Icons.Filled.Bolt, null)
                Spacer(Modifier.width(8.dp))
                Text("Tengo un impulso ahora", style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            SectionCard(container = MaterialTheme.colorScheme.tertiaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null)
                    Text("  Acompañante IA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (aiEnabled) {
                    Text("Te ayuda a aplazar y recortar rituales, y a perder el miedo a no hacerlos. No te dará certezas: te ayudará a vivir con la duda.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onCoach) { Text("Hablar ahora") }
                        OutlinedButton(onClick = onRitualPlan) { Text("Soltar un ritual") }
                    }
                } else {
                    Text("Opcional: activa una IA que te acompañe en los momentos difíciles y te ayude a planificar cómo soltar tus rituales.")
                    OutlinedButton(onClick = onAiSettings) { Text("Activar") }
                }
            }
        }
        item {
            SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                Text("Frase del día", style = MaterialTheme.typography.labelLarge)
                Text("\"$phrase\"", style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic)
            }
        }
        item {
            SectionCard {
                Text("Siguiente práctica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                when {
                    data.steps.isEmpty() -> {
                        Text("Aún no tienes escalera. Empieza apuntando 5-10 situaciones que te disparen el TOC y el ritual que harías.")
                        FilledTonalButton(onClick = onOpenLadder) { Text("Crear mi escalera") }
                    }
                    next == null -> {
                        Text("Has consolidado todos tus peldaños. Añade otros más difíciles o repite alguno en un contexto nuevo para mantener lo aprendido.")
                        FilledTonalButton(onClick = onOpenLadder) { Text("Ir a la escalera") }
                    }
                    else -> {
                        Text(next.situation, style = MaterialTheme.typography.bodyLarge)
                        Text("Sin: ${next.ritual} · Malestar esperado ${next.expectedDistress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { onExpose(next) }) {
                            Icon(Icons.Filled.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Empezar exposición")
                        }
                    }
                }
                if (Progress.shouldVary(data)) {
                    Text("Consejo: tus últimas prácticas han sido iguales. Prueba otro lugar, otra hora u otro peldaño; variar hace que lo aprendido dure más.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
        if (Progress.weeklyCheckDue(data, now)) {
            item {
                SectionCard(container = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text("Chequeo semanal", fontWeight = FontWeight.Bold)
                    Text("10 preguntas rápidas para ver cómo evoluciona el TOC semana a semana.")
                    OutlinedButton(onClick = onCheck) { Text("Hacerlo ahora") }
                }
            }
        }
        item {
            SectionCard {
                Text("Últimos 7 días", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Exposiciones", "${week.exposures}")
                    Stat("Impulsos frenados", "${week.urgesResisted + week.urgesReduced}/${week.urges}")
                    Stat("Min. aplazados", "${week.minutesDelayed}")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        if (streak == 0) " Practica hoy para empezar una racha"
                        else " $streak ${if (streak == 1) "día" else "días"} seguidos practicando",
                    )
                }
            }
        }
        item {
            Text(
                "Recuerda: el objetivo no es dejar de sentir ansiedad, sino dejar de obedecerla.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
