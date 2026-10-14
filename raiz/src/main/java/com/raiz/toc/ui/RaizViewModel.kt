package com.raiz.toc.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raiz.toc.ai.AiModel
import com.raiz.toc.ai.ChatTurn
import com.raiz.toc.ai.ClaudeCoach
import com.raiz.toc.ai.Digest
import com.raiz.toc.ai.StepPlan
import com.raiz.toc.data.AppData
import com.raiz.toc.data.ExposureSession
import com.raiz.toc.data.ExposureStep
import com.raiz.toc.data.Store
import com.raiz.toc.data.UrgeLog
import com.raiz.toc.data.WeeklyCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Ajustes de la IA. La clave se guarda solo en el almacenamiento privado de la app. */
data class AiSettings(
    val apiKey: String = "",
    val model: AiModel = AiModel.OPUS,
    /** Si el acompañante puede ver un resumen de los registros para personalizar sus respuestas. */
    val shareRecords: Boolean = false,
) {
    val enabled: Boolean get() = apiKey.isNotBlank()
}

data class ChatState(
    val turns: List<ChatTurn> = emptyList(),
    /** Respuesta que está llegando en streaming, o null si no hay ninguna en curso. */
    val streaming: String? = null,
    val error: String? = null,
    /** Mensaje que falló, para devolverlo a la caja de texto. */
    val failedText: String? = null,
) {
    val busy: Boolean get() = streaming != null
}

/** Estado único de la app. `null` mientras se lee el archivo la primera vez. */
class RaizViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(File(app.filesDir, "raiz.json"))
    private val saveLock = Mutex()
    private val prefs = app.getSharedPreferences("raiz_ai", Context.MODE_PRIVATE)

    private val _data = MutableStateFlow<AppData?>(null)
    val data: StateFlow<AppData?> = _data.asStateFlow()

    private val _ai = MutableStateFlow(loadAi())
    val ai: StateFlow<AiSettings> = _ai.asStateFlow()

    private val _chat = MutableStateFlow(ChatState())
    val chat: StateFlow<ChatState> = _chat.asStateFlow()

    private var coach: ClaudeCoach? = null

    init {
        viewModelScope.launch(Dispatchers.IO) { _data.value = store.load() }
    }

    private fun edit(transform: (AppData) -> AppData) {
        _data.update { it?.let(transform) }
        // Siempre se guarda el estado más reciente, así el orden de las escrituras no importa.
        viewModelScope.launch(Dispatchers.IO) {
            saveLock.withLock { _data.value?.let(store::save) }
        }
    }

    fun acceptIntro() = edit { it.copy(acceptedIntro = true) }

    fun addStep(step: ExposureStep) = addSteps(listOf(step))

    fun addSteps(steps: List<ExposureStep>) = edit { d ->
        val added = steps.mapIndexed { i, s -> s.copy(id = d.nextId + i, createdAt = now()) }
        d.copy(steps = d.steps + added, nextId = d.nextId + steps.size)
    }

    fun deleteStep(id: Long) = edit { d ->
        d.copy(steps = d.steps.filterNot { it.id == id }, sessions = d.sessions.filterNot { it.stepId == id })
    }

    fun saveSession(session: ExposureSession) = edit {
        it.copy(sessions = it.sessions + session.copy(id = it.nextId), nextId = it.nextId + 1)
    }

    fun saveUrge(urge: UrgeLog) = edit {
        it.copy(urges = it.urges + urge.copy(id = it.nextId), nextId = it.nextId + 1)
    }

    fun saveCheck(check: WeeklyCheck) = edit { it.copy(checks = it.checks + check) }

    fun resetAll() = edit { AppData(acceptedIntro = true) }

    // ---------- IA ----------

    private fun loadAi() = AiSettings(
        apiKey = prefs.getString(KEY_API, "").orEmpty(),
        model = AiModel.entries.firstOrNull { it.name == prefs.getString(KEY_MODEL, null) } ?: AiModel.OPUS,
        shareRecords = prefs.getBoolean(KEY_SHARE, false),
    )

    fun saveAi(settings: AiSettings) {
        prefs.edit()
            .putString(KEY_API, settings.apiKey.trim())
            .putString(KEY_MODEL, settings.model.name)
            .putBoolean(KEY_SHARE, settings.shareRecords)
            .apply()
        _ai.value = settings.copy(apiKey = settings.apiKey.trim())
        resetCoach()
    }

    private fun resetCoach() {
        val old = coach
        coach = null
        if (old != null) viewModelScope.launch(Dispatchers.IO) { old.close() }
    }

    private fun coach(): ClaudeCoach {
        coach?.let { return it }
        val s = _ai.value
        check(s.enabled) { "IA no configurada" }
        return ClaudeCoach(s.apiKey, s.model).also { coach = it }
    }

    private fun recordsContext(): String? =
        if (_ai.value.shareRecords) _data.value?.let { Digest.build(it, now()) } else null

    private suspend fun <T> ai(block: (ClaudeCoach) -> T): Result<T> = withContext(Dispatchers.IO) {
        runCatching { block(coach()) }
    }

    fun sendChat(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _chat.value.busy) return
        val turns = _chat.value.turns + ChatTurn(fromUser = true, text = message)
        _chat.value = ChatState(turns = turns, streaming = "")
        val context = recordsContext()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                coach().chat(turns, context) { partial -> _chat.update { it.copy(streaming = partial) } }
            }.onSuccess { answer ->
                _chat.update { ChatState(turns = it.turns + ChatTurn(fromUser = false, text = answer)) }
            }.onFailure { e ->
                // Se quita el mensaje fallido para que los turnos sigan alternando.
                _chat.update { ChatState(turns = it.turns.dropLast(1), error = e.message ?: "Error desconocido", failedText = message) }
            }
        }
    }

    fun clearChatError() = _chat.update { it.copy(error = null, failedText = null) }

    fun newChat() {
        if (!_chat.value.busy) _chat.value = ChatState()
    }

    suspend fun suggestLadder(description: String): Result<StepPlan> = ai { it.suggestLadder(description) }

    suspend fun planRitual(ritual: String, fear: String): Result<StepPlan> = ai { it.planRitual(ritual, fear) }

    suspend fun suggestPrediction(step: ExposureStep): Result<String> =
        ai { it.suggestPrediction(step.situation, step.ritual) }

    suspend fun summarize(): Result<String> {
        val digest = _data.value?.let { Digest.build(it, now()) }.orEmpty()
        return ai { it.summarize(digest) }
    }

    override fun onCleared() {
        coach?.close()
    }

    companion object {
        private const val KEY_API = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_SHARE = "share_records"

        fun now() = System.currentTimeMillis()
    }
}
