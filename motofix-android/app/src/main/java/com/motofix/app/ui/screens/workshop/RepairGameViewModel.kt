package com.motofix.app.ui.screens.workshop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motofix.app.data.AppContainer
import com.motofix.app.data.Part
import com.motofix.app.data.RepairOutcome
import com.motofix.app.data.RepairReward
import com.motofix.app.data.RepairScenario
import com.motofix.app.data.RepairStep
import com.motofix.app.data.Tool
import com.motofix.app.data.WorkshopCatalog
import com.motofix.app.data.WorkshopRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class GamePhase { BRIEFING, PLAYING, PAUSED, SUCCESS, FAILED }

enum class FeedbackKind { INFO, SUCCESS, ERROR }

/** Mensaje corto tras cada acción. `id` cambia siempre para poder animarlo aunque se repita. */
data class Feedback(val message: String, val kind: FeedbackKind, val part: Part?, val id: Long)

data class RepairGameState(
    val scenario: RepairScenario,
    val phase: GamePhase = GamePhase.BRIEFING,
    val timeLeftSec: Int = scenario.timeLimitSec,
    val stepIndex: Int = 0,
    val selectedTool: Tool? = null,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    /** Se usó la pista en el paso actual: se resalta la herramienta y el componente. */
    val hintActive: Boolean = false,
    val feedback: Feedback? = null,
    val stars: Int = 0,
    val reward: RepairReward? = null,
    val failReason: String = "",
) {
    val currentStep: RepairStep? get() = scenario.steps.getOrNull(stepIndex)
    val totalSteps: Int get() = scenario.steps.size
    val timeUsedSec: Int get() = scenario.timeLimitSec - timeLeftSec
    val isRunning: Boolean get() = phase == GamePhase.PLAYING
}

/**
 * Lógica del juego de reparación (RF09):
 * 1. Se elige una herramienta de la caja.
 * 2. Se toca el componente correcto en el diagrama.
 * Los errores restan tiempo; la pista ayuda pero baja la calificación.
 */
class RepairGameViewModel(
    scenarioId: String,
    private val workshop: WorkshopRepository = AppContainer.workshop,
) : ViewModel() {

    private val scenario = WorkshopCatalog.find(scenarioId) ?: WorkshopCatalog.scenarios.first()

    var state by mutableStateOf(RepairGameState(scenario))
        private set

    private var timerJob: Job? = null
    private var feedbackCounter = 0L

    fun start() {
        state = RepairGameState(scenario, phase = GamePhase.PLAYING)
        startTimer()
    }

    fun pause() {
        if (state.phase != GamePhase.PLAYING) return
        state = state.copy(phase = GamePhase.PAUSED)
        timerJob?.cancel()
    }

    fun resume() {
        if (state.phase != GamePhase.PAUSED) return
        state = state.copy(phase = GamePhase.PLAYING)
        startTimer()
    }

    fun selectTool(tool: Tool) {
        if (!state.isRunning) return
        state = state.copy(selectedTool = if (state.selectedTool == tool) null else tool)
    }

    fun tapPart(part: Part) {
        if (!state.isRunning) return
        val step = state.currentStep ?: return
        val tool = state.selectedTool
        when {
            tool == null -> feedback("${part.label}: elige primero una herramienta de la caja", FeedbackKind.INFO, part)
            tool == step.tool && part == step.part -> completeStep(step)
            else -> {
                val message = when {
                    tool == step.tool -> "Ese no es el componente correcto (-$PENALTY_SEC s)"
                    part == step.part -> "${tool.label} no sirve para esto (-$PENALTY_SEC s)"
                    else -> "Acción incorrecta (-$PENALTY_SEC s)"
                }
                val left = (state.timeLeftSec - PENALTY_SEC).coerceAtLeast(0)
                state = state.copy(mistakes = state.mistakes + 1, timeLeftSec = left)
                feedback(message, FeedbackKind.ERROR, part)
                if (left == 0) fail("Se acabó el tiempo")
            }
        }
    }

    fun useHint() {
        if (!state.isRunning || state.hintActive) return
        val step = state.currentStep ?: return
        state = state.copy(hintsUsed = state.hintsUsed + 1, hintActive = true)
        feedback("Pista: usa ${step.tool.label} en ${step.part.label}", FeedbackKind.INFO, null)
    }

    private fun completeStep(step: RepairStep) {
        val next = state.stepIndex + 1
        state = state.copy(stepIndex = next, selectedTool = null, hintActive = false)
        feedback("✓ ${step.doneTitle}", FeedbackKind.SUCCESS, step.part)
        if (next >= scenario.steps.size) succeed()
    }

    private fun succeed() {
        timerJob?.cancel()
        val penalty = state.mistakes + state.hintsUsed * 2
        val stars = when {
            penalty == 0 -> 3
            penalty <= 2 -> 2
            else -> 1
        }
        val reward = workshop.completeRepair(RepairOutcome(scenario, stars, state.timeUsedSec, state.mistakes))
        state = state.copy(phase = GamePhase.SUCCESS, stars = stars, reward = reward)
    }

    private fun fail(reason: String) {
        timerJob?.cancel()
        state = state.copy(phase = GamePhase.FAILED, failReason = reason, selectedTool = null)
    }

    private fun feedback(message: String, kind: FeedbackKind, part: Part?) {
        feedbackCounter++
        state = state.copy(feedback = Feedback(message, kind, part, feedbackCounter))
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (state.phase == GamePhase.PLAYING) {
                delay(1_000)
                if (state.phase != GamePhase.PLAYING) break
                val left = state.timeLeftSec - 1
                state = state.copy(timeLeftSec = left.coerceAtLeast(0))
                if (left <= 0) {
                    fail("Se acabó el tiempo")
                    break
                }
            }
        }
    }

    companion object {
        const val PENALTY_SEC = 5
    }
}

fun formatClock(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
