package com.motofix.app.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.data.AppContainer
import com.motofix.app.data.auth.AuthProvider
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.MotoFixTextField
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.StatusChip
import com.motofix.app.ui.components.SystemFooter
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

/** Panel de control y ajustes (prototipo). */
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: ProfileViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(title = "Panel de control y ajustes", subtitle = "System_link: stable", subtitleColor = Cyan, onBack = onBack)

        SettingsSection("Apariencia visual")
        ToggleRow(
            Icons.Outlined.NightsStay,
            "Modo nocturno (Cyberpunk)",
            "Interfaz oscura de alto contraste para el taller.",
            checked = true,
            onChange = {},
            alwaysOn = true,
        )

        SettingsSection("Interacción y audio")
        ToggleRow(
            Icons.Outlined.Vibration,
            "Respuesta háptica",
            "Vibración cuando te equivocas en el taller",
            checked = settings.haptics,
            onChange = viewModel::setHaptics,
        )
        Spacer(Modifier.height(8.dp))
        ToggleRow(
            Icons.AutoMirrored.Outlined.VolumeUp,
            "Efectos de audio",
            "Reproducir los sonidos de ejemplo del diagnóstico IA",
            checked = settings.soundEffects,
            onChange = viewModel::setSoundEffects,
        )

        SettingsSection("Cuenta")
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (AppContainer.auth.isOnline) Icons.Outlined.Cloud else Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    tint = if (AppContainer.auth.isOnline) Green else Orange,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (AppContainer.auth.isOnline) "Cuenta en la nube (Firebase)" else "Cuenta guardada en este teléfono",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        if (AppContainer.auth.isOnline) "Puedes entrar con tu cuenta desde otro teléfono."
                        else "Al conectar Firebase podrás usar Google, GitHub y recuperar tu contraseña por correo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        SettingsSection("Soporte y ayuda")
        Faq(
            "¿Cómo funciona el diagnóstico de Audio IA?",
            "Graba 8 segundos del motor en ralentí. La app mide el ritmo de las explosiones, los golpes, chasquidos y siseos, y sugiere la falla más probable. Es orientativo y no reemplaza a un mecánico.",
        )
        Faq(
            "¿Cómo gano XP y monedas?",
            "Completando misiones diarias, el desafío semanal, los niveles del taller y grabando diagnósticos reales. Cada 1.000 XP subes de nivel.",
        )
        Faq(
            "¿Mis datos se mezclan con los de otra persona?",
            "No. Cada cuenta guarda su propio garaje, misiones y certificados.",
        )
        Faq(
            "¿Para qué sirven los certificados?",
            "Son el respaldo de las reparaciones que completaste en el taller virtual. Puedes guardarlos en PDF o compartirlos.",
        )

        SystemFooter(left = "MOTOFIX ENGINE V$version", right = "SISTEMA ACTIVO", modifier = Modifier.padding(vertical = 20.dp))
    }
}

@Composable
private fun SettingsSection(title: String) {
    Spacer(Modifier.height(20.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(14.dp).background(Orange))
        Spacer(Modifier.width(8.dp))
        SectionLabel(title)
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    alwaysOn: Boolean = false,
) {
    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceDark, border = BorderStroke(1.dp, Border)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceRaised),
            ) { Icon(icon, contentDescription = null, tint = Orange, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            if (alwaysOn) StatusChip("Siempre activo", Green)
            else Switch(checked = checked, onCheckedChange = onChange, colors = switchColors())
        }
    }
}

@Composable
private fun Faq(question: String, answer: String) {
    var open by rememberSaveable(question) { mutableStateOf(false) }
    Surface(
        onClick = { open = !open },
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, if (open) Orange.copy(alpha = 0.4f) else Border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(question, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = TextMuted)
            }
            AnimatedVisibility(open) {
                Text(answer, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

// ---------- Seguridad ----------

@Composable
fun SecurityScreen(onBack: () -> Unit, viewModel: SecurityViewModel = viewModel()) {
    val state = viewModel.state
    val user = AppContainer.store.current.user
    val provider = AuthProvider.entries.firstOrNull { it.name == user?.provider } ?: AuthProvider.PASSWORD

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(title = "Seguridad", subtitle = "Ajustes de cuenta", onBack = onBack)

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Shield, contentDescription = null, tint = Cyan, modifier = Modifier.size(84.dp))
            Spacer(Modifier.height(6.dp))
            StatusChip(if (AppContainer.auth.isOnline) "Protegida en la nube" else "Contraseña cifrada en el teléfono", Green)
        }
        Spacer(Modifier.height(20.dp))

        if (provider != AuthProvider.PASSWORD) {
            MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Cyan)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Entras con ${provider.label}, así que tu contraseña la gestiona ${provider.label}. Cámbiala desde tu cuenta de ${provider.label}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                }
            }
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Orange, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            SectionLabel("Actualizar credenciales", color = TextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        MotoFixCard(Modifier.fillMaxWidth()) {
            MotoFixTextField(
                value = state.current,
                onValueChange = viewModel::onCurrent,
                label = "Contraseña actual",
                placeholder = "••••••••",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
            )
            Spacer(Modifier.height(14.dp))
            MotoFixTextField(
                value = state.new,
                onValueChange = viewModel::onNew,
                label = "Nueva contraseña",
                placeholder = "Escribe la nueva contraseña",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
            )
            Spacer(Modifier.height(14.dp))
            MotoFixTextField(
                value = state.confirm,
                onValueChange = viewModel::onConfirm,
                label = "Confirmar contraseña",
                placeholder = "Repite la contraseña",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = viewModel::submit,
            )
            Spacer(Modifier.height(12.dp))
            Requirement("8 o más caracteres", state.hasLength)
            Requirement("Al menos un número", state.hasDigit)
            Requirement("Un símbolo (recomendado)", state.hasSymbol, optional = true)
            Requirement("Las contraseñas coinciden", state.matches)
        }
        state.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
        }
        if (state.success) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Contraseña actualizada.", color = Green, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            "Actualizar contraseña",
            onClick = viewModel::submit,
            loading = state.isLoading,
            enabled = state.canSubmit,
            trailingIcon = Icons.Outlined.Shield,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Requirement(text: String, ok: Boolean, optional: Boolean = false) {
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = when {
                ok -> Green
                optional -> TextMuted
                else -> TextSecondary
            },
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (ok) Green else TextMuted)
    }
}
