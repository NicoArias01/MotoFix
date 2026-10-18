package com.motofix.app.audio

import com.motofix.app.data.DiagnosisType
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Medidas acústicas que se extraen de la grabación. */
data class AudioFeatures(
    val durationSec: Float,
    val loudnessDb: Double,
    val clippedFraction: Double,
    val dominantHz: Double,
    /** Qué tan regular es el ritmo de las explosiones (0 a 1). */
    val periodicity: Double,
    val firingHz: Double,
    /** Fracción de ciclos con explosión mucho más débil (pulsos que faltan). */
    val weakPulseRatio: Double,
    /** Fracción de ciclos con golpe mucho más fuerte de lo normal. */
    val strongPulseRatio: Double,
    /** Energía relativa por bandas: <150, 150-400, 400-1k, 1k-2.5k, 2.5k-5k, >5k Hz. */
    val bandFractions: List<Double>,
    val lowRatio: Double,
    val midRatio: Double,
    val highRatio: Double,
    val airRatio: Double,
    val midKurtosis: Double,
    val highKurtosis: Double,
    val airKurtosis: Double,
)

sealed interface AnalysisResult {
    data class Diagnosis(
        val type: DiagnosisType,
        val confidence: Int,
        val features: AudioFeatures,
        val scores: Map<DiagnosisType, Double>,
        val waveform: List<Float>,
        val spectrum: List<Float>,
    ) : AnalysisResult

    /** La grabación no sirve para analizar (muy corta, muy baja o saturada). */
    data class QualityIssue(val title: String, val advice: String) : AnalysisResult
}

/**
 * Análisis acústico orientativo del motor (RF05).
 *
 * No es un modelo entrenado: mide características físicas del sonido y aplica reglas
 * basadas en cómo suena cada falla. Por eso el resultado es una sugerencia que no reemplaza
 * la revisión de un mecánico. Cuando exista el modelo de IA en el servidor, esta clase
 * puede seguir sirviendo para validar la calidad de la grabación antes de enviarla.
 */
object AudioAnalyzer {

    private const val MIN_SECONDS = 2.0
    private const val MIN_RMS = 0.006
    private const val DETECTION_THRESHOLD = 0.35

    fun analyze(samples: ShortArray, sampleRate: Int): AnalysisResult =
        analyze(FloatArray(samples.size) { samples[it] / 32768f }, sampleRate)

    fun analyze(raw: FloatArray, sampleRate: Int): AnalysisResult {
        // Se descartan los extremos (golpe del botón, movimiento del celular)
        val trim = (sampleRate * 0.25).toInt()
        val x = if (raw.size > trim * 4) raw.copyOfRange(trim, raw.size - trim) else raw
        val duration = x.size / sampleRate.toDouble()
        if (duration < MIN_SECONDS) {
            return AnalysisResult.QualityIssue(
                "Grabación muy corta",
                "Graba al menos 3 segundos con el motor encendido.",
            )
        }
        val rms = Dsp.rms(x)
        if (rms < MIN_RMS) {
            return AnalysisResult.QualityIssue(
                "No se escucha el motor",
                "Acerca el celular a unos 30 cm del motor y evita tapar el micrófono.",
            )
        }
        val clipped = x.count { abs(it) > 0.98f } / x.size.toDouble()
        if (clipped > 0.08) {
            return AnalysisResult.QualityIssue(
                "Sonido saturado",
                "El micrófono está demasiado cerca o el motor está muy acelerado. Aléjate un poco y graba en ralentí.",
            )
        }

        val features = extractFeatures(x, sampleRate, rms, clipped)
        val scores = score(features)
        val best = scores.maxByOrNull { it.value }!!
        val type: DiagnosisType
        val confidence: Double
        if (best.value < DETECTION_THRESHOLD) {
            type = DiagnosisType.HEALTHY
            // Más seguro cuanto más regular es el ritmo y más bajas las demás puntuaciones
            confidence = 60 + 25 * features.periodicity + 10 * (1 - best.value / DETECTION_THRESHOLD)
        } else {
            type = best.key
            val second = scores.filterKeys { it != best.key }.values.maxOrNull() ?: 0.0
            confidence = 55 + 30 * best.value + 15 * (best.value - second).coerceAtLeast(0.0)
        }
        val qualityPenalty = (if (clipped > 0.02) 8 else 0) + (if (duration < 4) 6 else 0)
        return AnalysisResult.Diagnosis(
            type = type,
            confidence = (confidence - qualityPenalty).roundToInt().coerceIn(40, 96),
            features = features,
            scores = scores,
            waveform = waveform(raw, 72),
            spectrum = spectrumBands(Dsp.averagePowerSpectrum(x), sampleRate, 18),
        )
    }

