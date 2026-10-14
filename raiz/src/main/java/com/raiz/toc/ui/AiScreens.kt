package com.raiz.toc.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.raiz.toc.ai.AiModel
import com.raiz.toc.ai.StepPlan
import com.raiz.toc.data.ExposureStep
import kotlinx.coroutines.launch

/** Cabecera común de los flujos a pantalla completa. */
@Composable
fun FlowHeader(title: String, onClose: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f))
        actions()
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Cerrar") }
    }
}

// ---------------------------------------------------------------- Ajustes

@Composable
fun AiSettingsFlow(current: AiSettings, onSave: (AiSettings) -> Unit, onClose: () -> Unit) {
    var key by remember { mutableStateOf(current.apiKey) }
    var model by remember { mutableStateOf(current.model) }
    var share by remember { mutableStateOf(current.shareRecords) }
    BackHandler(onBack = onClose)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FlowHeader("Acompañante IA", onClose)
        Text(
            "La IA es opcional. Usa Claude, de Anthropic, con tu propia clave de API " +
                "(créala en console.anthropic.com; el uso se paga en tu cuenta de Anthropic).",
        )
        SectionCard(container = MaterialTheme.colorScheme.secondaryContainer) {
            Text("Cómo está pensada", fontWeight = FontWeight.Bold)
            Text(
                "• Te ayuda a recortar y aplazar rituales y a perder el miedo a no hacerlos.\n" +
                    "• No te tranquilizará sobre tus obsesiones ni comprobará cosas por ti: eso alimentaría el TOC.\n" +
                    "• Es un apoyo para practicar, no un terapeuta. Puede equivocarse.\n" +
                    "• Si estás en crisis, llama al 024 o al 112.",
            )
        }
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("Clave de API de Anthropic") },
            placeholder = { Text("sk-ant-...") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Modelo", style = MaterialTheme.typography.titleSmall)
        AiModel.entries.forEach { m ->
            Row(
                Modifier.fillMaxWidth().selectable(selected = model == m, onClick = { model = m }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = model == m, onClick = { model = m })
                Text(m.label)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Compartir mis registros con el acompañante")
                Text(
                    "Le envía un resumen de tu escalera, prácticas e impulsos para personalizar las respuestas. " +
                        "Si lo desactivas, solo ve lo que escribas en el chat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = share, onCheckedChange = { share = it })
        }
        Text(
            "Privacidad: lo que envías a la IA viaja a los servidores de Anthropic. La clave se guarda solo en este teléfono. " +
                "Sin clave, la app funciona igual y nada sale del teléfono.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { onSave(AiSettings(key, model, share)); onClose() }, modifier = Modifier.fillMaxWidth()) {
            Text("Guardar")
        }
        if (current.enabled) {
            TextButton(onClick = { onSave(AiSettings()); onClose() }, modifier = Modifier.fillMaxWidth()) {
                Text("Desactivar la IA y borrar la clave")
            }
        }
    }
}

/** Aviso para cuando se intenta usar la IA sin configurarla. */
@Composable
fun AiNotConfigured(onSetup: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FlowHeader("Acompañante IA", onClose)
        Text("Para usar la IA necesitas añadir tu clave de API de Anthropic en los ajustes.")
        Button(onClick = onSetup) { Text("Configurar la IA") }
    }
}

// ---------------------------------------------------------------- Chat

private val quickPrompts = listOf(
    "Tengo muchas ganas de hacer el ritual ahora",
    "Ayúdame a recortar mi ritual",
    "Me da miedo no hacerlo",
    "Necesito estar seguro de que todo está bien",
    "Quiero preparar una exposición",
)

@Composable
fun CoachChat(
    state: ChatState,
    seed: String?,
    onSend: (String) -> Unit,
    onDismissError: () -> Unit,
    onNewChat: () -> Unit,
    onCrisis: () -> Unit,
    onClose: () -> Unit,
) {
    var input by remember { mutableStateOf(seed.orEmpty()) }
    val listState = rememberLazyListState()
    BackHandler(onBack = onClose)

    LaunchedEffect(state.failedText) { state.failedText?.let { input = it } }
    val itemCount = state.turns.size + (if (state.streaming != null) 1 else 0)
    LaunchedEffect(itemCount, state.streaming?.length) {
        if (itemCount > 0) listState.scrollToItem(itemCount)
    }

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
        Box(Modifier.padding(top = 12.dp)) {
            FlowHeader("Acompañante", onClose) {
                IconButton(onClick = onNewChat, enabled = !state.busy) { Icon(Icons.Filled.Refresh, "Nueva conversación") }
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        "Cuéntame qué ritual te está pidiendo el TOC. Te ayudo a aplazarlo, recortarlo o soltarlo, " +
                            "y a tolerar la duda. No te daré certezas: eso alimentaría el TOC.",
                    )
                    Text(
                        "Es una IA: puede equivocarse y no sustituye a tu terapeuta.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            items(state.turns) { Bubble(it.text, it.fromUser) }
            state.streaming?.let { partial ->
                item {
                    if (partial.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("  Pensando…", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Bubble(partial, fromUser = false)
                    }
                }
            }
        }
        state.error?.let {
            SectionCard(container = MaterialTheme.colorScheme.errorContainer) {
                Text(it)
                Row {
                    TextButton(onClick = onDismissError) { Text("Vale") }
                    if (it.contains("024")) TextButton(onClick = onCrisis) { Text("Teléfonos de ayuda") }
                }
            }
        }
        if (state.turns.isEmpty() && !state.busy) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                quickPrompts.forEach { p -> AssistChip(onClick = { input = p }, label = { Text(p) }) }
            }
        }
        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Escribe aquí…") },
                modifier = Modifier.weight(1f),
                maxLines = 5,
            )
            IconButton(
                onClick = { onDismissError(); onSend(input); input = "" },
                enabled = input.isNotBlank() && !state.busy,
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar") }
        }
    }
}

