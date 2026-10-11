package com.raiz.toc.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

class StoreTest {
    @Test
    fun roundTripsAndToleratesMissingOrBrokenFile() {
        val dir = Files.createTempDirectory("raiz").toFile()
        val file = File(dir, "data.json")
        val store = Store(file)
        assertEquals(AppData(), store.load())

        val data = AppData(
            acceptedIntro = true,
            steps = listOf(ExposureStep(1, "Tocar el pomo", "Lavarme", 40, Theme.CONTAMINATION)),
            urges = listOf(UrgeLog(2, 10, "pomo", 70, 5, RitualOutcome.PARTIAL)),
            nextId = 3,
        )
        store.save(data)
        assertEquals(data, store.load())

        file.writeText("{roto")
        assertEquals(AppData(), store.load())
        dir.deleteRecursively()
    }
}
