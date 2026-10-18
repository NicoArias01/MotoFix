package com.motofix.app.ui.screens.garage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SystemFooter
import com.motofix.app.ui.theme.TextSecondary

/** Todos los servicios de todas las motos, del más reciente al más antiguo. */
@Composable
fun GarageHistoryScreen(
    onBack: () -> Unit,
    onOpenMotorcycle: (String) -> Unit,
    viewModel: GarageHistoryViewModel = viewModel(),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ScreenHeader(
                title = "Historial de operaciones",
                subtitle = "${entries.size} servicios registrados",
                onBack = onBack,
            )
        }
        if (entries.isEmpty()) {
            item {
                Text(
                    "Todavía no hay servicios. Entra a una moto y pulsa \"Registrar mantenimiento\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
        items(entries, key = { it.record.id }) { entry ->
            MaintenanceRecordRow(
                record = entry.record,
                motoName = entry.motoName,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenMotorcycle(entry.record.motorcycleId) },
            )
        }
        item {
            SystemFooter(left = "END_OF_RECORDS", right = "LOG_NODE: GARAGE", modifier = Modifier.padding(vertical = 20.dp))
        }
    }
}
