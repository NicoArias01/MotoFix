package com.motofix.app.ui.screens.workshop

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.data.AppContainer
import com.motofix.app.data.WorkshopLevel
import com.motofix.app.data.WorkshopRepository
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.ProgressBar
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.StatusChip
import com.motofix.app.ui.components.SystemFooter
import com.motofix.app.ui.components.rememberAppVersion
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class WorkshopUiState(val levels: List<WorkshopLevel>, val certificates: Int) {
    val completed: Int get() = levels.count { it.completed }
    val stars: Int get() = levels.sumOf { it.progress.bestStars }
}

class WorkshopViewModel(workshop: WorkshopRepository = AppContainer.workshop) : ViewModel() {
    val state: StateFlow<WorkshopUiState> = combine(workshop.levels, workshop.certificates) { levels, certs ->
        WorkshopUiState(levels, certs.size)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WorkshopUiState(workshop.currentLevels(), AppContainer.store.current.certificates.size),
    )
}

@Composable
fun WorkshopScreen(
    onPlay: (String) -> Unit,
    onOpenCertificates: () -> Unit,
    viewModel: WorkshopViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(title = "Taller de reparación", subtitle = "Workshop protocol", onBack = null)

        // Resumen
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
            Box(Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Outlined.Build,
                    contentDescription = null,
                    tint = TextMuted.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.TopEnd),
                )
                Column(Modifier.padding(end = 56.dp)) {
                    Text("APRENDE REPARANDO", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Resuelve fallas reales con herramientas virtuales, contra reloj. Cada reparación completada te da un certificado técnico.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryBox("Niveles", "${state.completed}/${state.levels.size}", Modifier.weight(1f))
                SummaryBox("Estrellas", "${state.stars}/${state.levels.size * 3}", Modifier.weight(1f))
                SummaryBox("Certificados", "${state.certificates}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(if (state.levels.isEmpty()) 0f else state.completed / state.levels.size.toFloat(), color = Orange)
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Órdenes de trabajo")
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.levels.forEach { level ->
                LevelCard(level, onPlay = { onPlay(level.scenario.id) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            onClick = onOpenCertificates,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, Green.copy(alpha = 0.4f)),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Verified, contentDescription = null, tint = Green)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("HISTORIAL DE CERTIFICADOS", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Text(
                        "${state.certificates} certificados técnicos emitidos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
            }
        }

        SystemFooter(left = "WORKSHOP_NODE: ACTIVE", right = "MOTOFIX ENGINE V${rememberAppVersion()}", modifier = Modifier.padding(vertical = 20.dp))
    }
}

@Composable
private fun SummaryBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(6.dp), color = SurfaceRaised) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = TextMuted,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun LevelCard(level: WorkshopLevel, onPlay: () -> Unit) {
    val scenario = level.scenario
    val accent = when {
        !level.unlocked -> TextMuted
        level.completed -> Green
        else -> Orange
    }
    Surface(
        onClick = onPlay,
        enabled = level.unlocked,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, if (level.unlocked && !level.completed) Orange.copy(alpha = 0.5f) else Border),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.25f), SurfaceRaised))),
            ) {
                if (level.unlocked) {
                    Text("${scenario.number}", style = MaterialTheme.typography.headlineMedium, color = accent)
                } else {
                    Icon(Icons.Outlined.Lock, contentDescription = "Bloqueado", tint = TextMuted)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "NIVEL ${scenario.number} · ${scenario.category.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
                Text(
                    scenario.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (level.unlocked) TextPrimary else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(scenario.virtualMoto, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                if (level.unlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) { i ->
                            Icon(
                                if (i < level.progress.bestStars) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                tint = if (i < level.progress.bestStars) Orange else TextMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                        Text(
                            " ${level.progress.bestTimeSec?.let { "Récord ${formatClock(it)}" } ?: formatClock(scenario.timeLimitSec)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                    }
                    if (!level.completed) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RewardText(Icons.Outlined.MonetizationOn, "+${scenario.rewardCoins}", Orange)
                            RewardText(Icons.Outlined.Bolt, "+${scenario.rewardXp} XP", Cyan)
                        }
                    }
                } else {
                    Text("Completa el nivel ${scenario.number - 1} para desbloquear", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            if (level.unlocked) {
                if (level.completed) {
                    StatusChip("Repetir", Green)
                } else {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = "Jugar", tint = Orange, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun RewardText(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
