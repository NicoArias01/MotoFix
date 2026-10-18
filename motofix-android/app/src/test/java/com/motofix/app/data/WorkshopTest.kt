package com.motofix.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.math.hypot

class WorkshopTest {

    private fun newStore(): AppStore {
        val file = File.createTempFile("motofix", ".json").apply { delete(); deleteOnExit() }
        return AppStore(file).apply {
            update { AppData(user = UserProfile(name = "Tester", email = "t@t.com", level = 1, coins = 0, xp = 0), loggedIn = true) }
        }
    }

    @Test
    fun todosLosNivelesSePuedenGanar() {
        for (s in WorkshopCatalog.scenarios) {
            for (step in s.steps) {
                assertTrue("${s.id}: falta ${step.tool} en la caja", step.tool in s.toolbox)
                assertTrue("${s.id}: falta ${step.part} en el diagrama", step.part in s.parts)
            }
            assertTrue("${s.id}: debe tener herramientas de distracción", s.toolbox.size > s.steps.map { it.tool }.toSet().size)
            assertTrue("${s.id}: tiempo muy corto", s.timeLimitSec >= s.steps.size * 20)
        }
        assertEquals((1..WorkshopCatalog.scenarios.size).toList(), WorkshopCatalog.scenarios.map { it.number })
    }

    @Test
    fun losComponentesNoSeEncimanEnElDiagrama() {
        val parts = Part.entries
        for (i in parts.indices) for (j in i + 1 until parts.size) {
            val d = hypot(parts[i].x - parts[j].x, parts[i].y - parts[j].y)
            assertTrue("${parts[i]} y ${parts[j]} están muy cerca ($d)", d >= 0.1f)
        }
        parts.forEach { assertTrue("$it fuera del dibujo", it.x in 0f..1f && it.y in 0f..0.72f) }
    }

    @Test
    fun completarUnNivelDaRecompensaCertificadoYDesbloqueaElSiguiente() {
        val store = newStore()
        val missions = MissionsRepository(store) { LocalDate.of(2026, 10, 18) }
        val workshop = WorkshopRepository(store, missions)
        val first = WorkshopCatalog.scenarios[0]

        assertTrue(workshop.currentLevels()[0].unlocked)
        assertFalse(workshop.currentLevels()[1].unlocked)

        val reward = workshop.completeRepair(RepairOutcome(first, stars = 2, timeUsedSec = 90, mistakes = 1))
        assertNotNull(reward)
        assertTrue(reward!!.firstCompletion)
        assertEquals(first.rewardXp, reward.reward.xp)
        assertEquals(first.rewardCoins, store.current.user!!.coins)
        assertEquals(1, store.current.certificates.size)
        assertEquals(first.steps.size, store.current.certificates[0].steps.size)
        assertTrue(workshop.currentLevels()[1].unlocked)

        // Repetir sin mejorar: poca XP, sin monedas y el mismo certificado
        val replay = workshop.completeRepair(RepairOutcome(first, stars = 2, timeUsedSec = 80, mistakes = 1))!!
        assertEquals(10, replay.reward.xp)
        assertEquals(0, replay.reward.coins)
        assertEquals(reward.certificateId, replay.certificateId)
        assertEquals(80, workshop.level(first.id)!!.progress.bestTimeSec)

        // Mejorar las estrellas: bono y certificado nuevo que reemplaza al anterior
        val better = workshop.completeRepair(RepairOutcome(first, stars = 3, timeUsedSec = 70, mistakes = 0))!!
        assertTrue(better.improved)
        assertEquals(1, store.current.certificates.size)
        assertEquals(3, store.current.certificates[0].stars)
    }

    @Test
    fun reparacionYDiagnosticoAvanzanLasMisiones() {
        val store = newStore()
        // Día en que salen las misiones de taller y audio (ver MissionCatalog.dailyFor)
        val day = (0L..10L).map { LocalDate.of(2026, 10, 18).plusDays(it) }.first { date ->
            MissionCatalog.dailyFor(date.toEpochDay()).any { it.action == MissionAction.COMPLETE_REPAIR }
        }
        val missions = MissionsRepository(store) { day }
        val workshop = WorkshopRepository(store, missions)
        workshop.completeRepair(RepairOutcome(WorkshopCatalog.scenarios[0], 3, 60, 0))
        val repair = missions.currentOverview().daily.first { it.def.action == MissionAction.COMPLETE_REPAIR }
        assertTrue(repair.isCompleted)
    }

    @Test
    fun losDiagnosticosDeEjemploNoDanXp() {
        val store = newStore()
        val diagnoses = DiagnosisRepository(store, MissionsRepository(store))
        fun record(demo: Boolean) = AudioDiagnosisRecord(
            id = "id-$demo", createdAtMillis = 1, motoName = "x", type = DiagnosisType.HEALTHY, confidence = 80,
            dominantHz = 60, loudnessDb = -20, irregularity = 0, durationSec = 7f, isDemo = demo,
            waveform = emptyList(), spectrum = emptyList(),
        )
        assertNull(diagnoses.save(record(demo = true)))
        assertEquals(0, store.current.user!!.xp)
        assertNotNull(diagnoses.save(record(demo = false)))
        assertEquals(DiagnosisRepository.XP_PER_DIAGNOSIS, store.current.user!!.xp)
        assertEquals(2, store.current.diagnoses.size)
    }
}
