package com.motofix.app.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.motofix.app.data.AppContainer
import com.motofix.app.ui.components.ConfirmDialog
import com.motofix.app.ui.components.SecondaryButton
import com.motofix.app.ui.screens.dashboard.DashboardScreen
import com.motofix.app.ui.screens.garage.GarageHistoryScreen
import com.motofix.app.ui.screens.garage.GarageScreen
import com.motofix.app.ui.screens.garage.MaintenanceFormScreen
import com.motofix.app.ui.screens.garage.MotorcycleDetailScreen
import com.motofix.app.ui.screens.garage.MotorcycleFormScreen
import com.motofix.app.ui.screens.missions.MissionsScreen
import com.motofix.app.ui.screens.missions.WeeklyChallengeScreen
import com.motofix.app.ui.screens.audio.AudioScreen
import com.motofix.app.ui.screens.audio.DiagnosisDetailScreen
import com.motofix.app.ui.screens.workshop.CertificateScreen
import com.motofix.app.ui.screens.workshop.CertificatesScreen
import com.motofix.app.ui.screens.workshop.RepairGameScreen
import com.motofix.app.ui.screens.workshop.WorkshopScreen
import com.motofix.app.ui.screens.profile.ProfileScreen
import com.motofix.app.ui.screens.profile.SecurityScreen
import com.motofix.app.ui.screens.profile.SettingsScreen
import com.motofix.app.ui.theme.Background
import com.motofix.app.ui.theme.ErrorRed
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class MainTab(val route: String, val label: String, val icon: ImageVector) {
    INICIO("inicio", "Inicio", Icons.Outlined.Home),
    TALLER("taller", "Taller", Icons.Outlined.Build),
    AUDIO("audio", "Audio IA", Icons.Outlined.Mic),
    GARAJE("garaje", "Garaje", Icons.Outlined.TwoWheeler),
    PERFIL("perfil", "Perfil", Icons.Outlined.Person),
}

/** Rutas internas de las pestañas. Las que empiezan por el nombre de una pestaña la marcan como activa. */
private object InnerRoutes {
    const val MISSIONS = "inicio/misiones"
    const val WEEKLY = "inicio/misiones/semanal"
    const val GARAGE_NEW = "garaje/nueva"
    const val GARAGE_HISTORY = "garaje/historial"
    const val MOTO = "garaje/moto/{id}"
    const val MOTO_EDIT = "garaje/moto/{id}/editar"
    const val MOTO_MAINTENANCE = "garaje/moto/{id}/mantenimiento"

    fun moto(id: String) = "garaje/moto/$id"
    fun motoEdit(id: String) = "garaje/moto/$id/editar"
    fun motoMaintenance(id: String) = "garaje/moto/$id/mantenimiento"

    const val GAME = "taller/juego/{id}"
    const val CERTIFICATES = "taller/certificados"
    const val CERTIFICATE = "taller/certificado/{id}"
    const val DIAGNOSIS = "audio/diagnostico/{id}"
    const val SETTINGS = "perfil/ajustes"
    const val SECURITY = "perfil/seguridad"

    fun game(id: String) = "taller/juego/$id"
    fun certificate(id: String) = "taller/certificado/$id"
    fun diagnosis(id: String) = "audio/diagnostico/$id"
}

private fun tabFor(route: String?): MainTab? =
    MainTab.entries.firstOrNull { route == it.route || route?.startsWith(it.route + "/") == true }

