package com.motofix.app.ui.screens.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.motofix.app.data.AudioDiagnosisRecord
import com.motofix.app.data.Severity
import com.motofix.app.data.WorkshopCatalog
import com.motofix.app.ui.components.MotoFixCard
import com.motofix.app.ui.components.PrimaryButton
import com.motofix.app.ui.components.ProgressBar
import com.motofix.app.ui.components.ScreenHeader
import com.motofix.app.ui.components.SecondaryButton
import com.motofix.app.ui.components.SectionLabel
import com.motofix.app.ui.components.StatusChip
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.ErrorRed
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceDark
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextMuted
import com.motofix.app.ui.theme.TextPrimary
import com.motofix.app.ui.theme.TextSecondary
import com.motofix.app.util.shareText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val Severity.color: Color
    get() = when (this) {
        Severity.OK -> Green
        Severity.LOW -> Cyan
        Severity.MEDIUM -> Orange
        Severity.CRITICAL -> ErrorRed
    }

private val dateTime = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-CO"))
fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTime).replace(".", "")

/** Reporte de un diagnóstico (prototipo "Resultado Audio IA"). */
@Composable
fun DiagnosisResultContent(
    record: AudioDiagnosisRecord,
    onBack: () -> Unit,
    onPractice: ((String) -> Unit)?,
    onRegisterRepair: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    onReplay: (() -> Unit)? = null,
    onNewDiagnosis: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val type = record.type
    val severityColor = type.severity.color
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val sessionId = "MX_AUDIO_" + record.id.take(4).uppercase()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = "Resultado Audio IA",
            onBack = onBack,
            action = { StatusChip(if (record.isDemo) "Ejemplo" else "⚡ Análisis vivo", if (record.isDemo) Cyan else Orange) },
        )

        // Onda grabada
        Box(
            Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0B1F26), SurfaceDark)))
        ) {
            WaveformCanvas(record.waveform, Modifier.fillMaxSize())
            StatusChip(
                "Match: ${record.confidence}%",
                Orange,
                filled = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
            if (onReplay != null) {
                Surface(
                    onClick = onReplay,
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                        Text(" ESCUCHAR", style = MaterialTheme.typography.labelSmall, color = Cyan)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Row {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Motor detectado", color = TextMuted)
                    Text(record.motoName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 2)
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    SectionLabel("ID de sesión", color = TextMuted)
                    Text(sessionId, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }
        }

        // Reporte
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (type.severity == Severity.OK) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = severityColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            SectionLabel(if (type.severity == Severity.OK) "Reporte del motor" else "Reporte de falla", color = severityColor)
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, severityColor.copy(alpha = 0.5f)),
        ) {
            Row {
                Column(Modifier.padding(14.dp)) {
                    Text(type.title.uppercase(), style = MaterialTheme.typography.titleLarge, color = severityColor)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(type.severity.label, severityColor, filled = true)
                        Spacer(Modifier.width(8.dp))
                        Text(formatDateTime(record.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "\"${type.explanation}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row {
                        SectionLabel("Confianza del análisis", modifier = Modifier.weight(1f))
                        Text("${record.confidence}%", style = MaterialTheme.typography.labelMedium, color = severityColor)
                    }
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(record.confidence / 100f, color = severityColor)
                }
            }
        }

        // Recomendación
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Build, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            SectionLabel("Recomendación técnica", color = Cyan)
        }
        Spacer(Modifier.height(8.dp))
        MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
            Text(type.recommendation, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Costo est.", color = TextMuted)
                    Text(type.estimatedCost, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    SectionLabel("Recompensa", color = TextMuted)
                    Text(
                        if (record.isDemo) "Sin XP (ejemplo)" else "+50 XP",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (record.isDemo) TextMuted else Green,
                    )
                }
            }
        }

        // Acciones
        Spacer(Modifier.height(16.dp))
        val practiceId = type.practiceScenarioId
        if (onRegisterRepair != null && record.motorcycleId != null && type.severity != Severity.OK) {
            PrimaryButton(
                "Registrar reparación",
                onClick = { onRegisterRepair(record.motorcycleId) },
                trailingIcon = Icons.Outlined.PostAdd,
            )
            Spacer(Modifier.height(10.dp))
        }
        if (onPractice != null && practiceId != null) {
            val scenario = WorkshopCatalog.find(practiceId)
            SecondaryButton(
                "Practicar en el taller: ${scenario?.title ?: ""}",
                onClick = { onPractice(practiceId) },
                modifier = Modifier.fillMaxWidth(),
                contentColor = Orange,
                borderColor = Orange.copy(alpha = 0.5f),
                uppercase = true,
                leading = { Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            Spacer(Modifier.height(10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                "Compartir",
                onClick = { shareText(context, "Diagnóstico MotoFix", reportText(record)) },
                modifier = Modifier.weight(1f),
                uppercase = true,
                leading = { Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            SecondaryButton(
                if (showDetails) "Ocultar detalles" else "Detalles técnicos",
                onClick = { showDetails = !showDetails },
                modifier = Modifier.weight(1f),
                uppercase = true,
                leading = { Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
        }

        AnimatedVisibility(visible = showDetails) {
            Column {
                Spacer(Modifier.height(12.dp))
                MotoFixCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    SectionLabel("Espectro de frecuencias (40 Hz – 10 kHz)")
                    Spacer(Modifier.height(8.dp))
                    SpectrumBars(record.spectrum, Modifier.fillMaxWidth().height(90.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Graves", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("Agudos", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Spacer(Modifier.height(12.dp))
                    DetailRow("Duración analizada", "%.1f s".format(record.durationSec))
                    DetailRow("Frecuencia dominante", "${record.dominantHz} Hz")
                    DetailRow("Nivel de la grabación", "${record.loudnessDb} dBFS")
                    DetailRow("Explosiones irregulares", "${record.irregularity}%")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Diagnóstico orientativo basado en el análisis acústico de la grabación. Depende de la calidad del audio y no reemplaza la revisión de un mecánico.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
        if (onNewDiagnosis != null) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Nuevo diagnóstico", onClick = onNewDiagnosis, trailingIcon = Icons.Outlined.GraphicEq)
        }
        if (onDelete != null) {
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                "Eliminar del historial",
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                contentColor = ErrorRed,
                borderColor = ErrorRed.copy(alpha = 0.4f),
                uppercase = true,
                leading = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
    }
}

private fun reportText(record: AudioDiagnosisRecord): String = buildString {
    appendLine("🔧 Reporte de diagnóstico MotoFix")
    appendLine("Moto: ${record.motoName}")
    appendLine("Fecha: ${formatDateTime(record.createdAtMillis)}")
    appendLine("Resultado: ${record.type.title} (${record.type.severity.label}) — confianza ${record.confidence}%")
    appendLine()
    appendLine(record.type.explanation)
    appendLine("Recomendación: ${record.type.recommendation}")
    appendLine("Costo estimado: ${record.type.estimatedCost}")
    appendLine()
    append("Diagnóstico orientativo, no reemplaza la revisión de un mecánico.")
}

/** Forma de onda en espejo, cian con brillo naranja en los picos. */
@Composable
fun WaveformCanvas(values: List<Float>, modifier: Modifier = Modifier, color: Color = Cyan) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val mid = size.height / 2
        val step = size.width / values.size
        val path = Path()
        val mirror = Path()
        values.forEachIndexed { i, v ->
            val x = i * step + step / 2
            val h = (v.coerceIn(0f, 1f) * (size.height * 0.42f)).coerceAtLeast(1.5f)
            if (i == 0) {
                path.moveTo(x, mid - h)
                mirror.moveTo(x, mid + h)
            } else {
                path.lineTo(x, mid - h)
                mirror.lineTo(x, mid + h)
            }
            drawLine(
                if (v > 0.85f) Orange.copy(alpha = 0.8f) else color.copy(alpha = 0.35f),
                Offset(x, mid - h),
                Offset(x, mid + h),
                strokeWidth = (step * 0.5f).coerceAtLeast(1f),
            )
        }
        drawPath(path, color, style = Stroke(2.dp.toPx()))
        drawPath(mirror, color.copy(alpha = 0.5f), style = Stroke(1.5.dp.toPx()))
        drawLine(color.copy(alpha = 0.2f), Offset(0f, mid), Offset(size.width, mid), 1f)
    }
}

@Composable
private fun SpectrumBars(values: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val h = (v * size.height).coerceAtLeast(2f)
            val color = when {
                i < values.size / 3 -> Orange
                i < values.size * 2 / 3 -> Cyan
                else -> Color(0xFFA78BFA)
            }
            drawRoundRect(
                color = SurfaceRaised,
                topLeft = Offset(i * (barWidth + gap), 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

/** Fila del historial de diagnósticos. */
@Composable
fun DiagnosisHistoryRow(record: AudioDiagnosisRecord, onClick: () -> Unit) {
    val color = record.type.severity.color
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.12f)),
            ) {
                Icon(
                    if (record.type.severity == Severity.OK) Icons.Outlined.TaskAlt else Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = color,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.type.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1)
                Text(
                    "${record.motoName} · ${formatDateTime(record.createdAtMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${record.confidence}%", style = MaterialTheme.typography.labelLarge, color = color)
                if (!record.isDemo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Green, modifier = Modifier.size(10.dp))
                        Text("50", style = MaterialTheme.typography.labelSmall, color = Green)
                    }
                }
            }
        }
    }
}
