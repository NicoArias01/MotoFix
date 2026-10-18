package com.motofix.app.ui.screens.workshop

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.data.RepairScenario
import com.motofix.app.data.Tool
import com.motofix.app.data.WorkshopCatalog
import com.motofix.app.ui.components.ConfirmDialog
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.ProgressBar
import com.motofix.app.ui.components.SecondaryButton
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.StatusChip
import com.motofix.app.ui.components.formatNumber
import com.motofix.app.ui.components.icon
import com.motofix.app.ui.theme.Background
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.ErrorRed
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary

@Composable
fun RepairGameScreen(
    scenarioId: String,
    onExit: () -> Unit,
    onOpenCertificate: (String) -> Unit,
    onNextLevel: (String) -> Unit,
    viewModel: RepairGameViewModel = viewModel(key = "repair-$scenarioId") { RepairGameViewModel(scenarioId) },
) {
    val state = viewModel.state
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    // Si la app pasa a segundo plano, el reloj se detiene
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pause() }

    LaunchedEffect(state.feedback?.id) {
        if (state.feedback?.kind == FeedbackKind.ERROR && com.motofix.app.data.AppContainer.store.current.settings.haptics) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    BackHandler(enabled = state.phase == GamePhase.PLAYING || state.phase == GamePhase.PAUSED) {
        viewModel.pause()
        confirmExit = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when (state.phase) {
            GamePhase.BRIEFING -> BriefingContent(state.scenario, onStart = viewModel::start, onBack = onExit)
            GamePhase.PLAYING, GamePhase.PAUSED -> PlayingContent(
                state = state,
                onPause = viewModel::pause,
                onExit = {
                    viewModel.pause()
                    confirmExit = true
                },
                onSelectTool = viewModel::selectTool,
                onTapPart = viewModel::tapPart,
                onHint = viewModel::useHint,
            )
            GamePhase.SUCCESS -> SuccessContent(
                state = state,
                onNext = nextLevelId(state.scenario)?.let { next -> { onNextLevel(next) } },
                onCertificate = state.reward?.certificateId?.let { id -> { onOpenCertificate(id) } },
                onExit = onExit,
            )
            GamePhase.FAILED -> FailedContent(state, onRetry = viewModel::start, onExit = onExit)
        }

        if (state.phase == GamePhase.PAUSED && !confirmExit) {
            PauseOverlay(onResume = viewModel::resume, onExit = { confirmExit = true })
        }
    }

    if (confirmExit) {
        ConfirmDialog(
            title = "Abandonar reparación",
            message = "Perderás el progreso de este intento. ¿Quieres salir del taller?",
            confirmText = "Salir",
            destructive = true,
            onConfirm = {
                confirmExit = false
                onExit()
            },
            onDismiss = { confirmExit = false },
        )
    }
}

/** El siguiente nivel queda desbloqueado al completar el actual. */
private fun nextLevelId(current: RepairScenario): String? = WorkshopCatalog.next(current.id)?.id

private const val TOOL_COLUMNS = 3

// ---------- Antes de empezar ----------

@Composable
private fun BriefingContent(scenario: RepairScenario, onStart: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
        }
        Text("WORKSHOP PROTOCOL", style = MaterialTheme.typography.labelMedium, color = Orange)
        Text(
            "NIVEL ${scenario.number}: ${scenario.title.uppercase()}",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        StatusChip("● ${scenario.faultCode}", Orange)
        Spacer(Modifier.height(16.dp))

        MotoDiagram(
            parts = scenario.parts,
            onPartTap = {},
            enabled = false,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark),
        )

        Spacer(Modifier.height(16.dp))
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
            SectionLabel("Orden de trabajo")
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.TwoWheeler, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(scenario.virtualMoto, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Text(scenario.briefing, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(Icons.Outlined.Schedule, formatClock(scenario.timeLimitSec), TextPrimary)
                InfoPill(Icons.Outlined.TaskAlt, "${scenario.steps.size} pasos", TextPrimary)
                InfoPill(Icons.Outlined.MonetizationOn, "+${scenario.rewardCoins}", Orange)
                InfoPill(Icons.Outlined.Bolt, "+${scenario.rewardXp} XP", Green)
            }
        }

        Spacer(Modifier.height(12.dp))
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 16.dp, borderColor = Cyan.copy(alpha = 0.4f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SportsEsports, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                SectionLabel("Cómo se juega", color = Cyan)
            }
            Spacer(Modifier.height(8.dp))
            listOf(
                "1. Lee el objetivo de cada paso.",
                "2. Elige la herramienta correcta en la caja.",
                "3. Toca el componente correcto en el diagrama.",
                "Cada error resta ${RepairGameViewModel.PENALTY_SEC} s. La pista ayuda, pero baja tus estrellas.",
            ).forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(vertical = 2.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton("Iniciar reparación", onClick = onStart, trailingIcon = Icons.Outlined.PlayArrow)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = SurfaceRaised) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

