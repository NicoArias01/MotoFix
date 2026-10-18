package com.motofix.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.motofix.app.data.RewardResult
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.ErrorRed
import com.motofix.app.ui.theme.FieldBackground
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SpanishLocale: Locale = Locale.forLanguageTag("es-CO")
private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", SpanishLocale)

/** 18450 -> "18.450" (separador de miles colombiano). */
fun formatThousands(value: Int): String = String.format(Locale.US, "%,d", value).replace(',', '.')
fun formatKm(value: Int): String = "${formatThousands(value)} km"
fun formatCop(value: Int): String = "$${formatThousands(value)}"
fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DateFormat).replace(".", "")

/** Encabezado de pantalla interna: flecha atrás, título grande y acción opcional a la derecha. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = Orange,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            if (subtitle != null) {
                Text(subtitle.uppercase(), style = MaterialTheme.typography.labelSmall, color = subtitleColor)
            }
        }
        action?.invoke()
    }
}

/** Fila deslizable de opciones tipo "chip" (tipo de moto, tipo de mantenimiento...). */
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    icon: ((T) -> ImageVector)? = null,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) Orange else SurfaceRaised,
                border = BorderStroke(1.dp, if (isSelected) Orange else Border),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) {
                        Icon(
                            icon(option),
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        label(option),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) Color.Black else TextPrimary,
                    )
                }
            }
        }
    }
}

/** Campo de solo lectura que abre un selector (por ejemplo, un calendario). */
@Composable
fun PickerField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionLabel(label)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FieldBackground)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary)
            Spacer(Modifier.width(14.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        }
    }
}

/** Etiqueta pequeña con borde ("SINCRO OK", "PENDIENTE", "ESPECIAL"...). */
@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = if (filled) color else color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (filled) Color.Black else color,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text(title.uppercase(), color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, color = TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText.uppercase(), color = if (destructive) ErrorRed else Orange, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

/** Pantalla vacía con icono grande, título de dos colores y una acción principal. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    highlight: String,
    message: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    footer: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.dp, Cyan.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
        ) {
            Icon(icon, contentDescription = null, tint = Cyan.copy(alpha = 0.6f), modifier = Modifier.size(110.dp))
            StatusChip(
                "Estatus: vacío",
                Orange,
                filled = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(title.uppercase(), style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
        Text(highlight.uppercase(), style = MaterialTheme.typography.headlineMedium, color = Orange, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
        if (footer != null) {
            Spacer(Modifier.height(12.dp))
            Text(footer, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = actionText, onClick = onAction)
    }
}

/** Ventana de "¡Recompensa obtenida!" al reclamar misiones (inspirada en "Nuevo logro detectado"). */
@Composable
fun RewardDialog(reward: RewardResult, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, Orange.copy(alpha = 0.6f)),
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Orange, modifier = Modifier.size(16.dp))
                    Text(
                        if (reward.newLevel != null) " ¡SUBISTE DE NIVEL! " else " RECOMPENSA OBTENIDA ",
                        style = MaterialTheme.typography.labelMedium,
                        color = Orange,
                    )
                    Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Orange, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(20.dp))
                Icon(
                    Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = Cyan,
                    modifier = Modifier.size(96.dp),
                )
                Spacer(Modifier.height(16.dp))
                if (reward.newLevel != null) {
                    Text("NIVEL ${reward.newLevel}", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    reward.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (reward.xp > 0) RewardTile(Icons.Outlined.Bolt, "+${reward.xp} XP", Green, Modifier.weight(1f))
                    if (reward.coins > 0) RewardTile(Icons.Outlined.MonetizationOn, "+${formatNumber(reward.coins)}", Orange, Modifier.weight(1f))
                }
                if (reward.badge != null) {
                    Spacer(Modifier.height(10.dp))
                    RewardTile(Icons.Outlined.MilitaryTech, "Insignia ${reward.badge}", Cyan, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(24.dp))
                PrimaryButton(text = "Continuar", onClick = onDismiss)
            }
        }
    }
}

@Composable
fun RewardTile(icon: ImageVector, text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = SurfaceRaised,
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}
