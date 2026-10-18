package com.motofix.app.ui.screens.audio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motofix.app.audio.DemoSound
import com.motofix.app.ui.components.ChoiceChips
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.ProgressBar
import com.motofix.app.ui.components.RewardDialog
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SecondaryButton
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.SystemFooter
import com.motofix.app.ui.theme.Background
import com.motofix.app.ui.theme.Border
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.ErrorRed
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary
import kotlin.math.ceil
import kotlin.math.sin

private const val OTHER_MOTO = "__otra__"

@Composable
fun AudioScreen(
    onOpenDiagnosis: (String) -> Unit,
    onPractice: (String) -> Unit,
    onRegisterRepair: (String) -> Unit,
    onGoToGarage: () -> Unit,
    viewModel: AudioViewModel = viewModel(),
) {
    val context = LocalContext.current
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var showDemoPicker by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            permissionDenied = false
            viewModel.startRecording()
        } else {
            permissionDenied = true
        }
    }
    val startRecording = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    BackHandler(enabled = viewModel.phase !is AudioPhase.Idle) {
        if (viewModel.phase is AudioPhase.Result) viewModel.newDiagnosis() else viewModel.cancel()
    }

    when (val phase = viewModel.phase) {
        AudioPhase.Idle -> AudioHome(
            viewModel = viewModel,
            permissionDenied = permissionDenied,
            onRecord = startRecording,
            onDemo = { showDemoPicker = true },
            onOpenDiagnosis = onOpenDiagnosis,
            onGoToGarage = onGoToGarage,
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            },
        )
        AudioPhase.Recording -> RecordingContent(
            levels = viewModel.liveLevels,
            progress = viewModel.recordProgress,
            onStop = viewModel::stopRecording,
            onCancel = viewModel::cancel,
        )
        is AudioPhase.Analyzing -> AnalyzingContent(phase.progress, phase.stage)
        is AudioPhase.Problem -> ProblemContent(phase.title, phase.advice, onRetry = startRecording, onBack = viewModel::newDiagnosis)
        is AudioPhase.Result -> DiagnosisResultContent(
            record = phase.record,
            onBack = viewModel::newDiagnosis,
            onPractice = onPractice,
            onRegisterRepair = onRegisterRepair,
            onReplay = if (viewModel.canReplay) viewModel::replay else null,
            onNewDiagnosis = viewModel::newDiagnosis,
        )
    }

    if (showDemoPicker) {
        DemoPickerDialog(
            onPick = {
                showDemoPicker = false
                viewModel.runDemo(it)
            },
            onDismiss = { showDemoPicker = false },
        )
    }
    viewModel.reward?.let { RewardDialog(it, onDismiss = viewModel::dismissReward) }
}

// ---------- Inicio ----------

