package com.motofix.app.ui.screens.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motofix.app.data.AppContainer
import com.motofix.app.data.AppData
import com.motofix.app.data.AppSettings
import com.motofix.app.data.MissionCatalog
import com.motofix.app.data.UserProfile
import com.motofix.app.data.WorkshopCatalog
import com.motofix.app.data.auth.AuthException
import com.motofix.app.data.auth.AuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Achievement(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val progress: Int,
    val target: Int,
) {
    val unlocked: Boolean get() = progress >= target
}

data class ProfileState(
    val user: UserProfile?,
    val settings: AppSettings,
    val achievements: List<Achievement>,
    val motorcycles: Int,
    val repairs: Int,
    val certificates: Int,
) {
    val unlockedAchievements: Int get() = achievements.count { it.unlocked }
    val provider: AuthProvider
        get() = AuthProvider.entries.firstOrNull { it.name == user?.provider } ?: AuthProvider.PASSWORD
}

private fun buildState(data: AppData): ProfileState {
    val missions = data.missions
    val streak = if (missions.lastCheckInEpochDay >= LocalDate.now().toEpochDay() - 1) missions.streak else 0
    val completedLevels = WorkshopCatalog.scenarios.count { (data.workshop[it.id]?.completions ?: 0) > 0 }
    val achievements = listOf(
        Achievement("Primer servicio", "Registra un mantenimiento en tu garaje", Icons.Outlined.Build, data.maintenance.size, 1),
        Achievement("Coleccionista", "Ten 3 motos en tu garaje", Icons.Outlined.Collections, data.motorcycles.size, 3),
        Achievement("Mecánico certificado", "Obtén tu primer certificado técnico", Icons.Outlined.Verified, data.certificates.size, 1),
        Achievement("Perfeccionista", "Completa un nivel con 3 estrellas", Icons.Outlined.Star,
            if (data.workshop.values.any { it.bestStars == 3 }) 1 else 0, 1),
        Achievement("Oído de oro", "Haz un diagnóstico grabando tu moto", Icons.Outlined.Hearing,
            data.diagnoses.count { !it.isDemo }, 1),
        Achievement("Racha de fuego", "Entra 7 días seguidos", Icons.Outlined.LocalFireDepartment, streak, 7),
        Achievement("Maestro del taller", "Completa todos los niveles del taller", Icons.Outlined.EmojiEvents,
            completedLevels, WorkshopCatalog.scenarios.size),
        Achievement("Mantenimiento PRO", "Gana el desafío semanal", Icons.Outlined.MilitaryTech,
            if (data.user?.badges?.contains(MissionCatalog.WEEKLY_BADGE) == true) 1 else 0, 1),
    )
    return ProfileState(
        user = data.user,
        settings = data.settings,
        achievements = achievements,
        motorcycles = data.motorcycles.size,
        repairs = data.workshop.values.sumOf { it.completions },
        certificates = data.certificates.size,
    )
}

class ProfileViewModel : ViewModel() {
    private val users = AppContainer.users

    val state: StateFlow<ProfileState> = AppContainer.store.data
        .map(::buildState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildState(AppContainer.store.current))

    fun rename(name: String): String? {
        val clean = name.trim()
        if (clean.length < 2) return "Escribe al menos 2 letras"
        if (clean.length > 30) return "Máximo 30 caracteres"
        users.updateName(clean)
        return null
    }

    fun setHaptics(on: Boolean) = users.updateSettings { it.copy(haptics = on) }
    fun setSoundEffects(on: Boolean) = users.updateSettings { it.copy(soundEffects = on) }
    fun setNotifications(on: Boolean) = users.updateSettings { it.copy(notifications = on) }
}

/** Cambio de contraseña (prototipo "Seguridad"). */
data class SecurityState(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val error: String? = null,
    val success: Boolean = false,
    val isLoading: Boolean = false,
) {
    val hasLength: Boolean get() = new.length >= 8
    val hasDigit: Boolean get() = new.any { it.isDigit() }
    val hasSymbol: Boolean get() = new.any { !it.isLetterOrDigit() }
    val matches: Boolean get() = new.isNotEmpty() && new == confirm
    val canSubmit: Boolean get() = current.isNotEmpty() && hasLength && hasDigit && matches && !isLoading
}

class SecurityViewModel : ViewModel() {
    var state by mutableStateOf(SecurityState())
        private set

    val isOnline: Boolean = AppContainer.auth.isOnline

    fun onCurrent(v: String) { state = state.copy(current = v, error = null, success = false) }
    fun onNew(v: String) { state = state.copy(new = v, error = null, success = false) }
    fun onConfirm(v: String) { state = state.copy(confirm = v, error = null, success = false) }

    fun submit() {
        val s = state
        val email = AppContainer.store.current.user?.email ?: return
        if (!s.canSubmit) return
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            state = try {
                AppContainer.auth.changePassword(email, s.current, s.new)
                SecurityState(success = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                state.copy(isLoading = false, error = e.message)
            } catch (e: Exception) {
                state.copy(isLoading = false, error = "No se pudo cambiar. Revisa tu conexión.")
            }
        }
    }
}

fun memberSince(millis: Long): String {
    if (millis <= 0) return "Mecánico de MotoFix"
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    val month = date.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es-CO"))
    return "Mecánico desde $month de ${date.year}"
}
