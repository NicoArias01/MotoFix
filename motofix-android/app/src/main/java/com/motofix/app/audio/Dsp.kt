package com.motofix.app.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Herramientas de procesamiento de señal, en Kotlin puro para poder probarlas sin Android. */
internal object Dsp {

    /** FFT radix-2 en el lugar. `re` e `im` deben tener tamaño potencia de 2. */
    fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                re[i] = re[j].also { re[j] = re[i] }
                im[i] = im[j].also { im[j] = im[i] }
            }
        }
        var len = 2
        while (len <= n) {
            val angle = -2 * PI / len
            val wRe = cos(angle)
            val wIm = sin(angle)
            var i = 0
            while (i < n) {
                var curRe = 1.0
                var curIm = 0.0
                for (k in 0 until len / 2) {
                    val aRe = re[i + k]
                    val aIm = im[i + k]
                    val bRe = re[i + k + len / 2] * curRe - im[i + k + len / 2] * curIm
                    val bIm = re[i + k + len / 2] * curIm + im[i + k + len / 2] * curRe
                    re[i + k] = aRe + bRe
                    im[i + k] = aIm + bIm
                    re[i + k + len / 2] = aRe - bRe
                    im[i + k + len / 2] = aIm - bIm
                    val nextRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nextRe
                }
                i += len
            }
            len = len shl 1
        }
    }

    /** Espectro de potencia promedio (Welch simplificado con ventana de Hann). */
    fun averagePowerSpectrum(x: FloatArray, size: Int = 2048): DoubleArray {
        val window = DoubleArray(size) { 0.5 - 0.5 * cos(2 * PI * it / (size - 1)) }
        val power = DoubleArray(size / 2)
        var frames = 0
        var start = 0
        while (start + size <= x.size) {
            val re = DoubleArray(size) { x[start + it] * window[it] }
            val im = DoubleArray(size)
            fft(re, im)
            for (k in power.indices) power[k] += re[k] * re[k] + im[k] * im[k]
            frames++
            start += size / 2
        }
        if (frames > 0) for (k in power.indices) power[k] /= frames.toDouble()
        return power
    }

    /** Envolvente RMS en bloques de `frame` muestras. */
    fun envelope(x: FloatArray, frame: Int): FloatArray {
        val count = x.size / frame
        return FloatArray(count) { f ->
            var sum = 0.0
            val base = f * frame
            for (i in 0 until frame) {
                val v = x[base + i]
                sum += v * v
            }
            sqrt(sum / frame).toFloat()
        }
    }

    fun rms(x: FloatArray): Double {
        if (x.isEmpty()) return 0.0
        var sum = 0.0
        for (v in x) sum += v * v
        return sqrt(sum / x.size)
    }

    /** Curtosis (4º momento / varianza²). Ruido estable ≈ 3; señal con golpes aislados ≫ 3. */
    fun kurtosis(x: FloatArray): Double {
        if (x.size < 4) return 0.0
        val mean = x.average()
        var m2 = 0.0
        var m4 = 0.0
        for (v in x) {
            val d = v - mean
            val d2 = d * d
            m2 += d2
            m4 += d2 * d2
        }
        m2 /= x.size
        m4 /= x.size
        return if (m2 <= 1e-12) 0.0 else m4 / (m2 * m2)
    }

    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
    }
}

/** Filtro biquad (fórmulas de Robert Bristow-Johnson). */
internal class Biquad private constructor(
    private val b0: Double,
    private val b1: Double,
    private val b2: Double,
    private val a1: Double,
    private val a2: Double,
) {
    fun process(x: FloatArray): FloatArray {
        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0
        return FloatArray(x.size) { i ->
            val x0 = x[i].toDouble()
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0
            y0.toFloat()
        }
    }

    companion object {
        private fun make(b0: Double, b1: Double, b2: Double, a0: Double, a1: Double, a2: Double) =
            Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)

        fun lowPass(sampleRate: Int, freq: Double, q: Double = 0.707): Biquad {
            val w = 2 * PI * freq / sampleRate
            val alpha = sin(w) / (2 * q)
            val c = cos(w)
            return make((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + alpha, -2 * c, 1 - alpha)
        }

        fun highPass(sampleRate: Int, freq: Double, q: Double = 0.707): Biquad {
            val w = 2 * PI * freq / sampleRate
            val alpha = sin(w) / (2 * q)
            val c = cos(w)
            return make((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + alpha, -2 * c, 1 - alpha)
        }

        fun bandPass(sampleRate: Int, center: Double, q: Double): Biquad {
            val w = 2 * PI * center / sampleRate
            val alpha = sin(w) / (2 * q)
            val c = cos(w)
            return make(alpha, 0.0, -alpha, 1 + alpha, -2 * c, 1 - alpha)
        }
    }
}

/** Aplica dos veces un filtro (cuarto orden) para separar mejor las bandas. */
internal fun FloatArray.filtered(make: () -> Biquad): FloatArray = make().process(make().process(this))
