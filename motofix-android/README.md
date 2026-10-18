# MotoFix — App Android (Kotlin + Jetpack Compose)

App nativa construida a partir de `motofix documentacion/` (requisitos y prototipo).

## Cómo abrirla
1. Instala **Android Studio** (trae su propio JDK y el SDK de Android).
2. `File > Open` y elige esta carpeta `motofix-android`.
3. Espera a que termine el *Gradle Sync* (la primera vez descarga Gradle y librerías).
4. Pulsa ▶ **Run** con un emulador o un celular conectado.
5. Para generar el APK: `Build > Build App Bundle(s) / APK(s) > Build APK(s)`.

## Datos de prueba
No hay backend todavía. Todo se guarda en el teléfono (`motofix_data.json`) y la sesión queda abierta.
- Cualquier correo válido con contraseña de 6+ caracteres entra con la cuenta demo (DaniRider, 3 motos).
- Volver a entrar con el mismo correo recupera tus datos; **Registrarse** crea una cuenta nueva y vacía.

## Estado
| Módulo | Estado |
|---|---|
| Login / Registro / Recuperar contraseña | ✅ |
| Dashboard con datos reales (nivel, XP, monedas, motos, misiones) | ✅ |
| Garaje: agregar, ver, editar y eliminar motos (RF04) | ✅ |
| Historial de mantenimiento + aviso de cambio de aceite cada 3.000 km | ✅ |
| Misiones diarias, racha semanal y desafío semanal con recompensas (RF06) | ✅ |
| Subir de nivel con XP (parte de RF07) | ✅ |
| Taller jugable: 5 niveles contra reloj, estrellas, desbloqueos (RF09) | ✅ |
| Certificados técnicos con PDF para guardar o compartir (RF10) | ✅ |
| Audio IA: grabación real, análisis acústico, historial y sonidos de ejemplo (RF05) | ✅ |
| Concesionario, Ranking, Perfil completo, Notificaciones, Árbol de habilidades | pendiente |

## Pruebas
`./gradlew testDebugUnitTest` comprueba el análisis de audio (cada falla de ejemplo se reconoce,
grabaciones malas se rechazan) y el taller (niveles ganables, recompensas, certificados, misiones).
