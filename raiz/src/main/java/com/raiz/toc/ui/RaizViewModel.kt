package com.raiz.toc.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import java.io.File

/** Estado único de la app. `null` mientras se lee el archivo la primera vez. */
class RaizViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(File(app.filesDir, "raiz.json"))
    private val saveLock = Mutex()

    private val _data = MutableStateFlow<AppData?>(null)
    val data: StateFlow<AppData?> = _data.asStateFlow()

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

    fun addStep(step: ExposureStep) = edit {
        it.copy(steps = it.steps + step.copy(id = it.nextId, createdAt = now()), nextId = it.nextId + 1)
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

    companion object {
        fun now() = System.currentTimeMillis()
    }
}