private fun NavHostController.openTab(tab: MainTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Contenedor con la barra inferior de 5 pestañas del prototipo. */
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = tabFor(currentRoute)
    // Durante el juego de reparación la pantalla completa es para el taller
    val showBottomBar = currentRoute != InnerRoutes.GAME
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Entrada del día: mantiene la racha y cumple la misión "Inicia sesión hoy"
    LaunchedEffect(Unit) { AppContainer.missions.checkIn() }

    fun showComingSoon(feature: String) {
        scope.launch { snackbarHostState.showSnackbar("$feature: próximamente") }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = com.motofix.app.ui.theme.SurfaceRaised,
                    contentColor = com.motofix.app.ui.theme.TextPrimary,
                    actionColor = Orange,
                )
            }
        },
        bottomBar = {
            if (showBottomBar) NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = {
                            // Tocar la pestaña activa vuelve a su pantalla principal
                            if (currentTab == tab) nav.popBackStack(tab.route, inclusive = false) else nav.openTab(tab)
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Orange,
                            selectedTextColor = Orange,
                            indicatorColor = Orange.copy(alpha = 0.12f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = MainTab.INICIO.route,
            modifier = Modifier.padding(padding),
        ) {
            // ---------- Inicio ----------
            composable(MainTab.INICIO.route) {
                DashboardScreen(
                    onOpenWorkshop = { nav.openTab(MainTab.TALLER) },
                    onOpenAudio = { nav.openTab(MainTab.AUDIO) },
                    onOpenGarage = { nav.openTab(MainTab.GARAJE) },
                    onOpenDealer = { showComingSoon("Concesionario") },
                    onOpenNotifications = { showComingSoon("Notificaciones") },
                    onOpenMissions = { nav.navigate(InnerRoutes.MISSIONS) },
                )
            }
            composable(InnerRoutes.MISSIONS) {
                MissionsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenWeekly = { nav.navigate(InnerRoutes.WEEKLY) },
                )
            }
            composable(InnerRoutes.WEEKLY) {
                WeeklyChallengeScreen(onBack = { nav.popBackStack() })
            }

            // ---------- Garaje ----------
            composable(MainTab.GARAJE.route) {
                GarageScreen(
                    onAddMotorcycle = { nav.navigate(InnerRoutes.GARAGE_NEW) },
                    onOpenMotorcycle = { nav.navigate(InnerRoutes.moto(it)) },
                    onReviewAi = { nav.openTab(MainTab.AUDIO) },
                    onOpenHistory = { nav.navigate(InnerRoutes.GARAGE_HISTORY) },
                )
            }
            composable(InnerRoutes.GARAGE_NEW) {
                MotorcycleFormScreen(
                    motoId = null,
                    onBack = { nav.popBackStack() },
                    onSaved = { id ->
                        // Tras crearla se abre su ficha, y "atrás" vuelve al garaje
                        nav.navigate(InnerRoutes.moto(id)) {
                            popUpTo(MainTab.GARAJE.route)
                        }
                    },
                    onDeleted = { nav.popBackStack() },
                )
            }
            composable(InnerRoutes.GARAGE_HISTORY) {
                GarageHistoryScreen(
                    onBack = { nav.popBackStack() },
                    onOpenMotorcycle = { nav.navigate(InnerRoutes.moto(it)) },
                )
            }
            composable(InnerRoutes.MOTO, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                MotorcycleDetailScreen(
                    motoId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(InnerRoutes.motoEdit(id)) },
                    onAddMaintenance = { nav.navigate(InnerRoutes.motoMaintenance(id)) },
                    onReviewAi = { nav.openTab(MainTab.AUDIO) },
                )
            }
            composable(InnerRoutes.MOTO_EDIT, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                MotorcycleFormScreen(
                    motoId = id,
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack() },
                    onDeleted = { nav.popBackStack(MainTab.GARAJE.route, inclusive = false) },
                )
            }
            composable(InnerRoutes.MOTO_MAINTENANCE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                MaintenanceFormScreen(
                    motoId = id,
                    onBack = { nav.popBackStack() },
                    onSaved = {
                        nav.popBackStack()
                        scope.launch { snackbarHostState.showSnackbar("Servicio guardado en el historial") }
                    },
                )
            }

            // ---------- Taller ----------
            composable(MainTab.TALLER.route) {
                WorkshopScreen(
                    onPlay = { nav.navigate(InnerRoutes.game(it)) },
                    onOpenCertificates = { nav.navigate(InnerRoutes.CERTIFICATES) },
                )
            }
            composable(InnerRoutes.GAME, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                RepairGameScreen(
                    scenarioId = id,
                    onExit = { nav.popBackStack() },
                    onOpenCertificate = { certId ->
                        nav.navigate(InnerRoutes.certificate(certId)) {
                            popUpTo(InnerRoutes.GAME) { inclusive = true }
                        }
                    },
                    onNextLevel = { next ->
                        nav.navigate(InnerRoutes.game(next)) {
                            popUpTo(InnerRoutes.GAME) { inclusive = true }
                        }
                    },
                )
            }
            composable(InnerRoutes.CERTIFICATES) {
                CertificatesScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { nav.navigate(InnerRoutes.certificate(it)) },
                    onGoToWorkshop = { nav.popBackStack(MainTab.TALLER.route, inclusive = false) },
                )
            }
            composable(InnerRoutes.CERTIFICATE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                CertificateScreen(
                    certificateId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }

            // ---------- Audio IA ----------
            composable(MainTab.AUDIO.route) {
                AudioScreen(
                    onOpenDiagnosis = { nav.navigate(InnerRoutes.diagnosis(it)) },
                    onPractice = { nav.navigate(InnerRoutes.game(it)) },
                    onRegisterRepair = { nav.navigate(InnerRoutes.motoMaintenance(it)) },
                    onGoToGarage = { nav.openTab(MainTab.GARAJE) },
                )
            }
            composable(InnerRoutes.DIAGNOSIS, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                DiagnosisDetailScreen(
                    diagnosisId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onPractice = { nav.navigate(InnerRoutes.game(it)) },
                    onRegisterRepair = { nav.navigate(InnerRoutes.motoMaintenance(it)) },
                )
            }

            // ---------- Perfil ----------
            composable(MainTab.PERFIL.route) {
                ProfileScreen(
                    onOpenSettings = { nav.navigate(InnerRoutes.SETTINGS) },
                    onOpenSecurity = { nav.navigate(InnerRoutes.SECURITY) },
                    onOpenCertificates = { nav.navigate(InnerRoutes.CERTIFICATES) },
                    onOpenGarage = { nav.openTab(MainTab.GARAJE) },
                    onLogout = onLogout,
                )
            }
            composable(InnerRoutes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable(InnerRoutes.SECURITY) { SecurityScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
