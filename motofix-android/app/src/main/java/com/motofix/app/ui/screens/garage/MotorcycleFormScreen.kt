package com.motofix.app.ui.screens.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.motofix.app.data.BikeType
import com.motofix.app.ui.components.ChoiceChips
import com.motofix.app.ui.components.ConfirmDialog
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.MotoFixTextField
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SecondaryButton
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.icon
import com.motofix.app.ui.theme.ErrorRed

/** Agregar (motoId = null) o editar una moto del garaje. */
@Composable
fun MotorcycleFormScreen(
    motoId: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: MotorcycleFormViewModel = viewModel(key = "moto-form-$motoId") { MotorcycleFormViewModel(motoId) },
) {
    val state = viewModel.state
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = if (state.isEdit) "Editar vehículo" else "Nuevo vehículo",
            subtitle = if (state.isEdit) "Actualiza los datos de tu máquina" else "Registra tu máquina en el garaje",
            onBack = onBack,
        )

        MotoFixCard(Modifier.fillMaxWidth()) {
            SectionLabel("Tipo de moto")
            Spacer(Modifier.height(8.dp))
            ChoiceChips(
                options = BikeType.entries,
                selected = state.type,
                onSelect = viewModel::onType,
                label = { it.label },
                icon = { it.icon },
            )
            state.typeError?.let {
                Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
            MotoFixTextField(
                value = state.brand,
                onValueChange = viewModel::onBrand,
                label = "Marca",
                placeholder = "Yamaha, Honda, AKT...",
                leadingIcon = Icons.Outlined.Factory,
                errorText = state.brandError,
            )
            Spacer(Modifier.height(16.dp))
            MotoFixTextField(
                value = state.model,
                onValueChange = viewModel::onModel,
                label = "Modelo",
                placeholder = "MT-03, CB 190R...",
                leadingIcon = Icons.Outlined.TwoWheeler,
                errorText = state.modelError,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MotoFixTextField(
                    value = state.year,
                    onValueChange = viewModel::onYear,
                    label = "Año",
                    placeholder = "2022",
                    leadingIcon = Icons.Outlined.CalendarMonth,
                    keyboardType = KeyboardType.Number,
                    errorText = state.yearError,
                    modifier = Modifier.weight(1f),
                )
                MotoFixTextField(
                    value = state.displacement,
                    onValueChange = viewModel::onDisplacement,
                    label = "Cilindraje (cc)",
                    placeholder = "150",
                    leadingIcon = Icons.Outlined.LocalGasStation,
                    keyboardType = KeyboardType.Number,
                    errorText = state.displacementError,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(16.dp))
            MotoFixTextField(
                value = state.mileage,
                onValueChange = viewModel::onMileage,
                label = "Kilometraje actual",
                placeholder = "12500",
                leadingIcon = Icons.Outlined.Speed,
                keyboardType = KeyboardType.Number,
                errorText = state.mileageError,
            )
            Spacer(Modifier.height(16.dp))
            MotoFixTextField(
                value = state.plate,
                onValueChange = viewModel::onPlate,
                label = "Placa (opcional)",
                placeholder = "ABC-12D",
                leadingIcon = Icons.Outlined.ConfirmationNumber,
                imeAction = ImeAction.Done,
            )
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = if (state.isEdit) "Guardar cambios" else "Añadir al garaje",
            onClick = { viewModel.save()?.let(onSaved) },
        )
        if (state.isEdit) {
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = "Eliminar vehículo",
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
                contentColor = ErrorRed,
                borderColor = ErrorRed.copy(alpha = 0.5f),
                uppercase = true,
                leading = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Eliminar vehículo",
            message = "Se borrará la moto y todo su historial de mantenimiento. Esta acción no se puede deshacer.",
            confirmText = "Eliminar",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
                onDeleted()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
