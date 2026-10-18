package com.motofix.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate

/** Todo lo que necesita la pantalla de misiones, ya calculado para hoy. */
data class MissionsOverview(
    val daily: List<Mission>,
    val weekly: WeeklyChallenge,
    val streak: Int,
    /** Días de la semana actual (lunes a domingo) con su estado. */
    val week: List<WeekDayStatus>,
    val daysToStreakReward: Int,
    val canClaimStreakReward: Boolean,
) {
    val pendingClaims: Int
        get() = daily.count { it.canClaim } + (if (weekly.canClaim) 1 else 0) + (if (canClaimStreakReward) 1 else 0)
}

data class WeekDayStatus(val letter: String, val checkedIn: Boolean, val isToday: Boolean, val isFuture: Boolean)

class MissionsRepository(
    private val store: AppStore,
    private val today: () -> LocalDate = { LocalDate.now() },
) {

    val overview: Flow<MissionsOverview> = store.data
        .map { buildOverview(it.missions, today()) }
        .distinctUntilChanged()

    fun currentOverview(): MissionsOverview = buildOverview(store.current.missions, today())

    /** Registra la entrada del día: mantiene la racha y cumple la misión de inicio de sesión. */
    fun checkIn() {
        store.update { data ->
            if (!data.loggedIn) return@update data
            val day = today().toEpochDay()
            var m = normalize(data.missions, today())
            if (m.lastCheckInEpochDay == day) return@update data.copy(missions = m)
            val continues = m.lastCheckInEpochDay == day - 1
            m = m.copy(
                streak = if (continues) m.streak + 1 else 1,
                streakBlocksClaimed = if (continues) m.streakBlocksClaimed else 0,
                lastCheckInEpochDay = day,
                checkInDays = (m.checkInDays + day).filter { it > day - 14 }.distinct(),
            )
            applyAction(data.copy(missions = m), MissionAction.OPEN_APP)
        }
    }

    /** Suma progreso a las misiones que dependen de [action]. Se usa dentro de otras actualizaciones. */
    fun applyAction(data: AppData, action: MissionAction): AppData {
        val m = normalize(data.missions, today())
        fun List<MissionProgress>.advance(find: (String) -> MissionDef?) = map { p ->
            val def = find(p.id)
            if (def != null && def.action == action && !p.claimed && p.progress < def.target) {
                p.copy(progress = p.progress + 1)
            } else p
        }
        return data.copy(
            missions = m.copy(
                daily = m.daily.advance(MissionCatalog::findDaily),
                weekly = if (m.weeklyClaimed) m.weekly else m.weekly.advance(MissionCatalog::findWeekly),
            )
        )
    }

    fun claimDaily(id: String): RewardResult? {
        var result: RewardResult? = null
        store.update { data ->
            val m = normalize(data.missions, today())
            val progress = m.daily.firstOrNull { it.id == id } ?: return@update data
            val def = MissionCatalog.findDaily(id) ?: return@update data
            if (progress.claimed || progress.progress < def.target) return@update data
            val user = data.user ?: return@update data
            val (newUser, newLevel) = user.withRewards(def.rewardXp, def.rewardCoins, null)
            result = RewardResult(def.title, def.rewardXp, def.rewardCoins, null, newLevel)
            data.copy(
                user = newUser,
                missions = m.copy(daily = m.daily.map { if (it.id == id) it.copy(claimed = true) else it }),
            )
        }
        return result
    }

    fun claimWeekly(): RewardResult? {
        var result: RewardResult? = null
        store.update { data ->
            val m = normalize(data.missions, today())
            val user = data.user ?: return@update data
            if (!buildWeekly(m, today()).canClaim) return@update data
            val badge = MissionCatalog.WEEKLY_BADGE
            val (newUser, newLevel) = user.withRewards(MissionCatalog.WEEKLY_REWARD_XP, MissionCatalog.WEEKLY_REWARD_COINS, badge)
            result = RewardResult(
                title = "Desafío semanal: ${MissionCatalog.WEEKLY_TITLE}",
                xp = MissionCatalog.WEEKLY_REWARD_XP,
                coins = MissionCatalog.WEEKLY_REWARD_COINS,
                badge = badge,
                newLevel = newLevel,
            )
            data.copy(user = newUser, missions = m.copy(weeklyClaimed = true))
        }
        return result
    }

    fun claimStreakReward(): RewardResult? {
        var result: RewardResult? = null
        store.update { data ->
            val m = data.missions
            val user = data.user ?: return@update data
            if (!canClaimStreak(m, today())) return@update data
            val (newUser, newLevel) = user.withRewards(MissionCatalog.STREAK_REWARD_XP, MissionCatalog.STREAK_REWARD_COINS, null)
            result = RewardResult(
                title = "Racha de ${effectiveStreak(m, today())} días",
                xp = MissionCatalog.STREAK_REWARD_XP,
                coins = MissionCatalog.STREAK_REWARD_COINS,
                newLevel = newLevel,
            )
            data.copy(user = newUser, missions = m.copy(streakBlocksClaimed = m.streakBlocksClaimed + 1))
        }
        return result
    }

    // ---------- Cálculos ----------

    /** Reinicia las misiones diarias y semanales cuando cambia el día o la semana. */
    private fun normalize(m: MissionsState, date: LocalDate): MissionsState {
        var s = m
        val day = date.toEpochDay()
        if (s.dailyEpochDay != day) {
            s = s.copy(dailyEpochDay = day, daily = MissionCatalog.dailyFor(day).map { MissionProgress(it.id) })
        }
        val weekStart = date.with(DayOfWeek.MONDAY).toEpochDay()
        if (s.weekStartEpochDay != weekStart) {
            s = s.copy(
                weekStartEpochDay = weekStart,
                weekly = MissionCatalog.weeklyObjectives.map { MissionProgress(it.id) },
                weeklyClaimed = false,
            )
        }
        return s
    }

    private fun effectiveStreak(m: MissionsState, date: LocalDate): Int =
        if (m.lastCheckInEpochDay >= date.toEpochDay() - 1) m.streak else 0

    private fun canClaimStreak(m: MissionsState, date: LocalDate): Boolean {
        val streak = effectiveStreak(m, date)
        val blocks = if (streak == 0) 0 else m.streakBlocksClaimed
        return streak / MissionCatalog.STREAK_BLOCK_DAYS > blocks
    }

    private fun buildWeekly(m: MissionsState, date: LocalDate): WeeklyChallenge {
        val objectives = m.weekly.mapNotNull { p ->
            MissionCatalog.findWeekly(p.id)?.let { Mission(it, p.progress, m.weeklyClaimed) }
        }
        return WeeklyChallenge(
            code = "MX-W%02d".format(date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)),
            title = MissionCatalog.WEEKLY_TITLE,
            subtitle = MissionCatalog.WEEKLY_SUBTITLE,
            description = MissionCatalog.WEEKLY_DESCRIPTION,
            rewardXp = MissionCatalog.WEEKLY_REWARD_XP,
            rewardCoins = MissionCatalog.WEEKLY_REWARD_COINS,
            badge = MissionCatalog.WEEKLY_BADGE,
            objectives = objectives,
            claimed = m.weeklyClaimed,
            daysLeft = DayOfWeek.SUNDAY.value - date.dayOfWeek.value + 1,
        )
    }

    private fun buildOverview(stored: MissionsState, date: LocalDate): MissionsOverview {
        val m = normalize(stored, date)
        val daily = m.daily.mapNotNull { p ->
            MissionCatalog.findDaily(p.id)?.let { Mission(it, p.progress, p.claimed) }
        }
        val monday = date.with(DayOfWeek.MONDAY)
        val letters = listOf("L", "M", "M", "J", "V", "S", "D")
        val week = letters.mapIndexed { i, letter ->
            val d = monday.plusDays(i.toLong())
            WeekDayStatus(
                letter = letter,
                checkedIn = d.toEpochDay() in m.checkInDays,
                isToday = d == date,
                isFuture = d.isAfter(date),
            )
        }
        val streak = effectiveStreak(m, date)
        val canClaim = canClaimStreak(m, date)
        val block = MissionCatalog.STREAK_BLOCK_DAYS
        return MissionsOverview(
            daily = daily,
            weekly = buildWeekly(m, date),
            streak = streak,
            week = week,
            daysToStreakReward = if (canClaim) 0 else block - (streak % block),
            canClaimStreakReward = canClaim,
        )
    }
}

/** Suma XP y monedas subiendo de nivel cada [XP_PER_LEVEL]. Devuelve el nuevo nivel si cambió. */
fun UserProfile.withRewards(xpGained: Int, coinsGained: Int, badge: String?): Pair<UserProfile, Int?> {
    var totalXp = xp + xpGained
    var newLevel = level
    while (totalXp >= XP_PER_LEVEL) {
        totalXp -= XP_PER_LEVEL
        newLevel++
    }
    val updated = copy(
        xp = totalXp,
        level = newLevel,
        coins = coins + coinsGained,
        badges = if (badge != null && badge !in badges) badges + badge else badges,
    )
    return updated to (if (newLevel > level) newLevel else null)
}