    // ---------- Reglas por falla ----------

    private fun ramp(value: Double, from: Double, to: Double): Double = ((value - from) / (to - from)).coerceIn(0.0, 1.0)

    internal fun score(f: AudioFeatures): Map<DiagnosisType, Double> {
        // Chasquido agudo y repetitivo en 2,5–5 kHz
        val valve = ramp(f.highRatio, 0.025, 0.06) * ramp(f.highKurtosis, 5.0, 10.0)
        // Siseo constante muy agudo (> 5 kHz): mucha energía, pero sin golpes
        val exhaust = ramp(f.airRatio, 0.10, 0.30) * ramp(6.0 - f.airKurtosis, 0.0, 2.5)
        // Traqueteo metálico de frecuencia media
        val chain = ramp(f.midRatio, 0.05, 0.09) * ramp(f.midKurtosis, 6.0, 10.0) * (1 - valve)
        // Explosiones que faltan en un ritmo que por lo demás es regular
        val misfire = ramp(f.weakPulseRatio, 0.07, 0.22) * ramp(f.periodicity, 0.15, 0.4)
        // Golpes graves mucho más fuertes que el resto
        val knock = ramp(f.strongPulseRatio, 0.08, 0.25) * ramp(f.lowRatio, 0.55, 0.8)
        return mapOf(
            DiagnosisType.VALVE_CLEARANCE to valve,
            DiagnosisType.EXHAUST_LEAK to exhaust,
            DiagnosisType.LOOSE_CHAIN to chain,
            DiagnosisType.IGNITION_MISFIRE to misfire,
            DiagnosisType.ENGINE_KNOCK to knock,
        )
    }

    // ---------- Extracción de características ----------

    internal fun extractFeatures(x: FloatArray, sr: Int, rms: Double, clipped: Double): AudioFeatures {
        val spectrum = Dsp.averagePowerSpectrum(x)
        val binHz = sr / 2048.0
        fun bandEnergy(lo: Double, hi: Double): Double {
            var sum = 0.0
            for (k in spectrum.indices) {
                val f = k * binHz
                if (f >= lo && f < hi) sum += spectrum[k]
            }
            return sum
        }
        val edges = listOf(20.0, 150.0, 400.0, 1000.0, 2500.0, 5000.0, min(12000.0, sr / 2.0))
        val bands = edges.zipWithNext { lo, hi -> bandEnergy(lo, hi) }
        val total = bands.sum().coerceAtLeast(1e-12)
        val fractions = bands.map { it / total }

        var peakBin = 1
        for (k in 1 until spectrum.size) {
            val f = k * binHz
            if (f > 2000) break
            if (f >= 20 && spectrum[k] > spectrum[peakBin]) peakBin = k
        }

        // Señales por banda
        val low = x.filtered { Biquad.lowPass(sr, 400.0) }
        val mid = x.filtered { Biquad.bandPass(sr, 1600.0, 1.2) }
        val high = x.filtered { Biquad.bandPass(sr, 3500.0, 1.4) }
        val air = x.filtered { Biquad.highPass(sr, 5500.0) }

        val frame = max(1, (sr * 0.005).toInt()) // envolvente cada 5 ms
        val envLow = Dsp.envelope(low, frame)

        // Ritmo de explosiones: autocorrelación de la envolvente grave entre 20 y 250 ms
        val (period, periodicity) = findPeriod(envLow, minLag = 4, maxLag = 50)
        var weak = 0.0
        var strong = 0.0
        if (period > 0) {
            // Explosión débil o que falta: pico bajo o un hueco de más de 1,5 periodos sin explosión
            val peaks = findPeaks(envLow, period)
            if (peaks.size >= 4) {
                val amps = peaks.map { envLow[it].toDouble() }
                val med = Dsp.median(amps)
                val gaps = peaks.zipWithNext { a, b -> b - a }
                val missing = gaps.sumOf { gap -> if (gap > 1.5 * period) (gap / period.toDouble()).roundToInt() - 1 else 0 }
                val weakPeaks = amps.count { it < 0.45 * med }
                weak = (missing + weakPeaks) / (peaks.size + missing).toDouble()
                strong = amps.count { it > 1.8 * med } / amps.size.toDouble()
            }
        }

        return AudioFeatures(
            durationSec = x.size / sr.toFloat(),
            loudnessDb = 20 * log10(rms.coerceAtLeast(1e-9)),
            clippedFraction = clipped,
            dominantHz = peakBin * binHz,
            periodicity = periodicity,
            firingHz = if (period > 0) 1.0 / (period * frame / sr.toDouble()) else 0.0,
            weakPulseRatio = weak,
            strongPulseRatio = strong,
            bandFractions = fractions,
            lowRatio = Dsp.rms(low) / rms,
            midRatio = Dsp.rms(mid) / rms,
            highRatio = Dsp.rms(high) / rms,
            airRatio = Dsp.rms(air) / rms,
            midKurtosis = Dsp.kurtosis(Dsp.envelope(mid, frame)),
            highKurtosis = Dsp.kurtosis(Dsp.envelope(high, frame)),
            airKurtosis = Dsp.kurtosis(Dsp.envelope(air, frame)),
        )
    }

