package com.motofix.app.ui.screens.garage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motofix.app.data.AppContainer
import com.motofix.app.data.BikeType
import com.motofix.app.data.GarageRepository
import com.motofix.app.data.MaintenanceRecord
import com.motofix.app.data.MaintenanceType
import com.motofix.app.data.Motorcycle
import com.motofix.app.data.OilStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class GarageItem(
    val moto: Motorcycle,
    val oil: OilStatus,
    val records: Int,
)

data class GarageUiState(
    val items: List<GarageItem> = emptyList(),
    val totalRecords: Int = 0,
    val loaded: Boolean = false,
) {
    /** La moto que más urgentemente necesita servicio (aceite vencido o por vencer). */
    val nextService: GarageItem?
        get() = items
            .filter { it.oil is OilStatus.Overdue || it.oil is OilStatus.DueSoon }
            .minByOrNull {
                when (val oil = it.oil) {
                    is OilStatus.Overdue -> -oil.kmOver
                    is OilStatus.DueSoon -> oil.kmRemaining
                    else -> Int.MAX_VALUE
                }
            }
}

private fun buildItems(motos: List<Motorcycle>, records: List<MaintenanceRecord>) = motos.map { moto ->
    GarageItem(moto, OilStatus.of(moto, records), records.count { it.motorcycleId == moto.id })
}

// ---------- Lista del garaje ----------

