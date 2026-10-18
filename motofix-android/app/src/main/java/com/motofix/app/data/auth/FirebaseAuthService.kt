package com.motofix.app.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Datos que se leen de google-services.json. */
data class FirebaseConfig(val options: FirebaseOptions, val webClientId: String?)

/**
 * Conecta la app con Firebase leyendo `assets/google-services.json`.
 * Si el archivo no existe, la app funciona con cuentas locales.
 */
object FirebaseSetup {
    private const val CONFIG_FILE = "google-services.json"

    fun initialize(context: Context): FirebaseConfig? {
        val config = runCatching { read(context) }.getOrNull() ?: return null
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context, config.options)
        }
        return config
    }

    private fun read(context: Context): FirebaseConfig? {
        val text = runCatching { context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() } }.getOrNull() ?: return null
        val root = Json.parseToJsonElement(text).jsonObject
        val project = root["project_info"]!!.jsonObject
        val client = root["client"]!!.jsonArray
            .map { it.jsonObject }
            .firstOrNull { c ->
                c["client_info"]?.jsonObject?.get("android_client_info")?.jsonObject
                    ?.get("package_name")?.jsonPrimitive?.content == context.packageName
            } ?: return null
        val appId = client["client_info"]!!.jsonObject["mobilesdk_app_id"]!!.jsonPrimitive.content
        val apiKey = client["api_key"]!!.jsonArray.first().jsonObject["current_key"]!!.jsonPrimitive.content
        // El "cliente web" (tipo 3) es el que pide el inicio con Google
        val webClientId = client["oauth_client"]?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { it["client_type"]?.jsonPrimitive?.intOrNull == 3 }
            ?.get("client_id")?.jsonPrimitive?.content
        val options = FirebaseOptions.Builder()
            .setApplicationId(appId)
            .setApiKey(apiKey)
            .setProjectId(project["project_id"]!!.jsonPrimitive.content)
            .setGcmSenderId(project["project_number"]?.jsonPrimitive?.content)
            .setStorageBucket(project["storage_bucket"]?.jsonPrimitive?.content)
            .build()
        return FirebaseConfig(options, webClientId)
    }

    @Suppress("unused")
    private fun JsonObject.intValue(key: String) = this[key]?.jsonPrimitive?.int
}

/** Cuentas reales con Firebase Authentication: correo, Google y GitHub. */
class FirebaseAuthService(private val webClientId: String?) : AuthService {

    private val auth = FirebaseAuth.getInstance().apply { setLanguageCode("es") }

    override val isOnline = true
    override val currentUid: String? get() = auth.currentUser?.uid

    override suspend fun signIn(email: String, password: String): AuthUser = guard {
        auth.signInWithEmailAndPassword(email, password).await().toAuthUser(AuthProvider.PASSWORD)
    }

    override suspend fun register(name: String, email: String, password: String): AuthUser = guard {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.updateProfile(userProfileChangeRequest { displayName = name })?.await()
        AuthUser(result.user!!.uid, email, name, AuthProvider.PASSWORD)
    }

    override suspend fun sendPasswordReset(email: String) = guard {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    override suspend fun signInWithGoogle(activity: Activity): AuthUser = guard {
        val clientId = webClientId ?: throw AuthException(
            "Falta activar Google en Firebase. Actívalo y vuelve a descargar google-services.json."
        )
        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw AuthException("Inicio con Google cancelado.")
        } catch (e: NoCredentialException) {
            throw AuthException("No hay cuentas de Google en este teléfono.")
        } catch (e: GetCredentialException) {
            throw AuthException("Google no respondió: ${e.message ?: "inténtalo de nuevo"}.")
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthException("Google devolvió una credencial desconocida.")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().toAuthUser(AuthProvider.GOOGLE)
    }

    override suspend fun signInWithGitHub(activity: Activity): AuthUser = guard {
        val provider = OAuthProvider.newBuilder("github.com").apply { scopes = listOf("user:email") }.build()
        // Si Android cerró la app mientras estaba abierta la página de GitHub, se retoma ese resultado
        val result = auth.pendingAuthResult?.await() ?: auth.startActivityForSignInWithProvider(activity, provider).await()
        val username = result.additionalUserInfo?.username
        val user = result.toAuthUser(AuthProvider.GITHUB)
        if (user.displayName.isNullOrBlank() && username != null) user.copy(displayName = username) else user
    }

    override suspend fun changePassword(email: String, current: String, new: String) = guard {
        val user = auth.currentUser ?: throw AuthException("Vuelve a iniciar sesión e inténtalo de nuevo.")
        user.reauthenticate(EmailAuthProvider.getCredential(email, current)).await()
        user.updatePassword(new).await()
        Unit
    }

    override fun signOut() = auth.signOut()

    private fun AuthResult.toAuthUser(provider: AuthProvider): AuthUser {
        val user = user ?: throw AuthException("No se pudo iniciar sesión.")
        val email = user.email ?: user.providerData.firstNotNullOfOrNull { it.email }
            ?: throw AuthException("Tu cuenta de ${provider.label} no tiene un correo visible. Hazlo público e inténtalo de nuevo.")
        return AuthUser(user.uid, email, user.displayName, provider)
    }

    /** Traduce los errores de Firebase a mensajes claros en español. */
    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: AuthException) {
        throw e
    } catch (e: FirebaseNetworkException) {
        throw AuthException("Sin conexión a internet. Revisa tu Wi-Fi o datos móviles.")
    } catch (e: FirebaseTooManyRequestsException) {
        throw AuthException("Demasiados intentos. Espera unos minutos.")
    } catch (e: FirebaseAuthWeakPasswordException) {
        throw AuthException("La contraseña es muy débil: usa al menos 6 caracteres.")
    } catch (e: FirebaseAuthUserCollisionException) {
        throw AuthException(
            if (e.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") "Ya existe una cuenta con ese correo. Inicia sesión."
            else "Ese correo ya está registrado con otro método (correo, Google o GitHub). Entra con ese método."
        )
    } catch (e: FirebaseAuthInvalidUserException) {
        throw AuthException("No existe una cuenta con ese correo. Toca \"Crear cuenta\" para registrarte.")
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        throw AuthException("Correo o contraseña incorrectos. Si no tienes cuenta, créala.")
    } catch (e: FirebaseAuthException) {
        throw AuthException(
            when (e.errorCode) {
                "ERROR_WEB_CONTEXT_CANCELED" -> "Inicio de sesión cancelado."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Este método de inicio no está activado en Firebase."
                else -> "No se pudo completar: ${e.localizedMessage ?: e.errorCode}"
            }
        )
    }
}

/** Espera una tarea de Google Play Services sin bloquear. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (error != null) cont.resumeWithException(error)
        else if (task.isCanceled) cont.resumeWithException(AuthException("Operación cancelada."))
        else cont.resume(task.result)
    }
}
