package com.motofix.app.ui.screens.auth

import android.app.Activity
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motofix.app.data.AppContainer
import com.motofix.app.data.BikeType
import com.motofix.app.data.auth.AuthException
import com.motofix.app.data.auth.AuthService
import com.motofix.app.data.auth.AuthUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

object Validators {
    fun email(value: String): String? = when {
        value.isBlank() -> "Ingresa tu correo electrónico"
        !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> "El correo no es válido"
        else -> null
    }

    fun password(value: String): String? =
        if (value.length < 6) "La contraseña debe tener al menos 6 caracteres" else null
}

/** Mensaje de error para mostrar, sin exponer detalles técnicos. */
private fun Throwable.userMessage(): String = when (this) {
    is AuthException -> message ?: "No se pudo completar."
    else -> "Algo salió mal. Revisa tu conexión e inténtalo de nuevo."
}

// ---------- Login (RF02) ----------

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    /** Botón que está cargando: "email", "google" o "github". */
    val loadingMethod: String? = null,
)

class LoginViewModel(
    private val auth: AuthService = AppContainer.auth,
) : ViewModel() {

    var state by mutableStateOf(LoginUiState())
        private set

    val socialAvailable: Boolean get() = auth.isOnline

    fun onEmailChange(value: String) {
        state = state.copy(email = value, emailError = null, generalError = null)
    }

    fun onPasswordChange(value: String) {
        state = state.copy(password = value, passwordError = null, generalError = null)
    }

    fun login(onSuccess: () -> Unit) {
        val emailError = Validators.email(state.email)
        val passwordError = if (state.password.isEmpty()) "Ingresa tu contraseña" else null
        if (emailError != null || passwordError != null) {
            state = state.copy(emailError = emailError, passwordError = passwordError)
            return
        }
        run("email", onSuccess) { auth.signIn(state.email.trim(), state.password) }
    }

    fun loginWithGoogle(activity: Activity, onSuccess: () -> Unit) = run("google", onSuccess) { auth.signInWithGoogle(activity) }

    fun loginWithGitHub(activity: Activity, onSuccess: () -> Unit) = run("github", onSuccess) { auth.signInWithGitHub(activity) }

    private fun run(method: String, onSuccess: () -> Unit, block: suspend () -> AuthUser) {
        if (state.isLoading) return
        viewModelScope.launch {
            state = state.copy(isLoading = true, loadingMethod = method, generalError = null)
            try {
                AppContainer.enter(block())
                state = state.copy(isLoading = false, loadingMethod = null)
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(isLoading = false, loadingMethod = null, generalError = e.userMessage())
            }
        }
    }
}

// ---------- Registro (RF01) ----------

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val bike: BikeType? = null,
    val acceptedTerms: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val formError: String? = null,
    val isLoading: Boolean = false,
)

class RegisterViewModel(
    private val auth: AuthService = AppContainer.auth,
) : ViewModel() {

    var state by mutableStateOf(RegisterUiState())
        private set

    fun onNameChange(value: String) { state = state.copy(name = value.take(30), nameError = null) }
    fun onEmailChange(value: String) { state = state.copy(email = value, emailError = null, formError = null) }
    fun onPasswordChange(value: String) { state = state.copy(password = value, passwordError = null) }
    fun onConfirmChange(value: String) { state = state.copy(confirmPassword = value, confirmError = null) }
    fun onBikeSelected(bike: BikeType) { state = state.copy(bike = bike, formError = null) }
    fun onTermsChange(accepted: Boolean) { state = state.copy(acceptedTerms = accepted, formError = null) }

    fun register(onSuccess: () -> Unit) {
        val s = state
        val nameError = if (s.name.isBlank()) "Ingresa tu nombre o apodo" else null
        val emailError = Validators.email(s.email)
        val passwordError = Validators.password(s.password)
        val confirmError = if (s.confirmPassword != s.password) "Las contraseñas no coinciden" else null
        val formError = when {
            s.bike == null -> "Elige el tipo de tu máquina"
            !s.acceptedTerms -> "Debes aceptar los términos y condiciones"
            else -> null
        }
        state = s.copy(
            nameError = nameError,
            emailError = emailError,
            passwordError = passwordError,
            confirmError = confirmError,
            formError = formError,
        )
        if (listOf(nameError, emailError, passwordError, confirmError, formError).any { it != null } || s.isLoading) return

        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                AppContainer.enter(auth.register(s.name.trim(), s.email.trim(), s.password))
                state = state.copy(isLoading = false)
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(isLoading = false, formError = e.userMessage())
            }
        }
    }
}

// ---------- Recuperar contraseña (RF03) ----------

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    val sent: Boolean = false,
)

class ForgotPasswordViewModel(
    private val auth: AuthService = AppContainer.auth,
) : ViewModel() {

    var state by mutableStateOf(ForgotPasswordUiState())
        private set

    fun onEmailChange(value: String) {
        state = state.copy(email = value, emailError = null, error = null, sent = false)
    }

    fun sendCode() {
        val error = Validators.email(state.email)
        if (error != null) {
            state = state.copy(emailError = error)
            return
        }
        if (state.isLoading) return
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            state = try {
                auth.sendPasswordReset(state.email.trim())
                state.copy(isLoading = false, sent = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.copy(isLoading = false, error = e.userMessage())
            }
        }
    }
}