class GarageViewModel(
    garage: GarageRepository = AppContainer.garage,
) : ViewModel() {
    val state: StateFlow<GarageUiState> = combine(garage.motorcycles, garage.maintenance) { motos, records ->
        GarageUiState(buildItems(motos, records), records.size, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GarageUiState())
}

// ---------- Detalle / historial de una moto ----------

data class MotorcycleDetailState(
    val moto: Motorcycle? = null,
    val records: List<MaintenanceRecord> = emptyList(),
    val oil: OilStatus = OilStatus.NoRecord,
    val loaded: Boolean = false,
) {
    val totalSpent: Int get() = records.sumOf { it.costCop ?: 0 }
}

class MotorcycleDetailViewModel(
    private val motoId: String,
    private val garage: GarageRepository = AppContainer.garage,
) : ViewModel() {

    val state: StateFlow<MotorcycleDetailState> = combine(garage.motorcycles, garage.maintenance) { motos, records ->
        val moto = motos.firstOrNull { it.id == motoId }
        val own = records.filter { it.motorcycleId == motoId }
            .sortedWith(compareByDescending<MaintenanceRecord> { it.dateEpochDay }.thenByDescending { it.mileageKm })
        MotorcycleDetailState(
            moto = moto,
            records = own,
            oil = if (moto != null) OilStatus.of(moto, records) else OilStatus.NoRecord,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MotorcycleDetailState())

    init {
        // El ViewModel vive mientras la pantalla está en la pila: cuenta una visita por apertura
        garage.onHistoryViewed()
    }

    /** Devuelve un mensaje de error o null si se guardó. */
    fun updateMileage(text: String): String? {
        val km = text.trim().toIntOrNull() ?: return "Escribe un número válido"
        val current = garage.motorcycle(motoId)?.mileageKm ?: return "La moto ya no existe"
        if (km < 0 || km > 999_999) return "Kilometraje fuera de rango"
        if (km < current) return "Debe ser mayor o igual a ${current} km"
        garage.updateMileage(motoId, km)
        return null
    }

    fun deleteRecord(id: String) = garage.deleteMaintenance(id)
}

// ---------- Formulario de moto (agregar / editar) ----------

data class MotorcycleFormState(
    val isEdit: Boolean = false,
    val brand: String = "",
    val model: String = "",
    val year: String = "",
    val displacement: String = "",
    val mileage: String = "",
    val plate: String = "",
    val type: BikeType? = null,
    val brandError: String? = null,
    val modelError: String? = null,
    val yearError: String? = null,
    val displacementError: String? = null,
    val mileageError: String? = null,
    val typeError: String? = null,
)

class MotorcycleFormViewModel(
    private val motoId: String?,
    private val garage: GarageRepository = AppContainer.garage,
) : ViewModel() {

    var state by mutableStateOf(
        motoId?.let { garage.motorcycle(it) }?.let {
            MotorcycleFormState(
                isEdit = true,
                brand = it.brand,
                model = it.model,
                year = it.year.toString(),
                displacement = it.displacementCc.toString(),
                mileage = it.mileageKm.toString(),
                plate = it.plate,
                type = it.type,
            )
        } ?: MotorcycleFormState()
    )
        private set

    private fun digits(value: String, max: Int) = value.filter { it.isDigit() }.take(max)

    fun onBrand(v: String) { state = state.copy(brand = v.take(30), brandError = null) }
    fun onModel(v: String) { state = state.copy(model = v.take(40), modelError = null) }
    fun onYear(v: String) { state = state.copy(year = digits(v, 4), yearError = null) }
    fun onDisplacement(v: String) { state = state.copy(displacement = digits(v, 4), displacementError = null) }
    fun onMileage(v: String) { state = state.copy(mileage = digits(v, 6), mileageError = null) }
    fun onPlate(v: String) { state = state.copy(plate = v.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(8)) }
    fun onType(t: BikeType) { state = state.copy(type = t, typeError = null) }

    /** Devuelve el id de la moto guardada, o null si hay errores. */
    fun save(): String? {
        val s = state
        val maxYear = LocalDate.now().year + 1
        val year = s.year.toIntOrNull()
        val cc = s.displacement.toIntOrNull()
        val km = s.mileage.toIntOrNull()
        val errors = s.copy(
            brandError = if (s.brand.isBlank()) "Escribe la marca" else null,
            modelError = if (s.model.isBlank()) "Escribe el modelo" else null,
            yearError = if (year == null || year !in 1950..maxYear) "Año entre 1950 y $maxYear" else null,
            displacementError = if (cc == null || cc !in 49..2500) "Cilindraje entre 49 y 2500 cc" else null,
            mileageError = if (km == null) "Escribe el kilometraje" else null,
            typeError = if (s.type == null) "Elige el tipo de moto" else null,
        )
        state = errors
        if (listOf(errors.brandError, errors.modelError, errors.yearError, errors.displacementError, errors.mileageError, errors.typeError).any { it != null }) {
            return null
        }
        val moto = Motorcycle(
            id = motoId ?: "",
            brand = s.brand.trim(),
            model = s.model.trim(),
            year = year!!,
            displacementCc = cc!!,
            type = s.type!!,
            mileageKm = km!!,
            plate = s.plate.trim(),
        )
        return if (motoId != null) {
            garage.updateMotorcycle(moto)
            motoId
        } else {
            garage.addMotorcycle(moto)
        }
    }

    fun delete() {
        if (motoId != null) garage.deleteMotorcycle(motoId)
    }
}

// ---------- Formulario de mantenimiento ----------

data class MaintenanceFormState(
    val motoName: String = "",
    val currentMileage: Int = 0,
    val type: MaintenanceType? = null,
    val dateEpochDay: Long = LocalDate.now().toEpochDay(),
    val mileage: String = "",
    val cost: String = "",
    val workshop: String = "",
    val notes: String = "",
    val typeError: String? = null,
    val mileageError: String? = null,
    val dateError: String? = null,
)

class MaintenanceFormViewModel(
    private val motoId: String,
    private val garage: GarageRepository = AppContainer.garage,
) : ViewModel() {

    var state by mutableStateOf(
        garage.motorcycle(motoId).let { moto ->
            MaintenanceFormState(
                motoName = moto?.displayName.orEmpty(),
                currentMileage = moto?.mileageKm ?: 0,
                mileage = moto?.mileageKm?.toString().orEmpty(),
            )
        }
    )
        private set

    fun onType(t: MaintenanceType) { state = state.copy(type = t, typeError = null) }
    fun onDate(epochDay: Long) { state = state.copy(dateEpochDay = epochDay, dateError = null) }
    fun onMileage(v: String) { state = state.copy(mileage = v.filter { it.isDigit() }.take(6), mileageError = null) }
    fun onCost(v: String) { state = state.copy(cost = v.filter { it.isDigit() }.take(9)) }
    fun onWorkshop(v: String) { state = state.copy(workshop = v.take(40)) }
    fun onNotes(v: String) { state = state.copy(notes = v.take(300)) }

    fun save(): Boolean {
        val s = state
        val km = s.mileage.toIntOrNull()
        val checked = s.copy(
            typeError = if (s.type == null) "Elige qué servicio se hizo" else null,
            mileageError = if (km == null) "Escribe el kilometraje del servicio" else null,
            dateError = if (s.dateEpochDay > LocalDate.now().toEpochDay()) "La fecha no puede ser futura" else null,
        )
        state = checked
        if (checked.typeError != null || checked.mileageError != null || checked.dateError != null) return false
        garage.addMaintenance(
            MaintenanceRecord(
                id = "",
                motorcycleId = motoId,
                type = s.type!!,
                dateEpochDay = s.dateEpochDay,
                mileageKm = km!!,
                costCop = s.cost.toIntOrNull(),
                workshop = s.workshop.trim(),
                notes = s.notes.trim(),
            )
        )
        return true
    }
}

// ---------- Historial de todo el garaje ----------

data class HistoryEntry(val record: MaintenanceRecord, val motoName: String)

class GarageHistoryViewModel(
    garage: GarageRepository = AppContainer.garage,
) : ViewModel() {
    val entries: StateFlow<List<HistoryEntry>> = combine(garage.motorcycles, garage.maintenance) { motos, records ->
        val names = motos.associate { it.id to it.displayName }
        records
            .sortedWith(compareByDescending<MaintenanceRecord> { it.dateEpochDay }.thenByDescending { it.mileageKm })
            .map { HistoryEntry(it, names[it.motorcycleId] ?: "Moto eliminada") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
