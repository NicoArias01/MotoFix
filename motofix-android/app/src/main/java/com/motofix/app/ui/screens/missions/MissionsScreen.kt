package com.motofix.app.ui.screens.missions

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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.data.MissionCatalog
import com.motofix.app.data.WeeklyChallenge
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.ProgressBar
import com.motofix.app.ui.components.RewardDialog
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.StatusChip
import com.motofix.app.ui.components.SystemFooter
import com.motofix.app.ui.components.rememberAppVersion
import com.motofix.app.ui.components.formatNumber
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary

@Composable
fun MissionsScreen(
    onBack: () -> Unit,
    onOpenWeekly: () -> Unit,
    viewModel: MissionsViewModel = viewModel(),
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = "Misiones de taller",
            subtitle = "Estatus de rendimiento: óptimo",
            subtitleColor = TextMuted,
            onBack = onBack,
            action = {
                StatusChip("🔥 Racha: ${overview.streak} ${if (overview.streak == 1) "día" else "días"}", Orange)
            },
        )

        // Racha semanal
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
            WeekStreakRow(overview.week)
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(6.dp), color = SurfaceRaised) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceDark),
                    ) {
                        Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = Orange)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (overview.canClaimStreakReward) "¡Racha de ${overview.streak} días completada!"
                            else "${overview.daysToStreakReward} ${if (overview.daysToStreakReward == 1) "día" else "días"} más para tu recompensa de racha",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                        Text(
                            "+${formatNumber(MissionCatalog.STREAK_REWARD_COINS)} MONEDAS · +${MissionCatalog.STREAK_REWARD_XP} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Orange,
                        )
                    }
                    if (overview.canClaimStreakReward) {
                        ClaimButton(viewModel::claimStreak)
                    } else {
                        Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = TextMuted)
                    }
                }
            }
        }

        // Misiones de hoy
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Orange, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("MISIONES DE HOY", style = MaterialTheme.typography.labelLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            SectionLabel("Reinicia en ${hoursUntilReset()}h", color = TextMuted)
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            overview.daily.forEach { mission ->
                MissionCard(mission, onClaim = { viewModel.claimDaily(mission.def.id) })
            }
        }

        // Desafío semanal
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("EVENTO DE TEMPORADA", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
        Spacer(Modifier.height(12.dp))
        WeeklyChallengeCard(overview.weekly, onClick = onOpenWeekly)

        SystemFooter(left = "MOTOFIX ENGINE V${rememberAppVersion()}", right = "STREAK_NODE: SYNCED", modifier = Modifier.padding(vertical = 20.dp))
    }

    viewModel.reward?.let { RewardDialog(it, onDismiss = viewModel::dismissReward) }
}

@Composable
fun WeeklyChallengeCard(challenge: WeeklyChallenge, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = Cyan)
                Spacer(Modifier.width(8.dp))
                Text("DESAFÍO SEMANAL", style = MaterialTheme.typography.labelLarge, color = Cyan, modifier = Modifier.weight(1f))
                when {
                    challenge.claimed -> StatusChip("Completado", Green, filled = true)
                    challenge.canClaim -> StatusChip("¡Reclamar!", Green, filled = true)
                    else -> StatusChip("Especial", Cyan, filled = true)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(challenge.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                "${challenge.completedObjectives}/${challenge.objectives.size} objetivos completados · " +
                    if (challenge.daysLeft <= 1) "termina hoy" else "quedan ${challenge.daysLeft} días",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RewardPill(Icons.Outlined.MonetizationOn, "+${challenge.rewardCoins}", Orange)
                RewardPill(Icons.Outlined.Bolt, "+${challenge.rewardXp} XP", Green)
                RewardPill(Icons.Outlined.MilitaryTech, "Insignia", Cyan)
            }
            Spacer(Modifier.height(12.dp))
            ProgressBar(challenge.progress, color = Cyan)
        }
    }
}

@Composable
private fun RewardPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}
