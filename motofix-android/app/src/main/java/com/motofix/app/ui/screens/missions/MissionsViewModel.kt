package com.motofix.app.ui.screens.missions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motofix.app.data.AppContainer
import com.motofix.app.data.MissionsOverview
import com.motofix.app.data.MissionsRepository
import com.motofix.app.data.RewardResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class MissionsViewModel(
    private val missions: MissionsRepository = AppContainer.missions,
) : ViewModel() {

    val overview: StateFlow<MissionsOverview> = missions.overview
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), missions.currentOverview())

    /** Recompensa recién reclamada que se muestra en la ventana de celebración. */
    var reward by mutableStateOf<RewardResult?>(null)
        private set

    fun claimDaily(id: String) { reward = missions.claimDaily(id) }
    fun claimWeekly() { reward = missions.claimWeekly() }
    fun claimStreak() { reward = missions.claimStreakReward() }
    fun dismissReward() { reward = null }
}

/** Horas que faltan para que se reinicien las misiones diarias. */
fun hoursUntilReset(): Long {
    val midnight = LocalDate.now().plusDays(1).atStartOfDay()
    return Duration.between(LocalDateTime.now(), midnight).toHours().coerceAtLeast(0) + 1
}
