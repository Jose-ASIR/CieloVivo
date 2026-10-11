package com.raiz.toc.data

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Guarda todo en un único JSON privado de la app. Escritura atómica (temporal + renombrado)
 * para no perder datos si la app se cierra a mitad.
 */
class Store(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): AppData =
        runCatching { json.decodeFromString(AppData.serializer(), file.readText()) }
            .getOrDefault(AppData())

    fun save(data: AppData) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), data))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