@Composable
private fun AudioHome(
    viewModel: AudioViewModel,
    permissionDenied: Boolean,
    onRecord: () -> Unit,
    onDemo: () -> Unit,
    onOpenDiagnosis: (String) -> Unit,
    onGoToGarage: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val motos by viewModel.motorcycles.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(title = "Diagnóstico Audio IA", subtitle = "Diagnóstico avanzado", onBack = null)

        // Presentación
        Box(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1B1035), Color(0xFF0B2A33), SurfaceDark)))
                .border(1.dp, Cyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
        ) {
            AnimatedWave(Modifier.fillMaxSize())
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text("PODER DE LA IA", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(
                    "Detecta fallas mecánicas escuchando el motor de tu moto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        FeatureRow(Icons.Outlined.Hearing, "Análisis acústico", "Mide el ritmo de las explosiones, golpes, chasquidos y siseos del motor.")
        Spacer(Modifier.height(8.dp))
        FeatureRow(Icons.Outlined.Memory, "Conectado a tu garaje", "Cada diagnóstico queda guardado y puedes registrar la reparación en la moto.")

        // Moto a diagnosticar
        Spacer(Modifier.height(20.dp))
        SectionLabel("¿Qué moto vas a escuchar?")
        Spacer(Modifier.height(8.dp))
        if (motos.isEmpty()) {
            Surface(
                onClick = onGoToGarage,
                shape = RoundedCornerShape(8.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, Border),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TwoWheeler, contentDescription = null, tint = TextSecondary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "No tienes motos en el garaje. Agrega una para guardar el diagnóstico en su historial →",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        } else {
            ChoiceChips(
                options = motos.map { it.id } + OTHER_MOTO,
                selected = viewModel.selectedMotoId ?: OTHER_MOTO,
                onSelect = { viewModel.selectMoto(if (it == OTHER_MOTO) null else it) },
                label = { id -> motos.firstOrNull { it.id == id }?.displayName ?: "Otra moto" },
            )
        }

        // Consejos
        Spacer(Modifier.height(16.dp))
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 14.dp, borderColor = Orange.copy(alpha = 0.3f)) {
            SectionLabel("Para un buen diagnóstico", color = Orange)
            Spacer(Modifier.height(6.dp))
            listOf(
                "Motor encendido y caliente, en ralentí (sin acelerar).",
                "Celular a unos 30 cm del motor, con el micrófono hacia él.",
                "Lugar sin viento ni otros ruidos fuertes.",
                "Mantén el celular quieto durante los ${AudioViewModel.RECORD_SECONDS.toInt()} segundos.",
            ).forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(vertical = 2.dp))
            }
        }

        if (permissionDenied) {
            Spacer(Modifier.height(12.dp))
            MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 14.dp, borderColor = ErrorRed.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.MicOff, contentDescription = null, tint = ErrorRed)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "MotoFix necesita el micrófono para escuchar el motor. Puedes activarlo en los ajustes del teléfono.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                }
                TextButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = null, tint = Orange, modifier = Modifier.size(16.dp))
                    Text(" ABRIR AJUSTES", style = MaterialTheme.typography.labelMedium, color = Orange)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton("Grabar sonido del motor", onClick = onRecord, trailingIcon = Icons.Outlined.Mic)
        Spacer(Modifier.height(10.dp))
        SecondaryButton(
            "Probar con un sonido de ejemplo",
            onClick = onDemo,
            modifier = Modifier.fillMaxWidth(),
            contentColor = Cyan,
            borderColor = Cyan.copy(alpha = 0.5f),
            uppercase = true,
            leading = { Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )

        // Historial
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("HISTORIAL DE DIAGNÓSTICOS", style = MaterialTheme.typography.labelLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            SectionLabel("${history.size}", color = TextMuted)
        }
        Spacer(Modifier.height(10.dp))
        if (history.isEmpty()) {
            Text(
                "Aquí aparecerán tus diagnósticos. Las grabaciones reales te dan +50 XP.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                history.take(10).forEach { record ->
                    DiagnosisHistoryRow(record, onClick = { onOpenDiagnosis(record.id) })
                }
            }
        }
        SystemFooter(left = "AUDIO_NODE: READY", right = "SAMPLE_RATE: 44.1 KHZ", modifier = Modifier.padding(vertical = 20.dp))
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(36.dp).background(Orange))
        Spacer(Modifier.width(10.dp))
        Icon(icon, contentDescription = null, tint = Orange)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun AnimatedWave(modifier: Modifier = Modifier) {
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(2500), RepeatMode.Restart),
        label = "wave",
    )
    Canvas(modifier) {
        val mid = size.height * 0.35f
        listOf(Cyan to 1f, Orange to 0.6f, Color(0xFFA78BFA) to 0.4f).forEachIndexed { index, (color, amp) ->
            var prev: Offset? = null
            val points = 80
            for (i in 0..points) {
                val x = size.width * i / points
                val y = mid + sin(i * 0.25f + phase + index) * size.height * 0.18f * amp *
                    (0.5f + 0.5f * sin(i * 0.07f + phase * 0.5f))
                val p = Offset(x, y)
                prev?.let { drawLine(color.copy(alpha = 0.7f), it, p, 2.dp.toPx()) }
                prev = p
            }
        }
    }
}

// ---------- Grabando ----------

