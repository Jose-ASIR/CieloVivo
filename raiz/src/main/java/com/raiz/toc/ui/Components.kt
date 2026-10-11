package com.raiz.toc.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/** Deslizador 0-100 con la etiqueta y el valor actual. */
@Composable
fun PercentSlider(label: String, value: Int, onChange: (Int) -> Unit, hint: String? = null) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..100f,
            steps = 19,
        )
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun distressHint(v: Int): String = when {
    v < 20 -> "Tranquilo"
    v < 40 -> "Algo de malestar"
    v < 60 -> "Malestar claro, pero manejable"
    v < 80 -> "Mucho malestar"
    else -> "Malestar extremo"
}

/** Gráfica de líneas sencilla, eje Y fijo 0..[max]. */
@Composable
fun LineChart(
    series: List<List<Int>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    max: Int = 100,
) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        for (i in 0..4) {
            val y = size.height * i / 4f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        series.forEachIndexed { idx, points ->
            if (points.isEmpty()) return@forEachIndexed
            val color = colors[idx % colors.size]
            val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
            fun pos(i: Int) = Offset(
                if (points.size > 1) i * stepX else size.width / 2,
                size.height * (1f - points[i].coerceIn(0, max) / max.toFloat()),
            )
            val path = Path().apply {
                moveTo(pos(0).x, pos(0).y)
                for (i in 1 until points.size) lineTo(pos(i).x, pos(i).y)
            }
            drawPath(path, color, style = Stroke(width = 5f))
            points.indices.forEach { drawCircle(color, 7f, pos(it)) }
        }
    }
}

@Composable
fun Legend(items: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Text(" $label", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Círculo que crece (inspira 4 s) y se encoge (suelta 6 s). */
@Composable
fun BreathingCircle(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "respiracion")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10_000, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    val inhale = t < 0.4f
    val scale = if (inhale) 0.55f + 0.45f * (t / 0.4f) else 1f - 0.45f * ((t - 0.4f) / 0.6f)
    Box(modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(180.dp).scale(scale).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        )
        Text(
            if (inhale) "Inspira" else "Suelta",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
fun CrisisDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Phone, null) },
        title = { Text("Si lo estás pasando muy mal") },
        text = {
            Text(
                "Si tienes pensamientos de hacerte daño o sientes que no puedes más, no estás solo:\n\n" +
                    "• 024: línea de atención a la conducta suicida (España, gratuita, 24 h).\n" +
                    "• 112: emergencias.\n" +
                    "• Tu médico de cabecera puede derivarte a salud mental.\n\n" +
                    "Ojo: los pensamientos intrusivos del TOC sobre hacer daño (que te horrorizan y rechazas) " +
                    "son distintos de las ganas reales de hacerte daño. Si dudas, consúltalo con un profesional.",
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } },
    )
}

private val dateFmt = SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("es-ES"))
fun formatDate(ms: Long): String = dateFmt.format(Date(ms))

fun formatClock(totalSec: Int): String = "%d:%02d".format(totalSec / 60, totalSec % 60)
