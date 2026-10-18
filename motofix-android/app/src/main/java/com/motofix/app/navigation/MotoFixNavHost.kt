package com.motofix.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.motofix.app.data.AppContainer
import com.motofix.app.ui.screens.MainScreen
import com.motofix.app.ui.screens.auth.ForgotPasswordScreen
import com.motofix.app.ui.screens.auth.LoginScreen
import com.motofix.app.ui.screens.auth.RegisterScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val MAIN = "main"
}

@Composable
fun MotoFixNavHost(navController: NavHostController = rememberNavController()) {
    // Si la sesión quedó abierta, se entra directo sin pasar por el login
    val start = remember { if (AppContainer.isLoggedIn) Routes.MAIN else Routes.LOGIN }

    // Al cambiar entre login y app se borra toda la pila, así "atrás" no regresa a la otra
    fun goTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { goTo(Routes.MAIN) },
                onCreateAccount = { navController.navigate(Routes.REGISTER) },
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegistered = { goTo(Routes.MAIN) },
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MAIN) {
            MainScreen(onLogout = {
                AppContainer.logout()
                goTo(Routes.LOGIN)
            })
        }
    }
}
