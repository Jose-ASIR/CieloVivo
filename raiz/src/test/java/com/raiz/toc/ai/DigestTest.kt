package com.raiz.toc.ai

import com.raiz.toc.data.AppData
import com.raiz.toc.data.ExposureSession
import com.raiz.toc.data.ExposureStep
import com.raiz.toc.data.RitualOutcome
import com.raiz.toc.data.UrgeLog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DigestTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 400 * day

    @Test
    fun includesRecentRecordsAndSkipsOldOnes() {
        val data = AppData(
            steps = listOf(ExposureStep(1, "Tocar el pomo", "lavarme", 40)),
            sessions = listOf(
                ExposureSession(2, 1, now - day, 900, "Enfermaré", 80, listOf(50, 70, 40),
                    RitualOutcome.RESISTED, false, 20, "Pude aguantarlo"),
            ),
            urges = listOf(
                UrgeLog(3, now - 2 * day, "Pomo del baño", 70, 10, RitualOutcome.PARTIAL),
                UrgeLog(4, now - 60 * day, "Disparador antiguo", 70, 10, RitualOutcome.DONE),
            ),
        )
        val text = Digest.build(data, now)
        assertTrue(text.contains("Tocar el pomo"))
        assertTrue(text.contains("80% → 20%"))
        assertTrue(text.contains("Pude aguantarlo"))
        assertTrue(text.contains("Pomo del baño"))
        assertFalse(text.contains("Disparador antiguo"))
    }
}
