package com.motofix.app.data

/** Definición de todas las misiones. Más adelante las enviará el servidor (rol administrador). */
object MissionCatalog {

    private val dailyLogin = MissionDef(
        id = "d_login",
        title = "Inicia sesión hoy",
        description = "Abre MotoFix para mantener tu racha.",
        action = MissionAction.OPEN_APP,
        target = 1,
        rewardXp = 10,
        rewardCoins = 10,
    )

    /** Se eligen 2 de estas cada día, rotando (nunca dos con la misma acción). */
    private val dailyPool = listOf(
        MissionDef(
            id = "d_history",
            title = "Revisa el historial de una moto",
            description = "Entra al historial de cualquier moto de tu garaje.",
            action = MissionAction.VIEW_HISTORY,
            target = 1,
            rewardXp = 30,
            rewardCoins = 20,
        ),
        MissionDef(
            id = "d_maintenance",
            title = "Registra un mantenimiento",
            description = "Anota un servicio que le hayas hecho a tu moto.",
            action = MissionAction.ADD_MAINTENANCE,
            target = 1,
            rewardXp = 50,
            rewardCoins = 30,
        ),
        MissionDef(
            id = "d_mileage",
            title = "Actualiza el kilometraje",
            description = "Pon al día los kilómetros de una de tus motos.",
            action = MissionAction.UPDATE_MILEAGE,
            target = 1,
            rewardXp = 40,
            rewardCoins = 20,
        ),
        MissionDef(
            id = "d_repair",
            title = "Completa una reparación en el taller",
            description = "Termina cualquier nivel del taller virtual.",
            action = MissionAction.COMPLETE_REPAIR,
            target = 1,
            rewardXp = 50,
            rewardCoins = 40,
        ),
        MissionDef(
            id = "d_audio",
            title = "Completa 1 diagnóstico de Audio IA",
            description = "Graba el sonido del motor de tu moto.",
            action = MissionAction.AUDIO_DIAGNOSIS,
            target = 1,
            rewardXp = 50,
            rewardCoins = 30,
        ),
    )

    val weeklyObjectives = listOf(
        MissionDef(
            id = "w_maintenance",
            title = "Registra 3 mantenimientos",
            description = "Lleva el control de los servicios de tus motos.",
            action = MissionAction.ADD_MAINTENANCE,
            target = 3,
            rewardXp = 0,
        ),
        MissionDef(
            id = "w_history",
            title = "Revisa historiales 5 veces",
            description = "Consulta el historial técnico de tus motos.",
            action = MissionAction.VIEW_HISTORY,
            target = 5,
            rewardXp = 0,
        ),
        MissionDef(
            id = "w_mileage",
            title = "Actualiza el kilometraje 2 veces",
            description = "Un kilometraje real permite avisarte del próximo cambio de aceite.",
            action = MissionAction.UPDATE_MILEAGE,
            target = 2,
            rewardXp = 0,
        ),
    )

    const val WEEKLY_TITLE = "Mantenimiento al día"
    const val WEEKLY_SUBTITLE = "Control preventivo del garaje"
    const val WEEKLY_DESCRIPTION =
        "Una moto bien mantenida dura más y es más segura. Completa todos los objetivos " +
            "antes de que termine la semana para ganar la insignia."
    const val WEEKLY_REWARD_XP = 450
    const val WEEKLY_REWARD_COINS = 500
    const val WEEKLY_BADGE = "MANTENIMIENTO_PRO"

    const val STREAK_BLOCK_DAYS = 7
    const val STREAK_REWARD_COINS = 500
    const val STREAK_REWARD_XP = 100

    fun dailyFor(epochDay: Long): List<MissionDef> {
        val start = Math.floorMod(epochDay, dailyPool.size.toLong()).toInt()
        val picked = List(2) { dailyPool[(start + it) % dailyPool.size] }
        return listOf(dailyLogin) + picked
    }

    private val allDaily = listOf(dailyLogin) + dailyPool

    fun findDaily(id: String): MissionDef? = allDaily.firstOrNull { it.id == id }
    fun findWeekly(id: String): MissionDef? = weeklyObjectives.firstOrNull { it.id == id }
}
