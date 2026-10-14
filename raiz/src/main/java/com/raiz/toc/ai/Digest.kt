package com.raiz.toc.ai

import com.raiz.toc.data.AppData
import com.raiz.toc.data.Progress
import com.raiz.toc.data.RitualOutcome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Convierte los registros en un texto compacto para la IA. Solo se envía si la persona
 * lo permite en Ajustes de IA, y nunca la clave ni nada que no haya escrito en la app.
 */
object Digest {
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun build(data: AppData, now: Long, days: Int = 28): String {
        val from = now - days * DAY_MS
        val fmt = SimpleDateFormat("d MMM HH:mm", Locale.forLanguageTag("es-ES"))
        val sb = StringBuilder()

        val week = Progress.lastWeek(data, now)
        sb.appendLine("Últimos 7 días: ${week.exposures} exposiciones, ${week.urges} impulsos registrados " +
            "(${week.urgesResisted} resistidos, ${week.urgesReduced} recortados), ${week.minutesDelayed} min aplazados.")

        if (data.steps.isNotEmpty()) {
            sb.appendLine().appendLine("Escalera (malestar esperado · situación · ritual · prácticas sin ritual · estado):")
            data.steps.sortedBy { it.expectedDistress }.forEach { s ->
                val sessions = Progress.sessionsFor(data, s.id)
                val resisted = sessions.count { it.ritualDone == RitualOutcome.RESISTED }
                val state = if (Progress.isConsolidated(data, s)) "consolidado" else "en curso"
                sb.appendLine("- ${s.expectedDistress} · ${s.situation} · sin ${s.ritual} · $resisted/${sessions.size} · $state")
            }
        }

        val sessions = data.sessions.filter { it.startedAt >= from }.sortedBy { it.startedAt }.takeLast(15)
        if (sessions.isNotEmpty()) {
            sb.appendLine().appendLine("Exposiciones recientes:")
            sessions.forEach { s ->
                val step = data.steps.firstOrNull { it.id == s.stepId }?.situation ?: "(peldaño borrado)"
                sb.appendLine("- ${fmt.format(Date(s.startedAt))} · $step · ${s.durationSec / 60} min · " +
                    "malestar máx ${s.peakDistress}, final ${s.endDistress} · ritual: ${s.ritualDone.label} · " +
                    "predicción \"${s.prediction}\" ${s.predictionBelief}% → ${s.beliefAfter}%, " +
                    (if (s.predictionHappened) "ocurrió" else "no ocurrió") +
                    (if (s.learning.isNotBlank()) " · aprendió: \"${s.learning}\"" else ""))
            }
        }

        val urges = data.urges.filter { it.at >= from }.sortedBy { it.at }.takeLast(25)
        if (urges.isNotEmpty()) {
            sb.appendLine().appendLine("Impulsos recientes (fecha · disparador · intensidad · aplazado · resultado):")
            urges.forEach { u ->
                sb.appendLine("- ${fmt.format(Date(u.at))} · ${u.trigger.ifBlank { "sin describir" }} · ${u.intensity} · " +
                    "${u.delayedMin} min · ${u.outcome.label}")
            }
        }

        val checks = data.checks.sortedBy { it.at }
        if (checks.isNotEmpty()) {
            sb.appendLine().appendLine("Chequeos semanales (0-40, orientativos): " +
                checks.takeLast(8).joinToString(", ") { "${fmt.format(Date(it.at)).substringBeforeLast(' ')}: ${it.total}" })
        }
        return sb.toString().trim()
    }
}