    /**
     * Picos de la envolvente separados al menos 0,6 periodos. Se ignoran los que no sobresalen
     * del ruido de fondo, así una explosión que falta deja un hueco en vez de un pico falso.
     */
    private fun findPeaks(env: FloatArray, period: Int): List<Int> {
        val radius = max(1, (period * 0.6).toInt())
        val floor = Dsp.median(env.map { it.toDouble() }) * 1.2
        val peaks = mutableListOf<Int>()
        for (i in env.indices) {
            val v = env[i]
            if (v <= floor) continue
            var isMax = true
            for (j in max(0, i - radius)..min(env.size - 1, i + radius)) {
                if (env[j] > v || (env[j] == v && j < i)) {
                    isMax = false
                    break
                }
            }
            if (isMax) peaks.add(i)
        }
        return peaks
    }

    /** Devuelve (periodo en bloques, fuerza de la periodicidad 0..1). */
    private fun findPeriod(env: FloatArray, minLag: Int, maxLag: Int): Pair<Int, Double> {
        if (env.size < maxLag * 3) return 0 to 0.0
        // En escala logarítmica, unos pocos golpes muy fuertes no tapan el ritmo normal
        val floor = (env.max() * 1e-3).coerceAtLeast(1e-9)
        val logEnv = DoubleArray(env.size) { kotlin.math.ln(env[it] + floor) }
        val mean = logEnv.average()
        val d = DoubleArray(env.size) { logEnv[it] - mean }
        var energy = 0.0
        for (v in d) energy += v * v
        if (energy <= 1e-12) return 0 to 0.0
        var bestLag = 0
        var best = 0.0
        for (lag in minLag..maxLag) {
            var sum = 0.0
            for (i in 0 until d.size - lag) sum += d[i] * d[i + lag]
            val r = sum / energy
            if (r > best) {
                best = r
                bestLag = lag
            }
        }
        // Si una fracción del periodo también es fuerte, ese es el ritmo real
        // (pasa cuando una de cada 2 o 3 explosiones es distinta: golpeteo o fallo de encendido)
        fun corr(lag: Int): Double {
            var bestR = 0.0
            for (l in (lag - 1)..(lag + 1)) {
                if (l < 1 || l >= d.size) continue
                var sum = 0.0
                for (i in 0 until d.size - l) sum += d[i] * d[i + l]
                bestR = max(bestR, sum / energy)
            }
            return bestR
        }
        for (divisor in listOf(4, 3, 2)) {
            val shorter = (bestLag / divisor.toDouble()).roundToInt()
            if (shorter >= minLag && corr(shorter) > max(0.25, 0.6 * best)) {
                bestLag = shorter
                break
            }
        }
        return bestLag to best.coerceIn(0.0, 1.0)
    }

    /** Forma de onda reducida a `points` valores entre 0 y 1. */
    private fun waveform(x: FloatArray, points: Int): List<Float> {
        if (x.isEmpty()) return List(points) { 0f }
        val chunk = max(1, x.size / points)
        val peaks = List(points) { p ->
            val start = p * chunk
            val end = min(x.size, start + chunk)
            if (start >= end) 0f else Dsp.rms(x.copyOfRange(start, end)).toFloat()
        }
        val top = peaks.max().coerceAtLeast(1e-6f)
        return peaks.map { it / top }
    }

    /** Energía en `count` bandas logarítmicas (40 Hz – 10 kHz), normalizada a 0..1 en una escala de 45 dB. */
    private fun spectrumBands(power: DoubleArray, sr: Int, count: Int): List<Float> {
        val binHz = sr / 2048.0
        val lo = 40.0
        val hi = min(10000.0, sr / 2.0)
        val db = List(count) { i ->
            val f0 = lo * (hi / lo).pow(i / count.toDouble())
            val f1 = lo * (hi / lo).pow((i + 1) / count.toDouble())
            var sum = 0.0
            var n = 0
            for (k in power.indices) {
                val f = k * binHz
                if (f >= f0 && f < f1) {
                    sum += power[k]
                    n++
                }
            }
            10 * log10((if (n > 0) sum / n else 0.0).coerceAtLeast(1e-12))
        }
        val top = db.max()
        return db.map { ((it - (top - 45)) / 45).coerceIn(0.0, 1.0).toFloat() }
    }
}
