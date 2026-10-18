package com.motofix.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Única fuente de verdad de la app: guarda todo en un JSON dentro del teléfono.
 * Cuando exista la API en Python, los repositorios hablarán con ella y esto quedará como caché.
 */
class AppStore(private val file: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    val current: AppData get() = _data.value

    fun update(transform: (AppData) -> AppData) {
        val before = _data.value
        val after = _data.updateAndGet(transform)
        if (after != before) persist()
    }

    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
    } catch (e: Exception) {
        // Archivo dañado o de una versión incompatible: se empieza de cero
        AppData()
    }

    private fun persist() {
        scope.launch {
            writeLock.withLock {
                // Siempre se escribe el valor más reciente, así no importa el orden de las corrutinas
                val text = json.encodeToString(AppData.serializer(), _data.value)
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(file)) {
                    file.writeText(text)
                    tmp.delete()
                }
            }
        }
    }
}
