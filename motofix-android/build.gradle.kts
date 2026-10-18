plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// OneDrive bloquea el borrado de carpetas dentro de build/ y rompe la compilación,
// así que si el proyecto está en OneDrive los archivos de compilación van fuera.
if (rootDir.absolutePath.contains("OneDrive", ignoreCase = true)) {
    val outside = File(System.getProperty("user.home"), ".motofix-build")
    allprojects {
        layout.buildDirectory.set(File(outside, if (this == rootProject) "root" else name))
    }
}
