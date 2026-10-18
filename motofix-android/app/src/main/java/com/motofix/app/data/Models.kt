package com.motofix.app.data

import kotlinx.serialization.Serializable

const val XP_PER_LEVEL = 1000

@Serializable
data class UserProfile(
    val name: String,
    val email: String,
    val level: Int = 1,
    val coins: Int = 0,
    val xp: Int = 0,
    val badges: List<String> = emptyList(),
    val unreadNotifications: Int = 0,
    val createdAtMillis: Long = 0,
    /** Cómo se creó la cuenta: PASSWORD, GOOGLE o GITHUB. */
    val provider: String = "PASSWORD",
) {
    val xpForNextLevel: Int get() = XP_PER_LEVEL

    val rank: String
        get() = when {
            level <= 2 -> "Aprendiz"
            level == 3 -> "Mecánico Junior"
            level <= 5 -> "Mecánico Senior"
            else -> "Maestro Mecánico"
        }

    val initials: String
        get() = name.filter { it.isLetter() }.take(2).uppercase().ifEmpty { "MF" }
}

@Serializable
enum class BikeType(val label: String) {
    SCOOTER("Scooter"),
    NAKED("Naked"),
    SPORTS("Sports"),
    TRAIL("Trail"),
    CLASSIC("Clásica");

    companion object {
        /** Las tres opciones que muestra el registro, como en el prototipo. */
        val registrationOptions = listOf(SCOOTER, NAKED, SPORTS)
    }
}

@Serializable
data class Motorcycle(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    val displacementCc: Int,
    val type: BikeType,
    val mileageKm: Int,
    val plate: String = "",
) {
    val displayName: String get() = "$brand $model"
}

@Serializable
enum class MaintenanceType(val label: String) {
    OIL_CHANGE("Cambio de aceite"),
    BRAKES("Frenos"),
    CHAIN("Cadena y piñones"),
    TIRES("Llantas"),
    ELECTRICAL("Sistema eléctrico"),
    GENERAL("Revisión general"),
    OTHER("Otro"),
}

@Serializable
data class MaintenanceRecord(
    val id: String,
    val motorcycleId: String,
    val type: MaintenanceType,
    /** Fecha como `LocalDate.toEpochDay()`. */
    val dateEpochDay: Long,
    val mileageKm: Int,
    val costCop: Int? = null,
    val workshop: String = "",
    val notes: String = "",
)

/** Estado del aceite calculado a partir del último cambio registrado. */
sealed interface OilStatus {
    data object NoRecord : OilStatus
    data class Ok(val kmRemaining: Int) : OilStatus
    data class DueSoon(val kmRemaining: Int) : OilStatus
    data class Overdue(val kmOver: Int) : OilStatus

    companion object {
        const val OIL_INTERVAL_KM = 3000
        private const val WARNING_KM = 500

        fun of(moto: Motorcycle, records: List<MaintenanceRecord>): OilStatus {
            val lastOil = records
                .filter { it.motorcycleId == moto.id && it.type == MaintenanceType.OIL_CHANGE }
                .maxByOrNull { it.mileageKm }
                ?: return NoRecord
            val remaining = lastOil.mileageKm + OIL_INTERVAL_KM - moto.mileageKm
            return when {
                remaining < 0 -> Overdue(-remaining)
                remaining <= WARNING_KM -> DueSoon(remaining)
                else -> Ok(remaining)
            }
        }
    }
}

// ---------- Misiones ----------

/** Acciones reales de la app que hacen avanzar las misiones. */
@Serializable
enum class MissionAction {
    OPEN_APP,
    ADD_MOTORCYCLE,
    VIEW_HISTORY,
    ADD_MAINTENANCE,
    UPDATE_MILEAGE,
    COMPLETE_REPAIR,
    AUDIO_DIAGNOSIS,
}

data class MissionDef(
    val id: String,
    val title: String,
    val description: String,
    val action: MissionAction,
    val target: Int,
    val rewardXp: Int,
    val rewardCoins: Int = 0,
)

@Serializable
data class MissionProgress(
    val id: String,
    val progress: Int = 0,
    val claimed: Boolean = false,
)

/** Misión lista para mostrar: definición + progreso del usuario. */
data class Mission(
    val def: MissionDef,
    val progress: Int,
    val claimed: Boolean,
) {
    val title: String get() = def.title
    val total: Int get() = def.target
    val isCompleted: Boolean get() = progress >= def.target
    val canClaim: Boolean get() = isCompleted && !claimed
}

data class WeeklyChallenge(
    val code: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val rewardXp: Int,
    val rewardCoins: Int,
    val badge: String,
    val objectives: List<Mission>,
    val claimed: Boolean,
    val daysLeft: Int,
) {
    val completedObjectives: Int get() = objectives.count { it.isCompleted }
    val progress: Float
        get() = if (objectives.isEmpty()) 0f
        else objectives.sumOf { minOf(it.progress, it.total).toDouble() / it.total }.toFloat() / objectives.size
    val isCompleted: Boolean get() = objectives.all { it.isCompleted }
    val canClaim: Boolean get() = isCompleted && !claimed
}

@Serializable
data class MissionsState(
    val dailyEpochDay: Long = -1,
    val daily: List<MissionProgress> = emptyList(),
    val weekStartEpochDay: Long = -1,
    val weekly: List<MissionProgress> = emptyList(),
    val weeklyClaimed: Boolean = false,
    val checkInDays: List<Long> = emptyList(),
    val lastCheckInEpochDay: Long = -1,
    val streak: Int = 0,
    /** Bloques de 7 días de racha cuya recompensa ya se cobró. */
    val streakBlocksClaimed: Int = 0,
)

/** Lo que recibe el usuario al reclamar una recompensa. */
data class RewardResult(
    val title: String,
    val xp: Int,
    val coins: Int,
    val badge: String? = null,
    val newLevel: Int? = null,
)

// ---------- Todo lo que se guarda en el teléfono ----------

@Serializable
data class AppData(
    val user: UserProfile? = null,
    val loggedIn: Boolean = false,
    val motorcycles: List<Motorcycle> = emptyList(),
    val maintenance: List<MaintenanceRecord> = emptyList(),
    val missions: MissionsState = MissionsState(),
    /** Progreso por nivel del taller, con el id del escenario como llave. */
    val workshop: Map<String, LevelProgress> = emptyMap(),
    val certificates: List<Certificate> = emptyList(),
    val diagnoses: List<AudioDiagnosisRecord> = emptyList(),
    val settings: AppSettings = AppSettings(),
)

/** Preferencias del usuario (pantalla de ajustes). */
@Serializable
data class AppSettings(
    val haptics: Boolean = true,
    val soundEffects: Boolean = true,
    val notifications: Boolean = true,
)
