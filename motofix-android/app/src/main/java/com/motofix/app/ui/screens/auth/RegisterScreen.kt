package com.motofix.app.ui.screens.auth

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.data.BikeType
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.MotoFixTextField
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.icon
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.ErrorRed
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
) {
    val state = viewModel.state

    Box(
        Modifier
            .fillMaxSize()
            .background(AuthBackground)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("ÚNETE A MOTOFIX", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("Crea tu perfil y empieza a reparar", style = MaterialTheme.typography.bodyMedium, color = Cyan)
                }
            }

            MotoFixCard(Modifier.fillMaxWidth()) {
                MotoFixTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = "Nombre completo o apodo",
                    placeholder = "DaniRider_99",
                    leadingIcon = Icons.Outlined.Person,
                    errorText = state.nameError,
                )
                Spacer(Modifier.height(16.dp))
                MotoFixTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Correo electrónico",
                    placeholder = "rider@motofix.com",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                    errorText = state.emailError,
                )
                Spacer(Modifier.height(16.dp))
                MotoFixTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Contraseña",
                    placeholder = "Mínimo 6 caracteres",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    errorText = state.passwordError,
                )
                Spacer(Modifier.height(16.dp))
                MotoFixTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmChange,
                    label = "Confirmar contraseña",
                    placeholder = "Repite tu contraseña",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    errorText = state.confirmError,
                    imeAction = ImeAction.Done,
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = Border)
                SectionLabel("Tu máquina", Modifier.padding(horizontal = 12.dp))
                HorizontalDivider(Modifier.weight(1f), color = Border)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BikeType.registrationOptions.forEach { bike ->
                    BikeOption(
                        bike = bike,
                        selected = state.bike == bike,
                        onClick = { viewModel.onBikeSelected(bike) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.acceptedTerms,
                    onCheckedChange = viewModel::onTermsChange,
                    colors = CheckboxDefaults.colors(checkedColor = Orange, uncheckedColor = TextSecondary),
                )
                Text(
                    text = buildAnnotatedString {
                        append("Acepto los ")
                        withStyle(SpanStyle(color = Cyan)) { append("Términos y Condiciones") }
                        append(" y la ")
                        withStyle(SpanStyle(color = Cyan)) { append("Política de Privacidad") }
                        append(" de MOTOFIX.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            state.formError?.let {
                Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBack)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("¿Ya tienes una cuenta? ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Text("Iniciar sesión", color = Orange, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "Crear cuenta y empezar",
                onClick = { viewModel.register(onRegistered) },
                loading = state.isLoading,
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BikeOption(
    bike: BikeType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Orange else Border),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp),
        ) {
            Icon(bike.icon, contentDescription = null, tint = if (selected) Orange else TextSecondary, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(8.dp))
            Text(bike.label, color = if (selected) TextPrimary else TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
