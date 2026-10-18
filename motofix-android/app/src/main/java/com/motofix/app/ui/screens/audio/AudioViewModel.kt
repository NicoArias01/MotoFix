package com.motofix.app.ui.screens.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motofix.app.audio.AnalysisResult
import com.motofix.app.audio.AudioAnalyzer
import com.motofix.app.audio.DemoSound
import com.motofix.app.audio.EngineRecorder
import com.motofix.app.audio.SAMPLE_RATE
import com.motofix.app.audio.SamplePlayer
import com.motofix.app.audio.toPcm16
import com.motofix.app.data.AppContainer
import com.motofix.app.data.AudioDiagnosisRecord
import com.motofix.app.data.DiagnosisRepository
import com.motofix.app.data.GarageRepository
import com.motofix.app.data.Motorcycle
import com.motofix.app.data.RewardResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface AudioPhase {
    data object Idle : AudioPhase
    data object Recording : AudioPhase
    data class Analyzing(val progress: Float, val stage: String) : AudioPhase
    data class Result(val record: AudioDiagnosisRecord) : AudioPhase
    data class Problem(val title: String, val advice: String) : AudioPhase
}

class AudioViewModel(
    garage: GarageRepository = AppContainer.garage,
    private val diagnoses: DiagnosisRepository = AppContainer.diagnoses,
) : ViewModel() {

    val motorcycles: StateFlow<List<Motorcycle>> = garage.motorcycles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppContainer.store.current.motorcycles)

    val history: StateFlow<List<AudioDiagnosisRecord>> = diagnoses.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppContainer.store.current.diagnoses)

    /** Moto que se está diagnosticando; null = una moto que no está en el garaje. */
    var selectedMotoId by mutableStateOf(AppContainer.store.current.motorcycles.firstOrNull()?.id)
        private set

    var phase by mutableStateOf<AudioPhase>(AudioPhase.Idle)
        private set

    /** Niveles recientes del micrófono para dibujar la onda en vivo. */
    val liveLevels = mutableStateListOf<Float>()
    var recordProgress by mutableStateOf(0f)
        private set

    var reward by mutableStateOf<RewardResult?>(null)
        private set

    /** Hay audio de la sesión actual para volver a escucharlo. */
    var canReplay by mutableStateOf(false)
        private set

    private var lastSamples: ShortArray? = null
    private val player = SamplePlayer()
    private var job: Job? = null
    @Volatile private var stopRequested = false

    fun selectMoto(id: String?) { selectedMotoId = id }

    fun startRecording() {
        if (job?.isActive == true) return
        stopRequested = false
        liveLevels.clear()
        recordProgress = 0f
        phase = AudioPhase.Recording
        job = viewModelScope.launch {
            val samples = try {
                EngineRecorder.record(
                    seconds = RECORD_SECONDS,
                    onLevel = { level, progress ->
                        liveLevels.add(level)
                        if (liveLevels.size > MAX_LEVELS) liveLevels.removeAt(0)
                        recordProgress = progress
                    },
                    shouldStop = { stopRequested },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                phase = AudioPhase.Problem(
                    "No se pudo usar el micrófono",
                    "Cierra otras apps que estén grabando o llamando e inténtalo de nuevo.",
                )
                return@launch
            }
            lastSamples = samples
            canReplay = true
            analyze(FloatArray(samples.size) { samples[it] / 32768f }, isDemo = false, demoName = null)
        }
    }

    /** Termina la grabación antes de tiempo y analiza lo grabado. */
    fun stopRecording() { stopRequested = true }

    fun cancel() {
        job?.cancel()
        player.stop()
        phase = AudioPhase.Idle
    }

    fun runDemo(sound: DemoSound) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val floats = withContext(Dispatchers.Default) { sound.generate(SAMPLE_RATE) }
            lastSamples = floats.toPcm16()
            canReplay = true
            if (AppContainer.store.current.settings.soundEffects) player.play(lastSamples!!)
            analyze(floats, isDemo = true, demoName = sound.label)
        }
    }

    fun replay() { lastSamples?.let { player.play(it) } }

    fun dismissReward() { reward = null }

    fun newDiagnosis() {
        player.stop()
        phase = AudioPhase.Idle
    }

    private suspend fun analyze(samples: FloatArray, isDemo: Boolean, demoName: String?) {
        val stages = listOf("CARGANDO_MUESTRA", "CALCULANDO_FFT", "ANALIZANDO_RITMO", "COMPARANDO_PATRONES", "GENERANDO_REPORTE")
        phase = AudioPhase.Analyzing(0f, stages.first())
        val analysis = viewModelScope.async(Dispatchers.Default) { AudioAnalyzer.analyze(samples, SAMPLE_RATE) }
        // Animación mínima para que el usuario vea las etapas (el cálculo real tarda menos)
        val steps = 40
        for (i in 1..steps) {
            delay(ANALYSIS_MILLIS / steps)
            val progress = i / steps.toFloat()
            phase = AudioPhase.Analyzing(progress, stages[((progress * stages.size).toInt()).coerceAtMost(stages.size - 1)])
        }
        when (val result = analysis.await()) {
            is AnalysisResult.QualityIssue -> phase = AudioPhase.Problem(result.title, result.advice)
            is AnalysisResult.Diagnosis -> {
                val moto = selectedMotoId?.let { id -> motorcycles.value.firstOrNull { it.id == id } }
                val record = AudioDiagnosisRecord(
                    id = UUID.randomUUID().toString(),
                    createdAtMillis = System.currentTimeMillis(),
                    motorcycleId = if (isDemo) null else moto?.id,
                    motoName = when {
                        isDemo -> "Ejemplo: $demoName"
                        moto != null -> moto.displayName
                        else -> "Moto no registrada"
                    },
                    type = result.type,
                    confidence = result.confidence,
                    dominantHz = result.features.dominantHz.toInt(),
                    loudnessDb = result.features.loudnessDb.toInt(),
                    irregularity = (result.features.weakPulseRatio * 100).toInt(),
                    durationSec = result.features.durationSec,
                    isDemo = isDemo,
                    waveform = result.waveform,
                    spectrum = result.spectrum,
                )
                reward = diagnoses.save(record)
                phase = AudioPhase.Result(record)
            }
        }
    }

    override fun onCleared() {
        player.stop()
    }

    companion object {
        const val RECORD_SECONDS = 8.0
        const val MIN_SECONDS_BEFORE_STOP = 3.0
        private const val MAX_LEVELS = 60
        private const val ANALYSIS_MILLIS = 2_400L
    }
}
