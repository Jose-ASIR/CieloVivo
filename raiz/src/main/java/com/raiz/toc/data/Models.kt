package com.raiz.toc.data

import kotlinx.serialization.Serializable

/**
 * Un peldaño de la escalera de exposición: una situación que dispara la obsesión
 * y el ritual que normalmente la "neutraliza".
 */
@Serializable
data class ExposureStep(
    val id: Long,
    val situation: String,
    /** Ritual (visible o mental) que se dejará de hacer durante la exposición. */
    val ritual: String,
    /** Malestar esperado de 0 a 100 (SUDS). */
    val expectedDistress: Int,
    /** Tema del TOC, para agrupar y variar las exposiciones. */
    val theme: Theme = Theme.OTHER,
    val createdAt: Long = 0,
)

@Serializable
enum class Theme(val label: String) {
    CONTAMINATION("Contaminación"),
    HARM("Daño / responsabilidad"),
    CHECKING("Comprobación"),
    SYMMETRY("Orden / simetría / \"justo así\""),
    INTRUSIVE("Pensamientos intrusivos (sexuales, religiosos, violentos)"),
    RELATIONSHIP("Pareja / relaciones"),
    HEALTH("Salud"),
    OTHER("Otro"),
}

/**
 * Una práctica de exposición con prevención de respuesta (EPR). Sigue el modelo de
 * aprendizaje inhibitorio: lo que importa es comprobar la predicción, no que la ansiedad baje.
 */
@Serializable
data class ExposureSession(
    val id: Long,
    val stepId: Long,
    val startedAt: Long,
    val durationSec: Int,
    /** Lo que la persona teme que ocurra si no hace el ritual. */
    val prediction: String,
    /** Probabilidad que le daba a la predicción antes de empezar (0-100). */
    val predictionBelief: Int,
    /** Malestar registrado durante la práctica (0-100), en orden. */
    val distress: List<Int>,
    val ritualDone: RitualOutcome,
    /** ¿Ocurrió lo temido? */
    val predictionHappened: Boolean,
    /** Probabilidad que le da después (0-100). */
    val beliefAfter: Int,
    /** Qué ha aprendido, con sus palabras. */
    val learning: String = "",
) {
    val peakDistress: Int get() = distress.maxOrNull() ?: 0
    val endDistress: Int get() = distress.lastOrNull() ?: 0
}

@Serializable
enum class RitualOutcome(val label: String) {
    RESISTED("Lo resistí entero"),
    PARTIAL("Lo hice a medias o más corto"),
    DONE("Lo hice"),
}

/** Un impulso de hacer un ritual que la persona registra en el momento. */
@Serializable
data class UrgeLog(
    val id: Long,
    val at: Long,
    val trigger: String,
    val intensity: Int,
    /** Minutos que logró aplazar el ritual (0 si lo hizo enseguida). */
    val delayedMin: Int,
    val outcome: RitualOutcome,
)

/**
 * Autoevaluación semanal orientativa. Cinco dimensiones (tiempo, malestar, interferencia,
 * resistencia, control) puntuadas de 0 a 4, por separado para obsesiones y compulsiones.
 * No es un diagnóstico ni sustituye a una escala clínica administrada por un profesional.
 */
@Serializable
data class WeeklyCheck(
    val at: Long,
    val obsessions: List<Int>,
    val compulsions: List<Int>,
) {
    val total: Int get() = obsessions.sum() + compulsions.sum()
}

@Serializable
data class AppData(
    val acceptedIntro: Boolean = false,
    val steps: List<ExposureStep> = emptyList(),
    val sessions: List<ExposureSession> = emptyList(),
    val urges: List<UrgeLog> = emptyList(),
    val checks: List<WeeklyCheck> = emptyList(),
    val nextId: Long = 1,
)
