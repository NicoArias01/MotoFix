package com.motofix.app.ui.screens.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motofix.app.data.MaintenanceRecord
import com.motofix.app.data.OilStatus
import com.motofix.app.ui.components.StatusChip
import com.motofix.app.ui.components.formatCop
import com.motofix.app.ui.components.formatDate
import com.motofix.app.ui.components.formatKm
import com.motofix.app.ui.components.formatThousands
import com.motofix.app.ui.components.icon
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

// ---------- Estado del aceite ----------

val OilStatus.color: Color
    get() = when (this) {
        is OilStatus.Ok -> Green
        is OilStatus.DueSoon -> Orange
        is OilStatus.Overdue -> ErrorRed
        OilStatus.NoRecord -> TextSecondary
    }

val OilStatus.statusIcon: ImageVector
    get() = when (this) {
        is OilStatus.Ok -> Icons.Outlined.CheckCircle
        is OilStatus.DueSoon -> Icons.Outlined.WarningAmber
        is OilStatus.Overdue -> Icons.Outlined.ErrorOutline
        OilStatus.NoRecord -> Icons.AutoMirrored.Outlined.HelpOutline
    }

/** Texto corto para la tarjeta del garaje. */
val OilStatus.shortLabel: String
    get() = when (this) {
        is OilStatus.Ok -> "Sistemas al 100%"
        is OilStatus.DueSoon -> "Cambio de aceite en ${formatKm(kmRemaining)}"
        is OilStatus.Overdue -> "Aceite vencido hace ${formatKm(kmOver)}"
        OilStatus.NoRecord -> "Sin registro de aceite"
    }

/** Texto largo para el detalle de la moto. */
val OilStatus.longLabel: String
    get() = when (this) {
        is OilStatus.Ok -> "Próximo cambio de aceite en ${formatKm(kmRemaining)}."
        is OilStatus.DueSoon -> "¡Atención! Solo faltan ${formatKm(kmRemaining)} para el cambio de aceite."
        is OilStatus.Overdue -> "El cambio de aceite está vencido por ${formatKm(kmOver)}. Agéndalo cuanto antes."
        OilStatus.NoRecord -> "Registra el último cambio de aceite para recibir avisos de mantenimiento."
    }

/** Qué tanto se ha "gastado" el aceite actual (0 recién cambiado, 1 vencido). */
val OilStatus.usage: Float
    get() = when (this) {
        is OilStatus.Ok -> 1f - kmRemaining / OilStatus.OIL_INTERVAL_KM.toFloat()
        is OilStatus.DueSoon -> 1f - kmRemaining / OilStatus.OIL_INTERVAL_KM.toFloat()
        is OilStatus.Overdue -> 1f
        OilStatus.NoRecord -> 0f
    }

// ---------- Botones pequeños de las tarjetas ----------

@Composable
fun SmallActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
    filled: Boolean = false,
) {
    val shape = RoundedCornerShape(4.dp)
    val padding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(34.dp),
            shape = shape,
            contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.Black),
        ) { Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(34.dp),
            shape = shape,
            contentPadding = padding,
            border = BorderStroke(1.dp, Border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        ) { Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
    }
}

// ---------- Tarjeta de moto del garaje ----------

@Composable
fun MotorcycleCard(
    item: GarageItem,
    onOpen: () -> Unit,
    onReviewAi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moto = item.moto
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, Border),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Franja de color con el estado de la moto
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(item.oil.color)
            )
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceRaised),
                ) {
                    Icon(moto.type.icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        moto.displayName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${moto.type.label} · ${moto.year} · ${moto.displacementCc}cc".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(item.oil.statusIcon, contentDescription = null, tint = item.oil.color, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(item.oil.shortLabel, style = MaterialTheme.typography.labelSmall, color = item.oil.color)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionButton("Historial", onOpen, Modifier.weight(1f))
                        SmallActionButton("Revisar IA", onReviewAi, Modifier.weight(1f), color = Cyan, filled = true)
                    }
                }
            }
        }
    }
}

// ---------- Registro de mantenimiento ----------

@Composable
fun MaintenanceRecordRow(
    record: MaintenanceRecord,
    modifier: Modifier = Modifier,
    motoName: String? = null,
    onDelete: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, Border),
    ) {
        Row(Modifier.padding(12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Orange.copy(alpha = 0.12f)),
            ) {
                Icon(record.type.icon, contentDescription = null, tint = Orange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (motoName != null) {
                    Text(motoName.uppercase(), style = MaterialTheme.typography.labelSmall, color = Cyan)
                }
                Text(record.type.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "${formatDate(record.dateEpochDay)} · ${formatThousands(record.mileageKm)} km".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                val extra = listOfNotNull(
                    record.workshop.takeIf { it.isNotBlank() },
                    record.costCop?.let { formatCop(it) },
                ).joinToString(" · ")
                if (extra.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(extra, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                if (record.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(record.notes, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar registro", tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** Pequeño bloque de estadística: etiqueta arriba y valor abajo. */
@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = SurfaceRaised,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, maxLines = 1)
        }
    }
}

@Composable
fun OilStatusChip(oil: OilStatus) {
    val text = when (oil) {
        is OilStatus.Ok -> "Óptimo"
        is OilStatus.DueSoon -> "Revisión pronto"
        is OilStatus.Overdue -> "Servicio vencido"
        OilStatus.NoRecord -> "Sin datos"
    }
    StatusChip(text, oil.color)
}
