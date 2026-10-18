package com.motofix.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motofix.app.data.AppContainer
import com.motofix.app.data.GarageRepository
import com.motofix.app.data.Mission
import com.motofix.app.data.MissionsRepository
import com.motofix.app.data.UserProfile
import com.motofix.app.data.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardState(
    val user: UserProfile? = null,
    val motorcycles: Int = 0,
    val missions: List<Mission> = emptyList(),
    val pendingClaims: Int = 0,
)

class DashboardViewModel(
    users: UserRepository = AppContainer.users,
    garage: GarageRepository = AppContainer.garage,
    missions: MissionsRepository = AppContainer.missions,
) : ViewModel() {

    val state: StateFlow<DashboardState> =
        combine(users.user, garage.motorcycles, missions.overview) { user, motos, overview ->
            DashboardState(user, motos.size, overview.daily, overview.pendingClaims)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppContainer.store.current.let { data ->
                val overview = missions.currentOverview()
                DashboardState(data.user, data.motorcycles.size, overview.daily, overview.pendingClaims)
            },
        )
}
