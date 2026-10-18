package com.motofix.app.ui.screens.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.data.OilStatus
import com.motofix.app.ui.components.EmptyState
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.StatusChip
import com.motofix.app.ui.components.SystemFooter
import com.motofix.app.ui.components.rememberAppVersion
import com.motofix.app.ui.components.formatKm
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary

@Composable
fun GarageScreen(
    onAddMotorcycle: () -> Unit,
    onOpenMotorcycle: (String) -> Unit,
    onReviewAi: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: GarageViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = "Mi garaje personal",
            subtitle = "MotoFix systems",
            onBack = null,
            action = {
                if (state.items.isNotEmpty()) {
                    IconButton(
                        onClick = onAddMotorcycle,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Orange, contentColor = Color.Black),
                    ) { Icon(Icons.Outlined.Add, contentDescription = "Añadir moto") }
                }
            },
        )

        if (!state.loaded) return@Column

        if (state.items.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.TwoWheeler,
                title = "Tu garaje está",
                highlight = "desierto",
                message = "No tienes vehículos registrados. Añade tu primera máquina para empezar el seguimiento de su mantenimiento.",
                actionText = "+ Añadir vehículo",
                onAction = onAddMotorcycle,
                footer = "GARAGE_NODE: OFFLINE",
            )
            return@Column
        }

        Spacer(Modifier.height(8.dp))
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Estado del garaje")
                    Text(
                        "VEHÍCULOS EN PROPIEDAD: ${state.items.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                }
                StatusChip("Sincro OK", Green)
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Tus motocicletas registradas")
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.items.forEach { item ->
                MotorcycleCard(
                    item = item,
                    onOpen = { onOpenMotorcycle(item.moto.id) },
                    onReviewAi = onReviewAi,
                )
            }
        }

        state.nextService?.let { next ->
            Spacer(Modifier.height(20.dp))
            MotoFixCard(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenMotorcycle(next.moto.id) },
                borderColor = next.oil.color.copy(alpha = 0.5f), contentPadding = 16.dp) {
                Row {
                    Icon(Icons.AutoMirrored.Outlined.EventNote, contentDescription = null, tint = next.oil.color)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("PRÓXIMA REVISIÓN TÉCNICA", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        val detail = when (val oil = next.oil) {
                            is OilStatus.Overdue -> "tiene el cambio de aceite vencido por ${formatKm(oil.kmOver)}"
                            is OilStatus.DueSoon -> "necesita cambio de aceite en ${formatKm(oil.kmRemaining)}"
                            else -> ""
                        }
                        Text(
                            "Tu ${next.moto.displayName} $detail. Agenda el servicio para mantener su historial al día.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "VER HISTORIAL →",
                            style = MaterialTheme.typography.labelMedium,
                            color = Orange,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = onOpenHistory,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, Border),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("HISTORIAL DE OPERACIONES", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Text(
                        "${state.totalRecords} servicios registrados en total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
            }
        }

        SystemFooter(left = "GARAGE_NODE: ONLINE", right = "MOTOFIX OS V${rememberAppVersion()}", modifier = Modifier.padding(vertical = 20.dp))
    }
}
