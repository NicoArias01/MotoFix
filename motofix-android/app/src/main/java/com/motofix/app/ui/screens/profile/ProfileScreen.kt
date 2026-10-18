package com.motofix.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.data.auth.AuthProvider
import com.motofix.app.ui.components.ConfirmDialog
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.MotoFixTextField
import com.motofix.app.ui.components.ProgressBar
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.StatusChip
import com.motofix.app.ui.components.SystemFooter
import com.motofix.app.ui.components.rememberAppVersion
import com.motofix.app.ui.components.formatNumber
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.ErrorRed
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.OnOrange
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.OrangeDark
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenCertificates: () -> Unit,
    onOpenGarage: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user ?: return
    var showRename by rememberSaveable { mutableStateOf(false) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = "Mi perfil de mecánico",
            onBack = null,
            action = {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = TextPrimary)
                }
            },
        )

        // Tarjeta del mecánico
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, Border),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Cyan.copy(alpha = 0.08f), Color.Transparent)))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(92.dp)
                        .border(2.dp, Cyan.copy(alpha = 0.7f), CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Orange, OrangeDark))),
                ) {
                    Text(user.initials, style = MaterialTheme.typography.headlineMedium, color = OnOrange)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.name.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    IconButton(onClick = { showRename = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar nombre", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
                StatusChip("⚡ Nivel ${user.level} · ${user.rank}", Orange, filled = true)
                Spacer(Modifier.height(6.dp))
                Text(memberSince(user.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(user.email, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                StatusChip("Cuenta ${state.provider.label}", Cyan)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    SectionLabel("Experiencia (XP) actual", modifier = Modifier.weight(1f))
                    Text("${user.xp} / ${user.xpForNextLevel}", style = MaterialTheme.typography.labelMedium, color = Cyan)
                }
                Spacer(Modifier.height(6.dp))
                ProgressBar(user.xp / user.xpForNextLevel.toFloat(), color = Orange)
            }
        }

        // Estadísticas
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(Icons.Outlined.MonetizationOn, formatNumber(user.coins), "Monedas", Orange, Modifier.weight(1f))
            StatTile(Icons.Outlined.EmojiEvents, "${state.unlockedAchievements}/${state.achievements.size}", "Logros", Orange, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(Icons.Outlined.Build, "${state.repairs}", "Reparaciones", Cyan, Modifier.weight(1f))
            StatTile(Icons.Outlined.Badge, "${state.certificates}", "Certificados", Green, Modifier.weight(1f))
        }

        // Logros
        Spacer(Modifier.height(20.dp))
        SectionLabel("Logros y medallas")
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.achievements.sortedByDescending { it.unlocked }.forEach { AchievementTile(it) }
        }

        // Opciones
        Spacer(Modifier.height(20.dp))
        SectionLabel("Opciones de cuenta y configuración")
        Spacer(Modifier.height(10.dp))
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 4.dp) {
            OptionRow(Icons.Outlined.Description, "Historial de certificados técnicos", "${state.certificates} emitidos", onOpenCertificates)
            OptionRow(Icons.Outlined.TwoWheeler, "Mi garaje personal de motos", "${state.motorcycles} motos", onOpenGarage)
            OptionRow(
                Icons.Outlined.Notifications,
                "Notificaciones y alertas",
                if (state.settings.notifications) "Activadas" else "Desactivadas",
                onClick = { viewModel.setNotifications(!state.settings.notifications) },
                trailing = {
                    Switch(
                        checked = state.settings.notifications,
                        onCheckedChange = viewModel::setNotifications,
                        colors = switchColors(),
                    )
                },
            )
            OptionRow(
                Icons.Outlined.Lock,
                "Cambiar contraseña y seguridad",
                if (state.provider == AuthProvider.PASSWORD) "Contraseña de tu cuenta" else "Gestionada por ${state.provider.label}",
                onOpenSecurity,
            )
            OptionRow(Icons.Outlined.Settings, "Panel de control y ajustes", "Vibración, sonidos y ayuda", onOpenSettings)
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = { confirmLogout = true },
            shape = RoundedCornerShape(10.dp),
            color = ErrorRed.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ErrorRed)
                Spacer(Modifier.width(12.dp))
                Text("CERRAR SESIÓN", style = MaterialTheme.typography.labelLarge, color = ErrorRed)
            }
        }
        SystemFooter(left = "PROFILE_NODE: SYNCED", right = "MOTOFIX V${rememberAppVersion()}", modifier = Modifier.padding(vertical = 20.dp))
    }

    if (showRename) {
        RenameDialog(
            current = user.name,
            onSave = viewModel::rename,
            onDismiss = { showRename = false },
        )
    }
    if (confirmLogout) {
        ConfirmDialog(
            title = "Cerrar sesión",
            message = "¿Estás seguro de que deseas desconectar tu terminal de MotoFix? Tus datos quedan guardados en tu cuenta.",
            confirmText = "Cerrar sesión",
            destructive = true,
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
            onDismiss = { confirmLogout = false },
        )
    }
}

@Composable
fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Orange,
    uncheckedThumbColor = TextSecondary,
    uncheckedTrackColor = SurfaceRaised,
    uncheckedBorderColor = Border,
)

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = SurfaceDark, border = BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary, maxLines = 1)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        }
    }
}

@Composable
private fun AchievementTile(a: Achievement) {
    val color = if (a.unlocked) Orange else TextMuted
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, if (a.unlocked) Orange.copy(alpha = 0.5f) else Border),
        modifier = Modifier.width(112.dp),
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (a.unlocked) 0.18f else 0.08f)),
            ) {
                Icon(a.icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                a.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (a.unlocked) TextPrimary else TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            if (a.unlocked) {
                StatusChip("✓ Logrado", Green)
            } else {
                Text(
                    "${minOf(a.progress, a.target)}/${a.target}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(a.description, style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center, maxLines = 3, minLines = 3)
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceRaised),
            ) {
                Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            trailing?.invoke() ?: Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
        }
    }
}

@Composable
private fun RenameDialog(current: String, onSave: (String) -> String?, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(current) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("EDITAR NOMBRE", style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
        text = {
            MotoFixTextField(
                value = text,
                onValueChange = { text = it.take(30); error = null },
                label = "Nombre o apodo",
                placeholder = "Tu nombre de mecánico",
                leadingIcon = Icons.Outlined.Edit,
                errorText = error,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                error = onSave(text)
                if (error == null) onDismiss()
            }) { Text("GUARDAR", color = Orange, style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = TextSecondary, style = MaterialTheme.typography.labelLarge) }
        },
    )
}
