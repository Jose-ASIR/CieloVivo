package com.raiz.toc.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class ProgressTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 100 * day + 12 * 60 * 60 * 1000L

    private fun step(id: Long, distress: Int) = ExposureStep(id, "s$id", "r$id", distress)

    private fun session(
        stepId: Long,
        at: Long,
        outcome: RitualOutcome = RitualOutcome.RESISTED,
        beliefAfter: Int = 10,
        happened: Boolean = false,
    ) = ExposureSession(
        id = at, stepId = stepId, startedAt = at, durationSec = 600,
        prediction = "p", predictionBelief = 80, distress = listOf(40, 70, 50),
        ritualDone = outcome, predictionHappened = happened, beliefAfter = beliefAfter,
    )

    @Test
    fun suggestsLowestNotConsolidatedStep() {
        val data = AppData(
            steps = listOf(step(1, 60), step(2, 30), step(3, 45)),
            sessions = (1..3).map { session(2, now - it * day) },
        )
        assertTrue(Progress.isConsolidated(data, data.steps[1]))
        assertEquals(3L, Progress.suggestNext(data)?.id)
    }

    @Test
    fun notConsolidatedIfStillBelievesPrediction() {
        val data = AppData(
            steps = listOf(step(1, 30)),
            sessions = listOf(
                session(1, now - 3 * day),
                session(1, now - 2 * day),
                session(1, now - day, beliefAfter = 60),
            ),
        )
        assertFalse(Progress.isConsolidated(data, data.steps[0]))
    }

    @Test
    fun ritualSessionsDoNotCountTowardsConsolidation() {
        val data = AppData(
            steps = listOf(step(1, 30)),
            sessions = (1..3).map { session(1, now - it * day, outcome = RitualOutcome.DONE) },
        )
        assertFalse(Progress.isConsolidated(data, data.steps[0]))
    }

    @Test
    fun suggestsNothingWhenAllConsolidated() {
        val data = AppData(steps = listOf(step(1, 30)), sessions = (1..3).map { session(1, now - it * day) })
        assertNull(Progress.suggestNext(data))
    }

    @Test
    fun detectsLackOfVariety() {
        val same = AppData(sessions = (1..3).map { session(1, now - it * day) })
        val varied = AppData(sessions = listOf(session(1, now - day), session(2, now - 2 * day), session(1, now - 3 * day)))
        assertTrue(Progress.shouldVary(same))
        assertFalse(Progress.shouldVary(varied))
    }

    @Test
    fun weekStatsIgnoreOldEntries() {
        val data = AppData(
            urges = listOf(
                UrgeLog(1, now - day, "t", 70, 10, RitualOutcome.RESISTED),
                UrgeLog(2, now - 2 * day, "t", 70, 5, RitualOutcome.PARTIAL),
                UrgeLog(3, now - 3 * day, "t", 70, 0, RitualOutcome.DONE),
                UrgeLog(4, now - 10 * day, "t", 70, 30, RitualOutcome.RESISTED),
            ),
            sessions = listOf(session(1, now - day), session(1, now - 2 * day, happened = true)),
        )
        val w = Progress.lastWeek(data, now)
        assertEquals(3, w.urges)
        assertEquals(1, w.urgesResisted)
        assertEquals(15, w.minutesDelayed)
        assertEquals(2.0 / 3, w.resistRate!!, 1e-9)
        assertEquals(2, w.exposures)
        assertEquals(1, w.predictionsDisconfirmed)
    }

    @Test
    fun streakCountsConsecutiveDaysEndingTodayOrYesterday() {
        val data = AppData(
            sessions = listOf(session(1, now - day), session(1, now - 2 * day), session(1, now - 4 * day)),
            urges = listOf(UrgeLog(1, now - 3 * day, "t", 50, 0, RitualOutcome.DONE)),
        )
        // Hoy no hay nada; ayer y anteayer sí; el día -3 solo tiene un ritual hecho sin aplazar.
        assertEquals(2, Progress.streakDays(data, now, ZoneOffset.UTC))
        assertEquals(0, Progress.streakDays(AppData(), now, ZoneOffset.UTC))
    }

    @Test
    fun weeklyCheckDueAfterSevenDays() {
        assertTrue(Progress.weeklyCheckDue(AppData(), now))
        val recent = AppData(checks = listOf(WeeklyCheck(now - 2 * day, List(5) { 1 }, List(5) { 1 })))
        assertFalse(Progress.weeklyCheckDue(recent, now))
        assertEquals(10, recent.checks[0].total)
    }

    @Test
    fun trendComparesFirstAndLastCheck() {
        val data = AppData(
            checks = listOf(
                WeeklyCheck(now, List(5) { 2 }, List(5) { 2 }),
                WeeklyCheck(now - 14 * day, List(5) { 3 }, List(5) { 3 }),
            ),
        )
        assertEquals(-10, Progress.checkTrend(data))
        assertEquals("Moderado", Progress.severityLabel(20))
    }
}