@Composable
private fun Bubble(text: String, fromUser: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start) {
        Text(
            text,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (fromUser) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(12.dp),
            color = if (fromUser) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------------------------------------------------------------- Planes con IA

enum class PlanMode { RITUAL, LADDER }

/**
 * Genera peldaños con IA: o bien un plan gradual para soltar un ritual concreto,
 * o bien una escalera completa a partir de la descripción del TOC.
 */
@Composable
fun AiPlanFlow(
    mode: PlanMode,
    generate: suspend (String, String) -> Result<StepPlan>,
    onAdd: (List<ExposureStep>) -> Unit,
    onClose: () -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var plan by remember { mutableStateOf<StepPlan?>(null) }
    val selected = remember { mutableStateListOf<Int>() }
    var added by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onClose)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FlowHeader(if (mode == PlanMode.RITUAL) "Soltar un ritual" else "Escalera con IA", onClose)
        val p = plan
        when {
            added -> {
                SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Añadido a tu escalera", fontWeight = FontWeight.Bold)
                    Text("Empieza por el paso más fácil. Cada vez que lo practiques sin ritual, apunta si se cumplió tu predicción.")
                }
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Hecho") }
            }

            p == null -> {
                if (mode == PlanMode.RITUAL) {
                    Text("Describe el ritual y lo que temes que pase si no lo haces. La IA propondrá pasos graduales para soltarlo, cada uno como un pequeño experimento.")
                    OutlinedTextField(first, { first = it }, label = { Text("Ritual que quiero soltar") },
                        placeholder = { Text("Ej.: comprobar 10 veces que la puerta está cerrada") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(second, { second = it }, label = { Text("¿Qué temes que pase si no lo haces?") },
                        placeholder = { Text("Ej.: que entren a robar y sea culpa mía") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2)
                } else {
                    Text("Cuenta con tus palabras cómo es tu TOC: qué pensamientos te angustian, qué situaciones evitas y qué rituales haces (también los mentales).")
                    OutlinedTextField(first, { first = it }, label = { Text("Mi TOC") },
                        modifier = Modifier.fillMaxWidth(), minLines = 5)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                val ready = first.isNotBlank() && (mode == PlanMode.LADDER || second.isNotBlank())
                Button(
                    onClick = {
                        loading = true
                        error = null
                        scope.launch {
                            generate(first.trim(), second.trim())
                                .onSuccess { plan = it; selected.clear(); selected.addAll(it.steps.indices) }
                                .onFailure { error = it.message }
                            loading = false
                        }
                    },
                    enabled = ready && !loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("  Preparando el plan…")
                    } else {
                        Icon(Icons.Filled.AutoAwesome, null)
                        Text("  Crear plan")
                    }
                }
            }

            else -> {
                SectionCard(container = MaterialTheme.colorScheme.primaryContainer) { Text(p.intro) }
                Text("Elige los pasos que quieres añadir a tu escalera:", style = MaterialTheme.typography.titleSmall)
                p.steps.forEachIndexed { i, s ->
                    SectionCard(Modifier.selectable(selected = i in selected, onClick = {
                        if (i in selected) selected.remove(i) else selected.add(i)
                    })) {
                        Row(verticalAlignment = Alignment.Top) {
                            Checkbox(checked = i in selected, onCheckedChange = { if (it) selected.add(i) else selected.remove(i) })
                            Column(Modifier.weight(1f)) {
                                Text("${s.expectedDistress} · ${s.situation}", fontWeight = FontWeight.Bold)
                                Text("Sin: ${s.ritual}", style = MaterialTheme.typography.bodySmall)
                                Text("Pondrás a prueba: \"${s.prediction}\"", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Text("Revísalos: si alguno te parece demasiado o no encaja, desmárcalo. Mejor aún si lo comentas con tu terapeuta.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = {
                        onAdd(selected.sorted().map { i ->
                            val s = p.steps[i]
                            ExposureStep(0, s.situation, s.ritual, s.expectedDistress, s.theme, prediction = s.prediction)
                        })
                        added = true
                    },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Añadir ${selected.size} a mi escalera") }
                OutlinedButton(onClick = { plan = null }, modifier = Modifier.fillMaxWidth()) { Text("Volver a empezar") }
            }
        }
    }
}

// ---------------------------------------------------------------- Resumen

@Composable
fun SummaryDialog(summarize: suspend () -> Result<String>, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        summarize().onSuccess { text = it }.onFailure { error = it.message }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resumen para tu terapeuta") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when {
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                    text == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("  Analizando tus registros…")
                    }
                    else -> Text(text!!)
                }
            }
        },
        confirmButton = {
            val t = text
            if (t != null) {
                TextButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "Resumen de mi práctica (Raíz)")
                        .putExtra(Intent.EXTRA_TEXT, t)
                    context.startActivity(Intent.createChooser(send, "Compartir resumen"))
                }) { Text("Compartir") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}