// ---------- Jugando ----------

@Composable
private fun PlayingContent(
    state: RepairGameState,
    onPause: () -> Unit,
    onExit: () -> Unit,
    onSelectTool: (Tool) -> Unit,
    onTapPart: (com.motofix.app.data.Part) -> Unit,
    onHint: () -> Unit,
) {
    val scenario = state.scenario
    val step = state.currentStep
    val lowTime = state.timeLeftSec <= 20

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Encabezado: nivel, reloj y pausa
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) {
                Icon(Icons.Outlined.Close, contentDescription = "Salir", tint = TextSecondary)
            }
            Column(Modifier.weight(1f)) {
                Text("NIVEL ${scenario.number} · WORKSHOP", style = MaterialTheme.typography.labelSmall, color = Orange)
                Text(
                    scenario.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2,
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, if (lowTime) ErrorRed else Cyan.copy(alpha = 0.5f)),
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = if (lowTime) ErrorRed else Cyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatClock(state.timeLeftSec),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (lowTime) ErrorRed else Cyan,
                    )
                }
            }
            IconButton(onClick = onPause) {
                Icon(Icons.Outlined.Pause, contentDescription = "Pausar", tint = TextPrimary)
            }
        }

        StatusChip("● ${scenario.faultCode}", Orange)
        Spacer(Modifier.height(10.dp))

        // Diagrama
        Box {
            MotoDiagram(
                parts = scenario.parts,
                onPartTap = onTapPart,
                hintPart = if (state.hintActive) step?.part else null,
                feedback = state.feedback,
                enabled = state.phase == GamePhase.PLAYING,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark),
            )
            if (state.selectedTool != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Orange,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(state.selectedTool.icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Toca un componente", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                    }
                }
            }
        }

        // Objetivo del paso actual
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, Border),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Paso ${minOf(state.stepIndex + 1, state.totalSteps)} de ${state.totalSteps}", color = Cyan)
                    Spacer(Modifier.width(8.dp))
                    SectionLabel(
                        "· Errores: ${state.mistakes}",
                        color = if (state.mistakes == 0) TextMuted else ErrorRed,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onHint, enabled = !state.hintActive) {
                        Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = if (state.hintActive) TextMuted else Orange, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PISTA", style = MaterialTheme.typography.labelMedium, color = if (state.hintActive) TextMuted else Orange)
                    }
                }
                Text(step?.instruction ?: "", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                ProgressBar(state.stepIndex / state.totalSteps.toFloat(), color = Cyan)
                Spacer(Modifier.height(8.dp))
                val fb = state.feedback
                Text(
                    fb?.message ?: "Elige una herramienta y toca el componente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (fb?.kind) {
                        FeedbackKind.SUCCESS -> Green
                        FeedbackKind.ERROR -> ErrorRed
                        FeedbackKind.INFO -> Cyan
                        null -> TextMuted
                    },
                    modifier = Modifier.heightIn(min = 20.dp),
                )
            }
        }

        // Caja de herramientas
        Spacer(Modifier.height(12.dp))
        SectionLabel("Caja de herramientas")
        Spacer(Modifier.height(8.dp))
        val hintTool = if (state.hintActive) step?.tool else null
        scenario.toolbox.chunked(TOOL_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                row.forEach { tool ->
                    ToolCard(
                        tool = tool,
                        selected = state.selectedTool == tool,
                        hinted = hintTool == tool,
                        onClick = { onSelectTool(tool) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(TOOL_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ToolCard(tool: Tool, selected: Boolean, hinted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = when {
        selected -> Orange
        hinted -> Cyan
        else -> Border
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) Orange.copy(alpha = 0.18f) else SurfaceRaised,
        border = BorderStroke(if (selected || hinted) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(tool.icon, contentDescription = null, tint = if (selected) Orange else TextPrimary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                tool.label.uppercase(),
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Orange else TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onExit: () -> Unit) {
    // Tapa el diagrama para que la pausa no sirva para pensar con ventaja
    Box(
        Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Outlined.Pause, contentDescription = null, tint = Orange, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text("REPARACIÓN EN PAUSA", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Continuar", onClick = onResume, trailingIcon = Icons.Outlined.PlayArrow)
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Salir del taller", onClick = onExit, modifier = Modifier.fillMaxWidth(), uppercase = true)
        }
    }
}

// ---------- Resultado ----------

@Composable
private fun SuccessContent(
    state: RepairGameState,
    onNext: (() -> Unit)?,
    onCertificate: (() -> Unit)?,
    onExit: () -> Unit,
) {
    val scenario = state.scenario
    val reward = state.reward
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(Green.copy(alpha = 0.25f), SurfaceDark))),
        ) {
            Icon(Icons.Outlined.Verified, contentDescription = null, tint = Green, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("¡REPARACIÓN EXITOSA!", style = MaterialTheme.typography.headlineMedium, color = Green, textAlign = TextAlign.Center)
        Text(
            "${scenario.virtualMoto.uppercase()} | NIVEL ${scenario.number}",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Row {
            repeat(3) { i ->
                Icon(
                    if (i < state.stars) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = if (i < state.stars) Orange else TextMuted,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Text(
            "Tiempo: ${formatClock(state.timeUsedSec)} · Errores: ${state.mistakes} · Pistas: ${state.hintsUsed}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )

        Spacer(Modifier.height(20.dp))
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
            SectionLabel("Protocolo finalizado", color = Green)
            Spacer(Modifier.height(8.dp))
            scenario.steps.forEachIndexed { i, s ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${i + 1}. ${s.doneTitle}.", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }
        }

        if (reward != null) {
            Spacer(Modifier.height(12.dp))
            MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                SectionLabel("Recompensas obtenidas")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RewardValue(Icons.Outlined.MonetizationOn, "+${formatNumber(reward.reward.coins)}", "Monedas", Orange)
                    RewardValue(Icons.Outlined.Bolt, "+${reward.reward.xp}", "XP", Cyan)
                }
                val note = when {
                    reward.reward.newLevel != null -> "¡Subiste a nivel ${reward.reward.newLevel}!"
                    reward.firstCompletion -> "Primera vez completado: recompensa completa."
                    reward.improved -> "¡Mejoraste tu récord! Recibes un bono."
                    else -> "Práctica completada. Mejora tus estrellas para ganar más."
                }
                Spacer(Modifier.height(8.dp))
                Text(note, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(20.dp))
        if (onNext != null) {
            PrimaryButton("Siguiente nivel", onClick = onNext, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward)
            Spacer(Modifier.height(10.dp))
        }
        if (onCertificate != null) {
            SecondaryButton(
                "Ver certificado técnico",
                onClick = onCertificate,
                modifier = Modifier.fillMaxWidth(),
                uppercase = true,
                leading = { Icon(Icons.Outlined.Verified, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            Spacer(Modifier.height(10.dp))
        }
        TextButton(onClick = onExit) {
            Text("VOLVER AL TALLER", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        }
    }
}

@Composable
private fun RewardValue(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
        ) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
private fun FailedContent(state: RepairGameState, onRetry: () -> Unit, onExit: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text("REPARACIÓN FALLIDA", style = MaterialTheme.typography.headlineMedium, color = ErrorRed)
        Spacer(Modifier.height(8.dp))
        Text(
            "${state.failReason}. Completaste ${state.stepIndex} de ${state.totalSteps} pasos con ${state.mistakes} errores.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
            Text(
                "Consejo: usa la pista si te quedas atascado; cuesta estrellas pero no tiempo.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Reintentar", onClick = onRetry, trailingIcon = Icons.Outlined.Replay)
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Volver al taller", onClick = onExit, modifier = Modifier.fillMaxWidth(), uppercase = true)
    }
}
