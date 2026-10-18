package com.motofix.app.ui.screens.workshop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motofix.app.data.AppContainer
import com.motofix.app.data.Certificate
import com.motofix.app.ui.components.MotoFixTextField
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SecondaryButton
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.StatusChip
import com.motofix.app.ui.components.SystemFooter
import com.motofix.app.ui.components.formatDate
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary
import com.motofix.app.util.shareCertificatePdf

/** Detalle de un certificado técnico (prototipo "Certificado técnico"). */
@Composable
fun CertificateScreen(certificateId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val certificates by AppContainer.workshop.certificates.collectAsStateWithLifecycle(AppContainer.store.current.certificates)
    val cert = certificates.firstOrNull { it.id == certificateId }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = "Certificado técnico",
            onBack = onBack,
            action = if (cert != null) {
                {
                    IconButton(onClick = { shareCertificatePdf(context, cert) }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Compartir", tint = Cyan)
                    }
                }
            } else null,
        )
        if (cert == null) {
            Text("Este certificado ya no existe.", color = TextSecondary, modifier = Modifier.padding(16.dp))
            return@Column
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, Green.copy(alpha = 0.5f)),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("Registro oficial")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Description, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("#${cert.id}", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        }
                    }
                    StatusChip("Verificado", Green)
                }
                Spacer(Modifier.height(14.dp))
                SectionLabel("Vehículo certificado", color = Cyan)
                Text(cert.virtualMoto.uppercase(), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("● ${cert.title} · ${cert.category}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Text(" ${formatDate(cert.dateEpochDay)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Row {
                        repeat(cert.stars) {
                            Icon(Icons.Outlined.Star, contentDescription = null, tint = Orange, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text("Mecánico: ${cert.mechanicName}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel("Desglose paso a paso")
                Spacer(Modifier.height(8.dp))
                cert.steps.forEachIndexed { i, step ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, Cyan.copy(alpha = 0.6f), CircleShape),
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = Cyan)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(step.title.uppercase(), style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            Text("Herramienta: ${step.tool}", style = MaterialTheme.typography.labelSmall, color = Orange)
                            Text(step.note, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Cyan.copy(alpha = 0.06f))
                        .border(1.dp, Cyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Memory, contentDescription = null, tint = Cyan)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Certificado emitido por el simulador de MotoFix. Falla atendida: ${cert.faultCode}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Este documento sirve como respaldo de tu aprendizaje técnico en MotoFix. No reemplaza la certificación de un centro autorizado.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            "Guardar certificado o compartir",
            onClick = { shareCertificatePdf(context, cert) },
            trailingIcon = Icons.Outlined.Share,
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Lista de certificados con búsqueda (prototipo "Historial de certificados"). */
@Composable
fun CertificatesScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onGoToWorkshop: () -> Unit) {
    val context = LocalContext.current
    val certificates by AppContainer.workshop.certificates.collectAsStateWithLifecycle(AppContainer.store.current.certificates)
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = certificates.filter {
        query.isBlank() || listOf(it.id, it.virtualMoto, it.title).any { field -> field.contains(query.trim(), ignoreCase = true) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(title = "Historial de certificados", subtitle = "${certificates.size} emitidos", onBack = onBack)
            MotoFixTextField(
                value = query,
                onValueChange = { query = it },
                label = "Buscar",
                placeholder = "ID, moto o reparación...",
                leadingIcon = Icons.Outlined.Search,
            )
        }
        if (certificates.isEmpty()) {
            item {
                Column(Modifier.padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Verified, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Aún no tienes certificados. Completa una reparación en el taller para obtener el primero.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Ir al taller", onClick = onGoToWorkshop, trailingIcon = Icons.Outlined.Build)
                }
            }
        }
        items(filtered, key = { it.id }) { cert ->
            CertificateCard(cert, onOpen = { onOpen(cert.id) }, onPdf = { shareCertificatePdf(context, cert) })
        }
        item {
            SystemFooter(
                left = "${certificates.size} CERTIFICADOS OFICIALES",
                right = "END_OF_RECORDS",
                modifier = Modifier.padding(vertical = 20.dp),
            )
        }
    }
}

@Composable
private fun CertificateCard(cert: Certificate, onOpen: () -> Unit, onPdf: () -> Unit) {
    val accent = when (cert.stars) {
        3 -> Green
        2 -> Cyan
        else -> Orange
    }
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, Border),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accent)
            )
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("ID_certificado", color = TextMuted)
                        Text("#${cert.id}", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                    StatusChip("✓ Verificado", Green)
                }
                Spacer(Modifier.height(8.dp))
                Text(cert.virtualMoto.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(cert.title, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Text(" ${formatDate(cert.dateEpochDay)}   ", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    repeat(cert.stars) {
                        Icon(Icons.Outlined.Star, contentDescription = null, tint = Orange, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    "Ver / descargar PDF",
                    onClick = onPdf,
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = Cyan,
                    borderColor = Cyan.copy(alpha = 0.5f),
                    uppercase = true,
                    leading = { Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }
    }
}
