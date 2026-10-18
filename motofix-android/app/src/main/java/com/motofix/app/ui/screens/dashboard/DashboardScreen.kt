package com.motofix.app.ui.screens.dashboard

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.motofix.app.data.FakeData
import com.motofix.app.data.Mission
import com.motofix.app.data.UserProfile
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.ui.components.CoinBadge
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.ProgressBar
import com.motofix.app.ui.components.SecondaryButton
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.SystemFooter
import com.motofix.app.ui.components.rememberAppVersion
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.OrangeDark
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    onOpenWorkshop: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenGarage: () -> Unit,
    onOpenDealer: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMissions: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user ?: return
    val missions = state.missions

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        DashboardHeader(user, onOpenNotifications)

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Progreso de rango")
            Text("${user.xp} / ${user.xpForNextLevel} XP", style = MaterialTheme.typography.labelMedium, color = Green)
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(user.xp / user.xpForNextLevel.toFloat())

        Spacer(Modifier.height(20.dp))
        WorkshopCard(onOpenWorkshop)

        Spacer(Modifier.height(16.dp))
        AudioDiagnosisCard(onOpenAudio)

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShortcutTile(
                title = "Mi garaje",
                subtitle = if (state.motorcycles == 1) "1 moto" else "${state.motorcycles} motos",
                icon = Icons.Outlined.TwoWheeler,
                onClick = onOpenGarage,
                modifier = Modifier.weight(1f),
            )
            ShortcutTile(
                title = "Concesionario",
                subtitle = "Próximamente",
                icon = Icons.Outlined.Storefront,
                onClick = onOpenDealer,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))
        MissionsPreview(missions, state.pendingClaims, onOpenMissions)

        SystemFooter(left = "MOTOFIX ENGINE V${rememberAppVersion()}", right = "SYNC: OK", modifier = Modifier.padding(vertical = 20.dp))
    }
}

@Composable
private fun DashboardHeader(user: UserProfile, onOpenNotifications: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Orange, OrangeDark))),
        ) {
            Text(user.initials, style = MaterialTheme.typography.titleMedium, color = Color.Black)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(
                "⚡ Nivel ${user.level} · ${user.rank}",
                style = MaterialTheme.typography.labelSmall,
                color = Orange,
            )
        }
        IconButton(onClick = onOpenNotifications) {
            BadgedBox(badge = {
                if (user.unreadNotifications > 0) Badge(containerColor = Orange) { Text("${user.unreadNotifications}") }
            }) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = TextPrimary)
            }
        }
        CoinBadge(user.coins)
    }
}

@Composable
private fun WorkshopCard(onClick: () -> Unit) {
    MotoFixCard(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            Icon(
                Icons.Outlined.Build,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.35f),
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.TopEnd),
            )
            Column(Modifier.padding(end = 56.dp)) {
                Text("TALLER DE\nREPARACIÓN", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Resuelve fallas reales con herramientas virtuales, contra reloj, y gana certificados técnicos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Entrar al taller",
            onClick = onClick,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            height = 48.dp,
        )
    }
}

@Composable
private fun AudioDiagnosisCard(onClick: () -> Unit) {
    MotoFixCard(Modifier.fillMaxWidth(), borderColor = Cyan.copy(alpha = 0.35f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            SectionLabel("Diagnóstico por IA", color = Cyan)
        }
        Spacer(Modifier.height(10.dp))
        Text("¿Tu moto real suena extraña?", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "Nuestra IA analiza el sonido del motor para detectar posibles fallas mecánicas.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        SecondaryButton(
            text = "Grabar sonido de falla",
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            contentColor = Cyan,
            borderColor = Cyan.copy(alpha = 0.5f),
            uppercase = true,
            leading = { Icon(Icons.Outlined.Mic, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
    }
}

@Composable
private fun ShortcutTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(10.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, Border),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(SurfaceRaised, SurfaceDark)))
                .padding(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.TopEnd),
            )
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = Orange, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun MissionsPreview(missions: List<Mission>, pendingClaims: Int, onOpenMissions: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onOpenMissions)
            .padding(vertical = 4.dp),
    ) {
        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Orange, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("MISIONES DE HOY", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        if (pendingClaims > 0) {
            Spacer(Modifier.width(8.dp))
            Badge(containerColor = Green) { Text("$pendingClaims") }
        }
        Spacer(Modifier.weight(1f))
        Text("VER TODO", style = MaterialTheme.typography.labelMedium, color = Orange)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Orange, modifier = Modifier.size(18.dp))
    }
    Spacer(Modifier.height(12.dp))
    missions.forEach { mission ->
        MotoFixCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenMissions),
            contentPadding = 14.dp,
            borderColor = if (mission.canClaim) Green.copy(alpha = 0.6f) else Border,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (mission.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (mission.isCompleted) Green else TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("RECOMPENSA: +${mission.def.rewardXp} XP", style = MaterialTheme.typography.labelSmall, color = Green)
                    Text(
                        mission.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (mission.claimed) TextSecondary else TextPrimary,
                        textDecoration = if (mission.claimed) TextDecoration.LineThrough else null,
                    )
                }
                when {
                    mission.canClaim -> Text("¡RECLAMAR!", style = MaterialTheme.typography.labelMedium, color = Green)
                    mission.claimed -> Text("RECLAMADA", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    else -> Text("${mission.progress}/${mission.total}", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(mission.progress / mission.total.toFloat(), color = Orange)
        }
    }
}
