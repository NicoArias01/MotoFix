package com.motofix.app.data.auth

import android.app.Activity

enum class AuthProvider(val label: String) {
    PASSWORD("Correo y contraseña"),
    GOOGLE("Google"),
    GITHUB("GitHub"),
}

/** Usuario autenticado, venga de Firebase o de una cuenta local. */
data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String?,
    val provider: AuthProvider,
)

/** Error con un mensaje listo para mostrarle al usuario. */
class AuthException(message: String) : Exception(message)

/** Módulo de cuentas (RF01, RF02, RF03, RF13). */
interface AuthService {
    /** true si las cuentas viven en Firebase; false si solo en este teléfono. */
    val isOnline: Boolean

    /** uid de la sesión que el servicio todavía considera abierta. */
    val currentUid: String?

    suspend fun signIn(email: String, password: String): AuthUser
    suspend fun register(name: String, email: String, password: String): AuthUser
    suspend fun sendPasswordReset(email: String)
    suspend fun signInWithGoogle(activity: Activity): AuthUser
    suspend fun signInWithGitHub(activity: Activity): AuthUser
    suspend fun changePassword(email: String, current: String, new: String)
    fun signOut()
}

const val OFFLINE_SOCIAL_MESSAGE =
    "El inicio con Google y GitHub se activa al conectar MotoFix con Firebase. Por ahora crea una cuenta con tu correo."
