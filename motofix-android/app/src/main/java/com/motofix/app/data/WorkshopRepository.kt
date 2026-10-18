package com.motofix.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.random.Random

data class WorkshopLevel(
    val scenario: RepairScenario,
    val progress: LevelProgress,
    val unlocked: Boolean,
) {
    val completed: Boolean get() = progress.completions > 0
}

/** Módulo del taller virtual (RF09) y sus certificados (RF10). */
class WorkshopRepository(
    private val store: AppStore,
    private val missions: MissionsRepository,
) {

    val levels: Flow<List<WorkshopLevel>> = store.data.map { buildLevels(it.workshop) }.distinctUntilChanged()

    val certificates: Flow<List<Certificate>> = store.data
        .map { data -> data.certificates.sortedByDescending { it.dateEpochDay } }
        .distinctUntilChanged()

    fun currentLevels(): List<WorkshopLevel> = buildLevels(store.current.workshop)

    fun level(id: String): WorkshopLevel? = currentLevels().firstOrNull { it.scenario.id == id }

    fun certificate(id: String): Certificate? = store.current.certificates.firstOrNull { it.id == id }

    /**
     * Guarda una reparación terminada. Recompensa completa la primera vez, una parte si mejora
     * las estrellas y un poco de XP por practicar. El certificado se emite la primera vez y se
     * vuelve a emitir cuando el resultado mejora.
     */
    fun completeRepair(outcome: RepairOutcome): RepairReward? {
        var result: RepairReward? = null
        val scenario = outcome.scenario
        store.update { data ->
            val user = data.user ?: return@update data
            val previous = data.workshop[scenario.id] ?: LevelProgress()
            val first = previous.completions == 0
            val improved = !first && outcome.stars > previous.bestStars

            val xp = when {
                first -> scenario.rewardXp
                improved -> scenario.rewardXp / 4
                else -> 10
            }
            val coins = when {
                first -> scenario.rewardCoins
                improved -> scenario.rewardCoins / 4
                else -> 0
            }
            val (newUser, newLevel) = user.withRewards(xp, coins, null)

            val progress = LevelProgress(
                completions = previous.completions + 1,
                bestStars = maxOf(previous.bestStars, outcome.stars),
                bestTimeSec = listOfNotNull(previous.bestTimeSec, outcome.timeUsedSec).minOrNull(),
            )

            val existing = data.certificates.firstOrNull { it.scenarioId == scenario.id }
            val certificates: List<Certificate>
            val certificateId: String
            if (existing == null || improved) {
                val cert = newCertificate(outcome, user.name)
                certificates = data.certificates.filterNot { it.scenarioId == scenario.id } + cert
                certificateId = cert.id
            } else {
                certificates = data.certificates
                certificateId = existing.id
            }

            result = RepairReward(
                reward = RewardResult(
                    title = "Nivel ${scenario.number}: ${scenario.title}",
                    xp = xp,
                    coins = coins,
                    newLevel = newLevel,
                ),
                certificateId = certificateId,
                firstCompletion = first,
                improved = improved,
            )
            missions.applyAction(
                data.copy(
                    user = newUser,
                    workshop = data.workshop + (scenario.id to progress),
                    certificates = certificates,
                ),
                MissionAction.COMPLETE_REPAIR,
            )
        }
        return result
    }

    private fun buildLevels(progress: Map<String, LevelProgress>): List<WorkshopLevel> {
        var previousDone = true
        return WorkshopCatalog.scenarios.map { scenario ->
            val p = progress[scenario.id] ?: LevelProgress()
            // Cada nivel se desbloquea al completar el anterior
            val level = WorkshopLevel(scenario, p, unlocked = previousDone)
            previousDone = p.completions > 0
            level
        }
    }

    private fun newCertificate(outcome: RepairOutcome, mechanic: String): Certificate {
        val scenario = outcome.scenario
        return Certificate(
            id = "MF-%04d-COL".format(Random.nextInt(1000, 10000)),
            scenarioId = scenario.id,
            title = scenario.title,
            category = scenario.category,
            virtualMoto = scenario.virtualMoto,
            faultCode = scenario.faultCode,
            mechanicName = mechanic,
            dateEpochDay = LocalDate.now().toEpochDay(),
            stars = outcome.stars,
            timeUsedSec = outcome.timeUsedSec,
            mistakes = outcome.mistakes,
            steps = scenario.steps.map { CertificateStep(it.doneTitle, it.tool.label, it.doneNote) },
        )
    }
}

/** Historial del diagnóstico por audio (RF05). */
class DiagnosisRepository(
    private val store: AppStore,
    private val missions: MissionsRepository,
) {
    val history: Flow<List<AudioDiagnosisRecord>> = store.data
        .map { data -> data.diagnoses.sortedByDescending { it.createdAtMillis } }
        .distinctUntilChanged()

    fun diagnosis(id: String): AudioDiagnosisRecord? = store.current.diagnoses.firstOrNull { it.id == id }

    /**
     * Guarda un diagnóstico. Solo las grabaciones reales dan XP y cuentan para las misiones;
     * los sonidos de ejemplo sirven para probar la función.
     */
    fun save(record: AudioDiagnosisRecord): RewardResult? {
        var reward: RewardResult? = null
        store.update { data ->
            val list = (listOf(record) + data.diagnoses).sortedByDescending { it.createdAtMillis }.take(MAX_HISTORY)
            val withRecord = data.copy(diagnoses = list)
            val user = data.user
            if (record.isDemo || user == null) return@update withRecord
            val (newUser, newLevel) = user.withRewards(XP_PER_DIAGNOSIS, 0, null)
            reward = RewardResult("Diagnóstico de Audio IA", XP_PER_DIAGNOSIS, 0, null, newLevel)
            missions.applyAction(withRecord.copy(user = newUser), MissionAction.AUDIO_DIAGNOSIS)
        }
        return reward
    }

    fun delete(id: String) {
        store.update { data -> data.copy(diagnoses = data.diagnoses.filterNot { it.id == id }) }
    }

    companion object {
        const val XP_PER_DIAGNOSIS = 50
        private const val MAX_HISTORY = 30
    }
}
