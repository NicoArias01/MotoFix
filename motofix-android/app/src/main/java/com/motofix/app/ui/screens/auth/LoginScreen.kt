package com.motofix.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.motofix.app.util.findActivity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.MotoFixTextField
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.SecondaryButton
import com.motofix.app.ui.components.SystemFooter
import com.motofix.app.ui.components.rememberAppVersion
import com.motofix.app.ui.theme.Background
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.ErrorRed
import com.motofix.app.ui.theme.Mono
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.TextSecondary

/** Fondo cálido con brillo naranja arriba, como el taller del prototipo. */
val AuthBackground = Brush.verticalGradient(
    0f to Color(0xFF2B1709),
    0.35f to Color(0xFF14110F),
    1f to Background,
)

@Composable
private fun ButtonSpinner() {
    CircularProgressIndicator(Modifier.size(18.dp), color = Orange, strokeWidth = 2.dp)
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val state = viewModel.state
    val activity = LocalContext.current.findActivity()

    Box(
        Modifier
            .fillMaxSize()
            .background(AuthBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = "MOTOFIX",
                style = MaterialTheme.typography.displayLarge,
                color = Orange,
            )
            Text(
                text = "el arte de la mecánica sobre dos ruedas",
                fontFamily = Mono,
                fontSize = 13.sp,
                color = Cyan,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))

            MotoFixCard(Modifier.fillMaxWidth()) {
                MotoFixTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Correo electrónico",
                    placeholder = "tucorreo@dominio.com",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                    errorText = state.emailError,
                )
                Spacer(Modifier.height(16.dp))
                MotoFixTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Contraseña",
                    placeholder = "••••••••",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    errorText = state.passwordError,
                    imeAction = ImeAction.Done,
                    onImeAction = { viewModel.login(onLoginSuccess) },
                )
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = Cyan,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onForgotPassword)
                        .padding(vertical = 12.dp),
                )
                state.generalError?.let {
                    Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                PrimaryButton(
                    text = "Iniciar sesión",
                    onClick = { viewModel.login(onLoginSuccess) },
                    loading = state.loadingMethod == "email",
                    enabled = !state.isLoading,
                )
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton(
                        text = "Google",
                        onClick = { viewModel.loginWithGoogle(activity, onLoginSuccess) },
                        modifier = Modifier.weight(1f),
                        leading = {
                            if (state.loadingMethod == "google") ButtonSpinner()
                            else Text("G", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        },
                    )
                    SecondaryButton(
                        text = "GitHub",
                        onClick = { viewModel.loginWithGitHub(activity, onLoginSuccess) },
                        modifier = Modifier.weight(1f),
                        leading = {
                            if (state.loadingMethod == "github") ButtonSpinner()
                            else Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                    )
                }
                if (!viewModel.socialAvailable) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Google y GitHub se activan al conectar MotoFix con Firebase.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("¿No tienes una cuenta?", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier
                    .clickable(onClick = onCreateAccount)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("CREAR CUENTA", color = Orange, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Orange, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(24.dp))
            SystemFooter(left = "SYS_STATUS: READY", right = "CORE_VER: ${rememberAppVersion()}", modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}
