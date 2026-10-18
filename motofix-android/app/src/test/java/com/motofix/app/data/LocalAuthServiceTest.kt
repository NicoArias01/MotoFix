package com.motofix.app.data

import com.motofix.app.data.auth.AuthException
import com.motofix.app.data.auth.LocalAuthService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocalAuthServiceTest {

    private fun service(): Pair<LocalAuthService, File> {
        val file = File.createTempFile("cuentas", ".json").apply { delete(); deleteOnExit() }
        return LocalAuthService(file) to file
    }

    private fun failsWith(text: String, block: suspend () -> Unit) {
        try {
            runBlocking { block() }
            throw AssertionError("Se esperaba un error")
        } catch (e: AuthException) {
            assertTrue("Mensaje inesperado: ${e.message}", e.message!!.contains(text))
        }
    }

    @Test
    fun noSePuedeEntrarSinCrearCuenta() {
        val (auth, _) = service()
        failsWith("No existe una cuenta") { auth.signIn("nuevo@correo.com", "secreto123") }
    }

    @Test
    fun registrarEntrarYRechazarContrasenaIncorrecta() = runBlocking {
        val (auth, file) = service()
        val user = auth.register("Nico", "nico@correo.com", "secreto123")
        assertEquals("nico@correo.com", user.email)
        assertEquals(user.uid, auth.signIn("NICO@correo.com", "secreto123").uid)
        failsWith("Contraseña incorrecta") { auth.signIn("nico@correo.com", "otra") }
        failsWith("Ya existe una cuenta") { auth.register("Otro", "nico@correo.com", "secreto123") }
        // La contraseña nunca se guarda en texto plano
        assertFalse(file.readText().contains("secreto123"))
    }

    @Test
    fun cambiarContrasena() = runBlocking {
        val (auth, _) = service()
        auth.register("Nico", "nico@correo.com", "secreto123")
        failsWith("actual no es correcta") { auth.changePassword("nico@correo.com", "mala", "nueva12345") }
        auth.changePassword("nico@correo.com", "secreto123", "nueva12345")
        auth.signIn("nico@correo.com", "nueva12345")
        failsWith("Contraseña incorrecta") { auth.signIn("nico@correo.com", "secreto123") }
    }

    @Test
    fun cuentaMigradaFijaSuContrasenaEnElPrimerIngreso() = runBlocking {
        val (auth, _) = service()
        val uid = LocalAuthService.uidFor("viejo@correo.com")
        auth.ensureMigratedAccount(uid, "viejo@correo.com", "Viejo")
        assertEquals(uid, auth.signIn("viejo@correo.com", "mipass1").uid)
        failsWith("Contraseña incorrecta") { auth.signIn("viejo@correo.com", "otra") }
    }

    @Test
    fun googleYGithubPidenFirebase() {
        val (auth, _) = service()
        assertFalse(auth.isOnline)
    }
}
