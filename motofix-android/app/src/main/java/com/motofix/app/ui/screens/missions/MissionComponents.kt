package com.motofix.app.ui.screens.missions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.motofix.app.data.Mission
import com.motofix.app.data.WeekDayStatus
import com.motofix.app.ui.components.ProgressBar
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.OnOrange
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary

/** Los 7 círculos de la semana (L M M J V S D) con la racha de entradas. */
@Composable
fun WeekStreakRow(week: List<WeekDayStatus>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val (bg, border) = when {
                    day.checkedIn -> Orange to Orange
                    day.isToday -> Color.Transparent to Cyan
                    else -> SurfaceRaised to Border
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .border(2.dp, border, CircleShape),
                ) {
                    if (day.checkedIn) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = OnOrange, modifier = Modifier.size(20.dp))
                    } else {
                        Text(day.letter, style = MaterialTheme.typography.labelMedium, color = if (day.isToday) Cyan else TextMuted)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (day.isToday) "HOY" else day.letter,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) Cyan else TextMuted,
                )
            }
        }
    }
}

/** Botón compacto "RECLAMAR". */
@Composable
fun ClaimButton(onClick: () -> Unit, modifier: Modifier = Modifier, text: String = "Reclamar") {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/** Tarjeta de una misión con su recompensa, progreso y botón de reclamar. */
@Composable
fun MissionCard(
    mission: Mission,
    onClaim: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showReward: Boolean = true,
) {
    val accent = when {
        mission.claimed -> TextMuted
        mission.isCompleted -> Green
        else -> Orange
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, if (mission.canClaim) Green.copy(alpha = 0.6f) else Border),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (mission.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (mission.isCompleted) Green else TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (showReward) {
                        val reward = buildString {
                            append("RECOMPENSA: +${mission.def.rewardXp} XP")
                            if (mission.def.rewardCoins > 0) append(" · +${mission.def.rewardCoins} MONEDAS")
                        }
                        Text(reward, style = MaterialTheme.typography.labelSmall, color = Green)
                    }
                    Text(
                        mission.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (mission.claimed) TextSecondary else TextPrimary,
                        textDecoration = if (mission.claimed) TextDecoration.LineThrough else null,
                    )
                    Text(mission.def.description, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                Spacer(Modifier.width(8.dp))
                when {
                    mission.canClaim && onClaim != null -> ClaimButton(onClaim)
                    mission.claimed -> Text("RECLAMADA", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    else -> Text(
                        "${mission.progress}/${mission.total}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(minOf(mission.progress, mission.total) / mission.total.toFloat(), color = accent)
        }
    }
}
