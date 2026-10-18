package com.motofix.app.ui.screens.missions

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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.ProgressBar
import com.motofix.app.ui.components.RewardDialog
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.StatusChip
import com.motofix.app.ui.components.formatNumber
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary

@Composable
fun WeeklyChallengeScreen(
    onBack: () -> Unit,
    viewModel: MissionsViewModel = viewModel(),
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val challenge = overview.weekly

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = "Desafío semanal",
            subtitle = "Sistema activo // misión_id: ${challenge.code}",
            onBack = onBack,
        )
        Text(challenge.subtitle.uppercase(), style = MaterialTheme.typography.labelMedium, color = TextSecondary)

        Spacer(Modifier.height(12.dp))
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Orange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            challenge.claimed -> "COMPLETADO"
                            challenge.isCompleted -> "LISTO PARA RECLAMAR"
                            else -> "EN PROGRESO"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                    )
                    Text(
                        if (challenge.daysLeft <= 1) "Termina hoy a la medianoche" else "Termina en ${challenge.daysLeft} días",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
                StatusChip("Especial", Cyan)
            }
        }

        // Imagen principal + progreso total
        Spacer(Modifier.height(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1B1035), Color(0xFF0B2A33), SurfaceDark))),
        ) {
            Icon(Icons.Outlined.Build, contentDescription = null, tint = Cyan.copy(alpha = 0.8f), modifier = Modifier.size(84.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row {
            SectionLabel("Progreso total", modifier = Modifier.weight(1f))
            Text("${(challenge.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = Orange)
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(challenge.progress, color = Orange)
        Spacer(Modifier.height(12.dp))
        Text(challenge.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

        // Recompensas
        Spacer(Modifier.height(20.dp))
        SectionLabel("Recompensas estimadas")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RewardColumn(Icons.Outlined.Bolt, "+${challenge.rewardXp} XP", "Experiencia", Green, Modifier.weight(1f))
            RewardColumn(Icons.Outlined.MonetizationOn, formatNumber(challenge.rewardCoins), "Monedas", Orange, Modifier.weight(1f))
            RewardColumn(Icons.Outlined.MilitaryTech, "PRO", "Insignia", Cyan, Modifier.weight(1f))
        }

        // Objetivos
        Spacer(Modifier.height(24.dp))
        Row {
            SectionLabel("Objetivos de misión", modifier = Modifier.weight(1f))
            SectionLabel("${challenge.completedObjectives}/${challenge.objectives.size}", color = TextMuted)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            challenge.objectives.forEach { objective ->
                MissionCard(objective, onClaim = null, showReward = false)
            }
        }

        Spacer(Modifier.height(20.dp))
        when {
            challenge.claimed -> Text(
                "¡Desafío completado! Vuelve el lunes para un nuevo desafío.",
                style = MaterialTheme.typography.bodyMedium,
                color = Green,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> PrimaryButton(
                text = if (challenge.canClaim) "Reclamar recompensas" else "Completa todos los objetivos",
                onClick = viewModel::claimWeekly,
                enabled = challenge.canClaim,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Los objetivos avanzan solos cuando usas tu garaje. El desafío se reinicia cada lunes.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    viewModel.reward?.let { RewardDialog(it, onDismiss = viewModel::dismissReward) }
}

@Composable
private fun RewardColumn(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = SurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
