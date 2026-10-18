package com.motofix.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Módulo "garaje" (RF04): motos del usuario y su historial de mantenimiento. */
class GarageRepository(
    private val store: AppStore,
    private val missions: MissionsRepository,
) {

    val motorcycles: Flow<List<Motorcycle>> = store.data.map { it.motorcycles }.distinctUntilChanged()

    val maintenance: Flow<List<MaintenanceRecord>> = store.data.map { it.maintenance }.distinctUntilChanged()

    fun motorcycle(id: String): Motorcycle? = store.current.motorcycles.firstOrNull { it.id == id }

    fun addMotorcycle(moto: Motorcycle): String {
        val id = UUID.randomUUID().toString()
        store.update { data ->
            missions.applyAction(data.copy(motorcycles = data.motorcycles + moto.copy(id = id)), MissionAction.ADD_MOTORCYCLE)
        }
        return id
    }

    fun updateMotorcycle(moto: Motorcycle) {
        store.update { data ->
            val old = data.motorcycles.firstOrNull { it.id == moto.id } ?: return@update data
            val updated = data.copy(motorcycles = data.motorcycles.map { if (it.id == moto.id) moto else it })
            if (moto.mileageKm > old.mileageKm) missions.applyAction(updated, MissionAction.UPDATE_MILEAGE) else updated
        }
    }

    fun deleteMotorcycle(id: String) {
        store.update { data ->
            data.copy(
                motorcycles = data.motorcycles.filterNot { it.id == id },
                maintenance = data.maintenance.filterNot { it.motorcycleId == id },
            )
        }
    }

    fun updateMileage(id: String, mileageKm: Int) {
        val moto = motorcycle(id) ?: return
        updateMotorcycle(moto.copy(mileageKm = mileageKm))
    }

    /** Guarda un mantenimiento; si se hizo con más km de los registrados, actualiza la moto. */
    fun addMaintenance(record: MaintenanceRecord) {
        store.update { data ->
            val withRecord = data.copy(
                maintenance = data.maintenance + record.copy(id = UUID.randomUUID().toString()),
                motorcycles = data.motorcycles.map {
                    if (it.id == record.motorcycleId && record.mileageKm > it.mileageKm) it.copy(mileageKm = record.mileageKm) else it
                },
            )
            missions.applyAction(withRecord, MissionAction.ADD_MAINTENANCE)
        }
    }

    fun deleteMaintenance(id: String) {
        store.update { data -> data.copy(maintenance = data.maintenance.filterNot { it.id == id }) }
    }

    fun onHistoryViewed() {
        store.update { data -> missions.applyAction(data, MissionAction.VIEW_HISTORY) }
    }
}
