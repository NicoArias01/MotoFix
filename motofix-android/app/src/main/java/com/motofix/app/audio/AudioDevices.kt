package com.motofix.app.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val SAMPLE_RATE = 44_100

/**
 * Graba audio crudo del micrófono (PCM 16 bits, mono).
 * Usa la fuente VOICE_RECOGNITION porque en la mayoría de celulares desactiva el control
 * automático de volumen, que deformaría el sonido del motor.
 */
object EngineRecorder {

    /**
     * Graba durante [seconds] y devuelve las muestras. [onLevel] recibe el nivel (0..1) y el
     * progreso (0..1) varias veces por segundo para dibujar la onda en vivo.
     * Se detiene antes si se cancela la corrutina o [shouldStop] devuelve true.
     */
    @SuppressLint("MissingPermission") // La pantalla pide el permiso antes de llamar aquí
    suspend fun record(
        seconds: Double,
        onLevel: (level: Float, progress: Float) -> Unit,
        shouldStop: () -> Boolean = { false },
    ): ShortArray = withContext(Dispatchers.IO) {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        require(minBuffer > 0) { "El micrófono no soporta este formato" }
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBuffer * 2, SAMPLE_RATE / 5 * 2),
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "No se pudo abrir el micrófono" }

        val total = (SAMPLE_RATE * seconds).toInt()
        val data = ShortArray(total)
        val chunk = ShortArray(SAMPLE_RATE / 20) // 50 ms
        var written = 0
        try {
            recorder.startRecording()
            while (written < total && !shouldStop()) {
                currentCoroutineContext().ensureActive()
                val read = recorder.read(chunk, 0, min(chunk.size, total - written))
                if (read <= 0) continue
                System.arraycopy(chunk, 0, data, written, read)
                written += read
                var peak = 0
                for (i in 0 until read) peak = max(peak, abs(chunk[i].toInt()))
                withContext(Dispatchers.Main) { onLevel(peak / 32768f, written / total.toFloat()) }
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
        data.copyOf(written)
    }
}

/** Reproduce una grabación o un sonido de ejemplo. */
class SamplePlayer {
    private var track: AudioTrack? = null

    fun play(samples: ShortArray, sampleRate: Int = SAMPLE_RATE) {
        stop()
        if (samples.isEmpty()) return
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        track = audioTrack
    }

    fun stop() {
        track?.let {
            runCatching { it.stop() }
            it.release()
        }
        track = null
    }
}

fun FloatArray.toPcm16(): ShortArray = ShortArray(size) { (this[it].coerceIn(-1f, 1f) * 32767).toInt().toShort() }
