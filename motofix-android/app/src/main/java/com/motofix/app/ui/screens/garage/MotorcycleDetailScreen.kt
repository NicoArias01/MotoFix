package com.motofix.app.ui.screens.garage

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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.data.MaintenanceRecord
import com.motofix.app.ui.components.ConfirmDialog
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.MotoFixTextField
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.ProgressBar
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SecondaryButton
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.formatCop
import com.motofix.app.ui.components.formatThousands
import com.motofix.app.ui.components.icon
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary
import com.motofix.app.ui.theme.Orange

@Composable
fun MotorcycleDetailScreen(
    motoId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddMaintenance: () -> Unit,
    onReviewAi: () -> Unit,
    viewModel: MotorcycleDetailViewModel = viewModel(key = "moto-detail-$motoId") { MotorcycleDetailViewModel(motoId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showMileageDialog by rememberSaveable { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<MaintenanceRecord?>(null) }

    // Si la moto se eliminó (por ejemplo desde Editar), se vuelve al garaje
    LaunchedEffect(state.loaded, state.moto) {
        if (state.loaded && state.moto == null) onBack()
    }
    val moto = state.moto ?: return

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = moto.displayName,
            subtitle = "Historial técnico",
            onBack = onBack,
            action = {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Editar moto", tint = TextPrimary)
                }
            },
        )

        // Tarjeta principal con la moto
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(listOf(SurfaceRaised, SurfaceDark))),
            ) {
                Icon(moto.type.icon, contentDescription = null, tint = Cyan.copy(alpha = 0.7f), modifier = Modifier.size(96.dp))
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) { OilStatusChip(state.oil) }
                if (moto.plate.isNotBlank()) {
                    Text(
                        moto.plate,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Kilometraje", "${formatThousands(moto.mileageKm)} km", Modifier.weight(1f))
                StatBox("Cilindraje", "${moto.displacementCc} cc", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Tipo · Año", "${moto.type.label} · ${moto.year}", Modifier.weight(1f))
                StatBox("Servicios", "${state.records.size}", Modifier.weight(1f))
            }
        }

        // Estado del aceite
        Spacer(Modifier.height(12.dp))
        MotoFixCard(Modifier.fillMaxWidth(), borderColor = state.oil.color.copy(alpha = 0.5f), contentPadding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = state.oil.color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                SectionLabel("Cambio de aceite", color = state.oil.color)
            }
            Spacer(Modifier.height(8.dp))
            Text(state.oil.longLabel, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            ProgressBar(state.oil.usage, color = state.oil.color)
            Spacer(Modifier.height(4.dp))
            Text("Intervalo recomendado: cada 3.000 km", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = "Actualizar km",
                onClick = { showMileageDialog = true },
                modifier = Modifier.weight(1f),
                uppercase = true,
                leading = { Icon(Icons.Outlined.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            SecondaryButton(
                text = "Revisar IA",
                onClick = onReviewAi,
                modifier = Modifier.weight(1f),
                contentColor = Cyan,
                borderColor = Cyan.copy(alpha = 0.5f),
                uppercase = true,
                leading = { Icon(Icons.Outlined.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }

        // Historial
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("HISTORIAL DE MANTENIMIENTO", style = MaterialTheme.typography.labelLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            if (state.totalSpent > 0) {
                Text("Total ${formatCop(state.totalSpent)}", style = MaterialTheme.typography.labelSmall, color = Orange)
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(text = "+ Registrar mantenimiento", onClick = onAddMaintenance, height = 48.dp)
        Spacer(Modifier.height(12.dp))
        if (state.records.isEmpty()) {
            Text(
                "Aún no hay servicios registrados para esta moto.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.records.forEach { record ->
                    MaintenanceRecordRow(record, onDelete = { recordToDelete = record })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showMileageDialog) {
        MileageDialog(
            current = moto.mileageKm,
            onSave = viewModel::updateMileage,
            onDismiss = { showMileageDialog = false },
        )
    }

    recordToDelete?.let { record ->
        ConfirmDialog(
            title = "Eliminar registro",
            message = "¿Borrar \"${record.type.label}\" del historial?",
            confirmText = "Eliminar",
            destructive = true,
            onConfirm = {
                viewModel.deleteRecord(record.id)
                recordToDelete = null
            },
            onDismiss = { recordToDelete = null },
        )
    }
}

@Composable
private fun MileageDialog(
    current: Int,
    onSave: (String) -> String?,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(current.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val submit = {
        error = onSave(text)
        if (error == null) onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("ACTUALIZAR KILOMETRAJE", color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text("Actual: ${formatThousands(current)} km", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                MotoFixTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(6); error = null },
                    label = "Nuevo kilometraje",
                    placeholder = "km",
                    leadingIcon = Icons.Outlined.Speed,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    onImeAction = submit,
                    errorText = error,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = submit) { Text("GUARDAR", color = Orange, style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = TextSecondary, style = MaterialTheme.typography.labelLarge) }
        },
    )
}
