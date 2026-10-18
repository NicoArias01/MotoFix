# Configurar Firebase para MotoFix (≈15 min)

Con esto funcionan de verdad: cuentas con correo, **Google**, **GitHub** y el correo de **recuperar contraseña**.

## 1. Crear el proyecto
1. Entra a https://console.firebase.google.com con tu cuenta de Google.
2. **Crear un proyecto** → nombre `MotoFix` → puedes desactivar Google Analytics → **Crear proyecto**.

## 2. Registrar la app Android
1. En la página del proyecto, toca el ícono de **Android**.
2. Llena:
   - **Nombre del paquete:** `com.motofix.app`
   - **Sobrenombre:** `MotoFix`
   - **Certificado SHA-1:** `E7:58:3E:75:DC:A8:F9:30:C0:C2:B0:4E:99:06:97:23:04:E9:B3:5F`
3. **Registrar app**. Puedes saltar los pasos de descargar el archivo y agregar el SDK (lo descargamos al final).

## 3. Activar los métodos de inicio de sesión
Menú izquierdo → **Compilación → Authentication → Comenzar → Sign-in method**:

1. **Correo electrónico/contraseña** → Habilitar → Guardar.
2. **Google** → Habilitar → elige tu correo de asistencia → Guardar.
3. **GitHub** → Habilitar. Te pide *ID de cliente* y *secreto*; deja esa pestaña abierta y copia la **URL de devolución de llamada** que muestra (termina en `/__/auth/handler`). Luego:
   1. En otra pestaña: https://github.com/settings/developers → **OAuth Apps → New OAuth App**.
   2. **Application name:** `MotoFix`
   3. **Homepage URL:** `https://TU-PROYECTO.firebaseapp.com` (la misma parte inicial de la URL de devolución).
   4. **Authorization callback URL:** pega la URL de devolución de Firebase.
   5. **Register application** → copia el **Client ID** → **Generate a new client secret** → cópialo.
   6. Pega ambos en Firebase → **Guardar**.

## 4. Descargar el archivo de configuración
⚠️ Hazlo **después** de activar Google (así el archivo incluye el ID que necesita el botón de Google).

1. ⚙️ **Configuración del proyecto** → pestaña **General** → baja hasta tu app Android.
2. Descarga **`google-services.json`**.
3. Dile a Claude dónde quedó (normalmente en `Descargas`). Claude lo pone en la app y la reinstala.

> El archivo no contiene contraseñas, pero no lo subas a un repositorio público.
