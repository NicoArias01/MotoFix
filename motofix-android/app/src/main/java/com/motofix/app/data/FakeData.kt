package com.motofix.app.data

import android.content.Context
import com.motofix.app.data.auth.AuthService
import com.motofix.app.data.auth.AuthUser
import com.motofix.app.data.auth.FirebaseAuthService
import com.motofix.app.data.auth.FirebaseSetup
import com.motofix.app.data.auth.LocalAuthService
import kotlinx.serialization.json.Json
import java.io.File

/** Datos fijos mientras no exista la API en Python. */
object FakeData {
    const val DEALER_MODELS = 12
    const val WELCOME_COINS = 500
}

/**
 * Dependencias compartidas de la app (inyección manual, sin librerías).
 *
 * Cada cuenta guarda sus datos en su propio archivo `account_<uid>.json`, así varias personas
 * pueden usar el mismo teléfono sin mezclar garajes ni progreso. Al iniciar o cerrar sesión
 * se vuelven a crear los repositorios apuntando al archivo de esa cuenta.
 */
object AppContainer {
    private const val GUEST = "guest"

    private lateinit var dir: File
    private lateinit var localAuth: LocalAuthService
    private val json = Json { ignoreUnknownKeys = true }

    lateinit var auth: AuthService
        private set
    lateinit var store: AppStore
        private set
    lateinit var users: UserRepository
        private set
    lateinit var missions: MissionsRepository
        private set
    lateinit var garage: GarageRepository
        private set
    lateinit var workshop: WorkshopRepository
        private set
    lateinit var diagnoses: DiagnosisRepository
        private set

    fun init(context: Context) {
        if (::auth.isInitialized) return
        dir = context.filesDir
        localAuth = LocalAuthService(File(dir, "accounts_local.json"))
        val firebase = FirebaseSetup.initialize(context)
        auth = if (firebase != null) FirebaseAuthService(firebase.webClientId) else localAuth
        migrateLegacyData()
        open(readSession() ?: GUEST)
        store.update { data ->
            val user = data.user
            if (user != null && user.createdAtMillis == 0L) data.copy(user = user.copy(createdAtMillis = System.currentTimeMillis())) else data
        }
    }

    /** Hay una sesión abierta y válida (en Firebase, además, la sesión de Firebase sigue activa). */
    val isLoggedIn: Boolean
        get() {
            val uid = readSession() ?: return false
            if (auth.isOnline && auth.currentUid != uid) return false
            return store.current.user != null
        }

    /** Abre la cuenta de un usuario recién autenticado; si es nueva, crea su perfil. */
    fun enter(user: AuthUser) {
        val file = accountFile(user.uid)
        if (!file.exists()) {
            // Si ya había datos de ese correo (versión anterior o cuenta local), se conservan
            findDataByEmail(user.email)?.copyTo(file)
        }
        open(user.uid)
        store.update { data ->
            val profile = data.user
            if (profile != null) {
                data.copy(loggedIn = true, user = profile.copy(email = user.email))
            } else {
                AppData(
                    user = UserProfile(
                        name = user.displayName?.takeIf { it.isNotBlank() } ?: user.email.substringBefore('@'),
                        email = user.email,
                        level = 1,
                        coins = FakeData.WELCOME_COINS,
                        xp = 0,
                        createdAtMillis = System.currentTimeMillis(),
                        provider = user.provider.name,
                    ),
                    loggedIn = true,
                )
            }
        }
        writeSession(user.uid)
    }

    fun logout() {
        auth.signOut()
        store.update { it.copy(loggedIn = false) }
        File(dir, "session.txt").delete()
        open(GUEST)
    }

    private fun open(uid: String) {
        store = AppStore(accountFile(uid))
        users = UserRepository(store)
        missions = MissionsRepository(store)
        garage = GarageRepository(store, missions)
        workshop = WorkshopRepository(store, missions)
        diagnoses = DiagnosisRepository(store, missions)
    }

    private fun accountFile(uid: String) = File(dir, "account_${uid.filter { it.isLetterOrDigit() || it == '-' || it == '_' }}.json")

    private fun readSession(): String? = File(dir, "session.txt").takeIf { it.exists() }?.readText()?.trim()?.ifEmpty { null }

    private fun writeSession(uid: String) = File(dir, "session.txt").writeText(uid)

    private fun readEmail(file: File): String? = runCatching {
        json.decodeFromString(AppData.serializer(), file.readText()).user?.email
    }.getOrNull()

    private fun findDataByEmail(email: String): File? = dir
        .listFiles { f -> f.name.startsWith("account_") && f.name.endsWith(".json") }
        ?.firstOrNull { readEmail(it).equals(email, ignoreCase = true) }

    /**
     * Las versiones 0.1–0.3 guardaban una sola cuenta en `motofix_data.json`, sin contraseña.
     * Se convierte en una cuenta local; la contraseña que escriba en su próximo ingreso queda guardada.
     */
    private fun migrateLegacyData() {
        val legacy = File(dir, "motofix_data.json")
        if (!legacy.exists()) return
        val data = runCatching { json.decodeFromString(AppData.serializer(), legacy.readText()) }.getOrNull()
        val user = data?.user
        if (user != null) {
            val uid = LocalAuthService.uidFor(user.email)
            val target = accountFile(uid)
            if (!target.exists()) legacy.copyTo(target)
            localAuth.ensureMigratedAccount(uid, user.email, user.name)
            // Sin Firebase se mantiene la sesión abierta; con Firebase tendrá que ingresar de nuevo
            if (data.loggedIn && !auth.isOnline) writeSession(uid)
        }
        legacy.renameTo(File(dir, "motofix_data.migrated.json"))
    }
}