@Composable
private fun RecordingContent(levels: List<Float>, progress: Float, onStop: () -> Unit, onCancel: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
        1f, 1.15f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "mic",
    )
    val total = AudioViewModel.RECORD_SECONDS
    val secondsLeft = ceil(total * (1 - progress)).toInt().coerceAtLeast(0)
    val canStop = progress * total >= AudioViewModel.MIN_SECONDS_BEFORE_STOP
    val current = levels.lastOrNull() ?: 0f

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("ESCUCHANDO EL MOTOR", style = MaterialTheme.typography.labelLarge, color = Orange)
        Spacer(Modifier.height(24.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(Orange.copy(alpha = 0.12f))
                .border(2.dp, Orange, CircleShape),
        ) {
            Icon(Icons.Outlined.Mic, contentDescription = null, tint = Orange, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(28.dp))
        // Onda en vivo
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            val bars = 60
            val gap = 2.dp.toPx()
            val w = (size.width - gap * (bars - 1)) / bars
            val padded = List(bars - levels.size.coerceAtMost(bars)) { 0f } + levels.takeLast(bars)
            padded.forEachIndexed { i, level ->
                val h = (level.coerceIn(0f, 1f) * size.height).coerceAtLeast(2f)
                drawRoundRect(
                    color = if (level > 0.9f) ErrorRed else Cyan,
                    topLeft = Offset(i * (w + gap), (size.height - h) / 2),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(w / 2),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                current < 0.02f -> "No se escucha nada… acerca el celular al motor"
                current > 0.95f -> "Demasiado fuerte: aleja un poco el celular"
                else -> "Buena señal. Mantén el celular quieto."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (current < 0.02f || current > 0.95f) Orange else TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Text("${secondsLeft}s", style = MaterialTheme.typography.displayLarge, color = TextPrimary)
        ProgressBar(progress, color = Orange, modifier = Modifier.padding(vertical = 12.dp))
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            if (canStop) "Detener y analizar" else "Grabando…",
            onClick = onStop,
            enabled = canStop,
            trailingIcon = Icons.Outlined.Stop,
        )
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onCancel) {
            Text("CANCELAR", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        }
    }
}

// ---------- Analizando ----------

@Composable
private fun AnalyzingContent(progress: Float, stage: String) {
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Background, Color(0xFF0B1F26)))),
    ) {
        GridBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Orange.copy(alpha = 0.6f + 0.4f * glow)),
            ) {
                Icon(Icons.Outlined.Build, contentDescription = null, tint = Color.Black, modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(4.dp), color = Color.Transparent, border = BorderStroke(1.dp, Orange.copy(alpha = 0.5f))) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = Orange, modifier = Modifier.size(16.dp))
                    Text("  PERFORMANCE DIAGNOSTICS", style = MaterialTheme.typography.labelMedium, color = Orange)
                }
            }
            Spacer(Modifier.height(80.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("STATUS_LINK", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(stage, style = MaterialTheme.typography.labelLarge, color = Orange)
                }
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
            ProgressBar(progress, color = Orange, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun GridBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Cyan.copy(alpha = 0.07f)
        val horizon = size.height * 0.55f
        for (i in 0..12) {
            val y = horizon + (size.height - horizon) * (i / 12f) * (i / 12f)
            drawLine(c, Offset(0f, y), Offset(size.width, y), 1f)
        }
        for (i in -10..10) {
            drawLine(c, Offset(size.width / 2, horizon), Offset(size.width / 2 + i * size.width / 6, size.height), 1f)
        }
    }
}

// ---------- Problema con la grabación ----------

@Composable
private fun ProblemContent(title: String, advice: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Orange, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(title.uppercase(), style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(advice, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("No se guardó ningún diagnóstico.", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Grabar de nuevo", onClick = onRetry, trailingIcon = Icons.Outlined.Mic)
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Volver", onClick = onBack, modifier = Modifier.fillMaxWidth(), uppercase = true)
    }
}

// ---------- Sonidos de ejemplo ----------

@Composable
private fun DemoPickerDialog(onPick: (DemoSound) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("SONIDOS DE EJEMPLO", style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Sirven para conocer la función sin encender la moto. No dan XP ni cuentan para misiones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                DemoSound.entries.forEach { sound ->
                    Surface(
                        onClick = { onPick(sound) },
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceRaised,
                        border = BorderStroke(1.dp, Border),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(sound.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(sound.description, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CERRAR", color = TextSecondary) }
        },
    )
}

/** Detalle de un diagnóstico del historial. */
@Composable
fun DiagnosisDetailScreen(
    diagnosisId: String,
    onBack: () -> Unit,
    onPractice: (String) -> Unit,
    onRegisterRepair: (String) -> Unit,
) {
    val history by com.motofix.app.data.AppContainer.diagnoses.history
        .collectAsStateWithLifecycle(com.motofix.app.data.AppContainer.store.current.diagnoses)
    val record = history.firstOrNull { it.id == diagnosisId }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (record == null) {
        Column(Modifier.padding(16.dp)) {
            ScreenHeader(title = "Diagnóstico", onBack = onBack)
            Text("Este diagnóstico ya no existe.", color = TextSecondary)
        }
        return
    }
    DiagnosisResultContent(
        record = record,
        onBack = onBack,
        onPractice = onPractice,
        onRegisterRepair = onRegisterRepair,
        onDelete = { confirmDelete = true },
    )
    if (confirmDelete) {
        com.motofix.app.ui.components.ConfirmDialog(
            title = "Eliminar diagnóstico",
            message = "Se borrará este diagnóstico del historial.",
            confirmText = "Eliminar",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                com.motofix.app.data.AppContainer.diagnoses.delete(record.id)
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
