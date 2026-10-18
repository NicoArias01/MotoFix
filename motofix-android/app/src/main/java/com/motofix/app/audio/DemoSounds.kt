package com.motofix.app.audio

import com.motofix.app.data.DiagnosisType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sonidos de motor sintetizados para probar el diagnóstico sin tener una moto encendida.
 * Cada uno imita el rasgo acústico de una falla.
 */
enum class DemoSound(val label: String, val description: String, val expected: DiagnosisType) {
    HEALTHY("Motor sano", "Ralentí estable de un monocilíndrico", DiagnosisType.HEALTHY),
    VALVES("Válvulas con holgura", "Chasquido agudo en cada ciclo", DiagnosisType.VALVE_CLEARANCE),
    MISFIRE("Fallo de encendido", "Explosiones que se saltan", DiagnosisType.IGNITION_MISFIRE),
    KNOCK("Golpeteo interno", "Golpes graves muy fuertes", DiagnosisType.ENGINE_KNOCK),
    EXHAUST("Fuga en el escape", "Siseo agudo constante", DiagnosisType.EXHAUST_LEAK),
    CHAIN("Cadena floja", "Traqueteo metálico", DiagnosisType.LOOSE_CHAIN);

    fun generate(sampleRate: Int = 44_100, seconds: Double = 6.0, seed: Int = 7): FloatArray =
        DemoSynth(sampleRate, seed).render(this, seconds)
}

private class DemoSynth(private val sr: Int, seed: Int) {
    private val random = Random(seed)

    fun render(sound: DemoSound, seconds: Double): FloatArray {
        val n = (sr * seconds).toInt()
        val out = FloatArray(n)
        val firingHz = 15.0
        val period = sr / firingHz

        // Ruido mecánico de fondo, suave y en todas las frecuencias
        var brown = 0.0
        for (i in 0 until n) {
            brown = (brown + random.nextDouble(-1.0, 1.0) * 0.05) * 0.995
            out[i] += (brown * 0.25 + random.nextDouble(-1.0, 1.0) * 0.004).toFloat()
        }

        var t = 0.0
        var cycle = 0
        while (t < n) {
            val start = (t + period * random.nextDouble(-0.03, 0.03)).toInt().coerceAtLeast(0)
            var amp = 1.0 + random.nextDouble(-0.1, 0.1)
            when (sound) {
                DemoSound.MISFIRE -> if (cycle % 5 == 2 || random.nextDouble() < 0.08) amp *= 0.08
                DemoSound.KNOCK -> if (cycle % 3 == 0) {
                    amp *= 2.4
                    addTone(out, start, 110.0, 0.6, 0.008)
                }
                else -> Unit
            }
            addCombustion(out, start, amp * 0.5)
            if (sound == DemoSound.VALVES) {
                addTone(out, start + (period * 0.4).toInt(), 3500.0 + random.nextDouble(-150.0, 150.0), 0.12, 0.0015)
            }
            t += period
            cycle++
        }

        if (sound == DemoSound.CHAIN) {
            var i = 0
            while (i < n) {
                addTone(out, i, 1600.0 + random.nextDouble(-200.0, 200.0), 0.10 + random.nextDouble(0.0, 0.06), 0.004)
                i += (sr / random.nextDouble(9.0, 15.0)).toInt()
            }
        }
        if (sound == DemoSound.EXHAUST) {
            // Siseo: ruido blanco filtrado por encima de 5 kHz
            val hiss = FloatArray(n) { random.nextDouble(-1.0, 1.0).toFloat() * 0.12f }
            val filtered = hiss.filtered { Biquad.highPass(sr, 5500.0) }
            for (i in 0 until n) out[i] += filtered[i]
        }

        val peak = out.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        for (i in 0 until n) out[i] = out[i] / peak * 0.6f
        return out
    }

    /** Explosión de la combustión: armónicos graves que se apagan rápido. */
    private fun addCombustion(out: FloatArray, start: Int, amp: Double) {
        val length = (sr * 0.06).toInt()
        val harmonics = listOf(60.0 to 1.0, 120.0 to 0.6, 180.0 to 0.35, 300.0 to 0.15)
        for (i in 0 until length) {
            val idx = start + i
            if (idx >= out.size) break
            val time = i / sr.toDouble()
            val env = exp(-time / 0.02)
            var v = 0.0
            for ((f, a) in harmonics) v += a * sin(2 * PI * f * time)
            out[idx] += (amp * env * v).toFloat()
        }
    }

    /** Tono corto que se apaga (chasquidos, golpes, traqueteos). */
    private fun addTone(out: FloatArray, start: Int, freq: Double, amp: Double, decay: Double) {
        val length = (sr * decay * 6).toInt()
        for (i in 0 until length) {
            val idx = start + i
            if (idx >= out.size) break
            val time = i / sr.toDouble()
            out[idx] += (amp * exp(-time / decay) * sin(2 * PI * freq * time)).toFloat()
        }
    }
}
