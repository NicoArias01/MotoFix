package com.motofix.app.audio

import com.motofix.app.data.DiagnosisType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AudioAnalyzerTest {

    private val sr = 44_100

    private fun diagnose(samples: FloatArray): AnalysisResult.Diagnosis {
        val result = AudioAnalyzer.analyze(samples, sr)
        assertTrue("Se esperaba un diagnóstico y llegó $result", result is AnalysisResult.Diagnosis)
        return result as AnalysisResult.Diagnosis
    }

    @Test
    fun cadaSonidoDeEjemploDaSuDiagnostico() {
        val failures = mutableListOf<String>()
        for (sound in DemoSound.entries) {
            for (seed in listOf(7, 21, 99)) {
                val d = diagnose(sound.generate(sr, seed = seed))
                val f = d.features
                println(
                    "%-8s seed=%-3d -> %-16s %d%% | per=%.2f fire=%.1fHz weak=%.2f strong=%.2f low=%.2f mid=%.2f(k%.1f) high=%.2f(k%.1f) air=%.2f(k%.1f) scores=%s".format(
                        sound.name, seed, d.type.name, d.confidence, f.periodicity, f.firingHz, f.weakPulseRatio,
                        f.strongPulseRatio, f.lowRatio, f.midRatio, f.midKurtosis, f.highRatio, f.highKurtosis,
                        f.airRatio, f.airKurtosis, d.scores.mapKeys { it.key.name.take(5) }.mapValues { "%.2f".format(it.value) },
                    )
                )
                if (d.type != sound.expected) failures += "${sound.name}(seed $seed) dio ${d.type}"
            }
        }
        assertTrue("Fallaron: $failures", failures.isEmpty())
    }

    @Test
    fun motorSanoConRuidoDeFondoYVolumenBajoSigueSano() {
        val random = Random(3)
        val noisy = DemoSound.HEALTHY.generate(sr).map { it * 0.3f + (random.nextFloat() - 0.5f) * 0.012f }.toFloatArray()
        val d = diagnose(noisy)
        assertEquals(DiagnosisType.HEALTHY, d.type)
    }

    @Test
    fun elSilencioPideRepetirLaGrabacion() {
        val silence = FloatArray(sr * 5) { Random.nextFloat() * 0.001f }
        assertTrue(AudioAnalyzer.analyze(silence, sr) is AnalysisResult.QualityIssue)
    }

    @Test
    fun unaGrabacionCortaPideRepetir() {
        val short = DemoSound.HEALTHY.generate(sr, seconds = 1.5)
        assertTrue(AudioAnalyzer.analyze(short, sr) is AnalysisResult.QualityIssue)
    }

    @Test
    fun elSonidoSaturadoPideAlejarse() {
        val clipped = FloatArray(sr * 5) { if (it % 2 == 0) 1f else -1f }
        assertTrue(AudioAnalyzer.analyze(clipped, sr) is AnalysisResult.QualityIssue)
    }

    @Test
    fun laConfianzaEstaEnRango() {
        for (sound in DemoSound.entries) {
            val d = diagnose(sound.generate(sr))
            assertTrue(d.confidence in 40..96)
            assertEquals(72, d.waveform.size)
            assertEquals(18, d.spectrum.size)
        }
    }

    @Test
    fun funcionaConOtraFrecuenciaDeMuestreo() {
        val d = AudioAnalyzer.analyze(DemoSound.HEALTHY.generate(22_050), 22_050)
        assertTrue(d is AnalysisResult.Diagnosis && d.type == DiagnosisType.HEALTHY)
    }
}
