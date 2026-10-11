package com.raiz.toc.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Cálculos puros sobre los datos del usuario (sin Android, para poder probarlos). */
object Progress {

    /** Prácticas resistidas necesarias para dar un peldaño por consolidado. */
    const val SESSIONS_TO_CONSOLIDATE = 3

    /** Probabilidad (0-100) por debajo de la cual la predicción se considera desmentida. */
    const val BELIEF_DISCONFIRMED = 30

    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun sessionsFor(data: AppData, stepId: Long): List<ExposureSession> =
        data.sessions.filter { it.stepId == stepId }.sortedBy { it.startedAt }

    /**
     * Un peldaño está consolidado cuando se ha practicado varias veces sin ritual y la
     * última vez ya no se creía la predicción. No se exige que la ansiedad baje a cero.
     */
    fun isConsolidated(data: AppData, step: ExposureStep): Boolean {
        val sessions = sessionsFor(data, step.id)
        val resisted = sessions.count { it.ritualDone == RitualOutcome.RESISTED }
        val last = sessions.lastOrNull() ?: return false
        return resisted >= SESSIONS_TO_CONSOLIDATE && last.beliefAfter <= BELIEF_DISCONFIRMED
    }

    /** El peldaño más bajo que aún no está consolidado. */
    fun suggestNext(data: AppData): ExposureStep? =
        data.steps
            .sortedWith(compareBy({ it.expectedDistress }, { it.id }))
            .firstOrNull { !isConsolidated(data, it) }

    /**
     * Si las últimas prácticas han sido siempre el mismo peldaño, conviene variar
     * (otro lugar, otra hora, otro peldaño): la variedad hace el aprendizaje más duradero.
     */
    fun shouldVary(data: AppData, window: Int = 3): Boolean {
        val recent = data.sessions.sortedByDescending { it.startedAt }.take(window)
        return recent.size == window && recent.map { it.stepId }.distinct().size == 1
    }

    fun consolidatedCount(data: AppData): Int = data.steps.count { isConsolidated(data, it) }

    data class WeekStats(
        val urges: Int,
        val urgesResisted: Int,
        val urgesReduced: Int,
        val minutesDelayed: Int,
        val exposures: Int,
        val predictionsDisconfirmed: Int,
    ) {
        /** Fracción de impulsos resistidos o recortados (0..1), o null si no hay registros. */
        val resistRate: Double? =
            if (urges == 0) null else (urgesResisted + urgesReduced).toDouble() / urges
    }

    fun lastWeek(data: AppData, now: Long): WeekStats {
        val from = now - 7 * DAY_MS
        val urges = data.urges.filter { it.at in from..now }
        val sessions = data.sessions.filter { it.startedAt in from..now }
        return WeekStats(
            urges = urges.size,
            urgesResisted = urges.count { it.outcome == RitualOutcome.RESISTED },
            urgesReduced = urges.count { it.outcome == RitualOutcome.PARTIAL },
            minutesDelayed = urges.sumOf { it.delayedMin },
            exposures = sessions.size,
            predictionsDisconfirmed = sessions.count { !it.predictionHappened },
        )
    }

    /** Días seguidos (terminando hoy o ayer) con al menos una práctica o un impulso trabajado. */
    fun streakDays(data: AppData, now: Long, zone: ZoneId): Int {
        val days = buildSet {
            data.sessions.forEach { add(day(it.startedAt, zone)) }
            data.urges.filter { it.outcome != RitualOutcome.DONE || it.delayedMin > 0 }
                .forEach { add(day(it.at, zone)) }
        }
        var cursor = day(now, zone)
        if (cursor !in days) cursor = cursor.minusDays(1)
        var streak = 0
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun day(ms: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun weeklyCheckDue(data: AppData, now: Long): Boolean {
        val last = data.checks.maxOfOrNull { it.at } ?: return true
        return now - last >= 7 * DAY_MS
    }

    /** Rango orientativo de la autoevaluación (0-40). */
    fun severityLabel(total: Int): String = when {
        total <= 7 -> "Mínimo"
        total <= 15 -> "Leve"
        total <= 23 -> "Moderado"
        total <= 31 -> "Alto"
        else -> "Muy alto"
    }

    /** Diferencia entre la primera y la última autoevaluación (negativa = mejora). */
    fun checkTrend(data: AppData): Int? {
        val sorted = data.checks.sortedBy { it.at }
        if (sorted.size < 2) return null
        return sorted.last().total - sorted.first().total
    }
}
