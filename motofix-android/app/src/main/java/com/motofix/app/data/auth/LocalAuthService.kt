package com.motofix.app.data.auth

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Serializable
data class LocalAccount(
    val uid: String,
    val email: String,
    val name: String,
    /** null = cuenta migrada de una versión anterior; la contraseña se fija en el próximo ingreso. */
    val passwordHash: String? = null,
    val salt: String? = null,
)

/**
 * Cuentas guardadas solo en este teléfono, para cuando Firebase no está configurado.
 * Las contraseñas nunca se guardan: solo su hash PBKDF2 con sal aleatoria (RNF01).
 */
class LocalAuthService(private val file: File) : AuthService {

    private val json = Json { ignoreUnknownKeys = true }
    private val random = SecureRandom()

    override val isOnline = false
    override val currentUid: String? = null // La sesión local la maneja SessionStore

    private fun load(): List<LocalAccount> = runCatching {
        if (file.exists()) json.decodeFromString(ListSerializer(LocalAccount.serializer()), file.readText()) else emptyList()
    }.getOrDefault(emptyList())

    private fun save(accounts: List<LocalAccount>) {
        file.writeText(json.encodeToString(ListSerializer(LocalAccount.serializer()), accounts))
    }

    override suspend fun signIn(email: String, password: String): AuthUser = withContext(Dispatchers.Default) {
        val accounts = load()
        val account = accounts.firstOrNull { it.email.equals(email, ignoreCase = true) }
            ?: throw AuthException("No existe una cuenta con ese correo. Toca \"Crear cuenta\" para registrarte.")
        if (account.passwordHash == null) {
            // Cuenta de la versión anterior (sin contraseña guardada): la que escriba ahora queda como su contraseña
            save(accounts.map { if (it.uid == account.uid) withPassword(it, password) else it })
        } else if (!matches(account, password)) {
            throw AuthException("Contraseña incorrecta.")
        }
        AuthUser(account.uid, account.email, account.name, AuthProvider.PASSWORD)
    }

    override suspend fun register(name: String, email: String, password: String): AuthUser = withContext(Dispatchers.Default) {
        val accounts = load()
        if (accounts.any { it.email.equals(email, ignoreCase = true) }) {
            throw AuthException("Ya existe una cuenta con ese correo. Inicia sesión.")
        }
        val account = withPassword(LocalAccount(uid = uidFor(email), email = email, name = name), password)
        save(accounts + account)
        AuthUser(account.uid, account.email, account.name, AuthProvider.PASSWORD)
    }

    override suspend fun sendPasswordReset(email: String) {
        throw AuthException("El correo de recuperación se activa al conectar MotoFix con Firebase.")
    }

    override suspend fun signInWithGoogle(activity: Activity): AuthUser = throw AuthException(OFFLINE_SOCIAL_MESSAGE)

    override suspend fun signInWithGitHub(activity: Activity): AuthUser = throw AuthException(OFFLINE_SOCIAL_MESSAGE)

    override suspend fun changePassword(email: String, current: String, new: String) = withContext(Dispatchers.Default) {
        val accounts = load()
        val account = accounts.firstOrNull { it.email.equals(email, ignoreCase = true) }
            ?: throw AuthException("No se encontró la cuenta.")
        if (account.passwordHash != null && !matches(account, current)) throw AuthException("La contraseña actual no es correcta.")
        save(accounts.map { if (it.uid == account.uid) withPassword(it, new) else it })
    }

    override fun signOut() = Unit

    /** Registra una cuenta que venía de la versión anterior de la app (sin contraseña). */
    fun ensureMigratedAccount(uid: String, email: String, name: String) {
        val accounts = load()
        if (accounts.none { it.email.equals(email, ignoreCase = true) }) {
            save(accounts + LocalAccount(uid = uid, email = email, name = name))
        }
    }

    private fun withPassword(account: LocalAccount, password: String): LocalAccount {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        return account.copy(passwordHash = hash(password, salt), salt = Base64.getEncoder().encodeToString(salt))
    }

    private fun matches(account: LocalAccount, password: String): Boolean {
        val salt = Base64.getDecoder().decode(account.salt ?: return false)
        return MessageDigest.isEqual(hash(password, salt).toByteArray(), account.passwordHash!!.toByteArray())
    }

    private fun hash(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(key)
    }

    companion object {
        private const val ITERATIONS = 120_000

        fun uidFor(email: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(email.lowercase().trim().toByteArray())
            return "local-" + digest.take(8).joinToString("") { "%02x".format(it) }
        }
    }
}
