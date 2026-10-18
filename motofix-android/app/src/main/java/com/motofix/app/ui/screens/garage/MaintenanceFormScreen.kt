package com.motofix.app.ui.screens.garage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.data.MaintenanceType
import com.motofix.app.ui.components.ChoiceChips
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.MotoFixTextField
import com.motofix.app.ui.components.PickerField
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.formatDate
import com.motofix.app.ui.components.formatThousands
import com.motofix.app.ui.components.icon
import com.motofix.app.ui.theme.ErrorRed
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextSecondary
import java.time.LocalDate

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceFormScreen(
    motoId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: MaintenanceFormViewModel = viewModel(key = "maintenance-$motoId") { MaintenanceFormViewModel(motoId) },
) {
    val state = viewModel.state
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = "Registrar servicio",
            subtitle = state.motoName,
            onBack = onBack,
        )

        MotoFixCard(Modifier.fillMaxWidth()) {
            SectionLabel("¿Qué se le hizo a la moto?")
            Spacer(Modifier.height(8.dp))
            ChoiceChips(
                options = MaintenanceType.entries,
                selected = state.type,
                onSelect = viewModel::onType,
                label = { it.label },
                icon = { it.icon },
            )
            state.typeError?.let {
                Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
            PickerField(
                label = "Fecha del servicio",
                value = formatDate(state.dateEpochDay),
                icon = Icons.Outlined.CalendarMonth,
                onClick = { showDatePicker = true },
            )
            state.dateError?.let {
                Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
            MotoFixTextField(
                value = state.mileage,
                onValueChange = viewModel::onMileage,
                label = "Kilometraje en el servicio",
                placeholder = formatThousands(state.currentMileage),
                leadingIcon = Icons.Outlined.Speed,
                keyboardType = KeyboardType.Number,
                errorText = state.mileageError,
            )
            Spacer(Modifier.height(16.dp))
            MotoFixTextField(
                value = state.cost,
                onValueChange = viewModel::onCost,
                label = "Costo en pesos (opcional)",
                placeholder = "120000",
                leadingIcon = Icons.Outlined.Payments,
                keyboardType = KeyboardType.Number,
            )
            Spacer(Modifier.height(16.dp))
            MotoFixTextField(
                value = state.workshop,
                onValueChange = viewModel::onWorkshop,
                label = "Taller (opcional)",
                placeholder = "Lo hice yo / Nombre del taller",
                leadingIcon = Icons.Outlined.Store,
            )
            Spacer(Modifier.height(16.dp))
            MotoFixTextField(
                value = state.notes,
                onValueChange = viewModel::onNotes,
                label = "Notas (opcional)",
                placeholder = "Repuestos, observaciones...",
                leadingIcon = Icons.AutoMirrored.Outlined.Notes,
                imeAction = ImeAction.Done,
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Si el kilometraje es mayor al actual de la moto, se actualizará automáticamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Guardar en el historial",
            onClick = { if (viewModel.save()) onSaved() },
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        val todayMillis = LocalDate.now().toEpochDay() * MILLIS_PER_DAY
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dateEpochDay * MILLIS_PER_DAY,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onDate(Math.floorDiv(it, MILLIS_PER_DAY)) }
                    showDatePicker = false
                }) { Text("ACEPTAR", color = Orange) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCELAR", color = TextSecondary) }
            },
            colors = DatePickerDefaults.colors(containerColor = SurfaceDark),
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = SurfaceDark,
                    selectedDayContainerColor = Orange,
                    todayDateBorderColor = Orange,
                    todayContentColor = Orange,
                ),
            )
        }
    }
}
