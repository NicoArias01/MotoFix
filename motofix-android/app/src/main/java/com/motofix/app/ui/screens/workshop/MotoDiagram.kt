package com.motofix.app.ui.screens.workshop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motofix.app.data.Part
import com.motofix.app.ui.theme.Cyan
import com.motofix.app.ui.theme.ErrorRed
import com.motofix.app.ui.theme.Green
import com.motofix.app.ui.theme.Orange
import com.motofix.app.ui.theme.SurfaceRaised
import com.motofix.app.ui.theme.TextPrimary
import kotlin.math.hypot

const val DIAGRAM_ASPECT = 0.80f

/**
 * Plano técnico de una moto (vista lateral) con los componentes tocables.
 * Se dibuja con formas simples para no depender de imágenes.
 */
@Composable
fun MotoDiagram(
    parts: List<Part>,
    onPartTap: (Part) -> Unit,
    modifier: Modifier = Modifier,
    hintPart: Part? = null,
    feedback: Feedback? = null,
    showLabels: Boolean = true,
    enabled: Boolean = true,
) {
    val textMeasurer = rememberTextMeasurer()
    val currentParts by rememberUpdatedState(parts)
    val currentOnTap by rememberUpdatedState(onPartTap)
    val currentEnabled by rememberUpdatedState(enabled)

    // Pulso de los puntos tocables
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "pulse",
    )

    // Destello verde/rojo en el componente tocado
    val flash = remember { Animatable(0f) }
    LaunchedEffect(feedback?.id) {
        if (feedback?.part != null && feedback.kind != FeedbackKind.INFO) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(700))
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / DIAGRAM_ASPECT)
            .semantics { contentDescription = "Diagrama de la moto con ${parts.size} componentes" }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    if (!currentEnabled) return@detectTapGestures
                    val w = size.width.toFloat()
                    val maxDistance = 26.dp.toPx()
                    currentParts
                        .map { it to hypot(tap.x - it.x * w, tap.y - it.y * w) }
                        .filter { it.second <= maxDistance }
                        .minByOrNull { it.second }
                        ?.let { currentOnTap(it.first) }
                }
            },
    ) {
        val w = size.width
        drawBlueprintGrid()
        drawMotorcycle()

        val labelStyle = TextStyle(color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        parts.forEach { part ->
            val center = Offset(part.x * w, part.y * w)
            val flashing = feedback?.part == part && flash.value > 0f
            val color = when {
                flashing && feedback?.kind == FeedbackKind.SUCCESS -> Green
                flashing && feedback?.kind == FeedbackKind.ERROR -> ErrorRed
                part == hintPart -> Cyan
                else -> Orange
            }
            val base = 7.dp.toPx()
            // Onda que se expande
            drawCircle(
                color = color.copy(alpha = (1f - pulse) * 0.5f),
                radius = base + pulse * 10.dp.toPx(),
                center = center,
                style = Stroke(1.5.dp.toPx()),
            )
            if (flashing) {
                drawCircle(color.copy(alpha = flash.value * 0.45f), radius = base * 2.6f, center = center)
            }
            drawCircle(Color.Black.copy(alpha = 0.6f), radius = base + 2.dp.toPx(), center = center)
            drawCircle(color, radius = base, center = center, style = Stroke(2.dp.toPx()))
            drawCircle(color, radius = base * 0.45f, center = center)

            if (showLabels) {
                val layout = textMeasurer.measure(part.shortLabel, labelStyle)
                val topLeft = Offset(
                    (center.x - layout.size.width / 2f).coerceIn(2f, w - layout.size.width - 2f),
                    center.y + base + 4.dp.toPx(),
                )
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.65f),
                    topLeft = topLeft - Offset(3.dp.toPx(), 1.dp.toPx()),
                    size = Size(layout.size.width + 6.dp.toPx(), layout.size.height + 2.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
                drawText(layout, topLeft = topLeft)
            }
        }
    }
}

private fun DrawScope.drawBlueprintGrid() {
    val step = size.width / 16f
    val grid = Cyan.copy(alpha = 0.06f)
    var x = 0f
    while (x <= size.width) {
        drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
}

/** Silueta de la moto. Todas las medidas son fracciones del ancho. */
private fun DrawScope.drawMotorcycle() {
    val w = size.width
    fun p(x: Float, y: Float) = Offset(x * w, y * w)
    val line = Cyan.copy(alpha = 0.55f)
    val thick = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    val thin = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    val fill = SurfaceRaised.copy(alpha = 0.9f)

    // Ruedas
    listOf(p(0.19f, 0.58f), p(0.81f, 0.58f)).forEach { c ->
        drawCircle(fill, radius = 0.13f * w, center = c)
        drawCircle(line, radius = 0.13f * w, center = c, style = thick)
        drawCircle(line.copy(alpha = 0.35f), radius = 0.10f * w, center = c, style = thin)
        drawCircle(line, radius = 0.025f * w, center = c, style = thin)
        // Radios
        for (i in 0 until 6) {
            val angle = Math.toRadians(i * 60.0)
            val end = c + Offset((kotlin.math.cos(angle) * 0.10f * w).toFloat(), (kotlin.math.sin(angle) * 0.10f * w).toFloat())
            drawLine(line.copy(alpha = 0.25f), c, end, 1.dp.toPx())
        }
    }
    // Disco de freno delantero
    drawCircle(line.copy(alpha = 0.6f), radius = 0.05f * w, center = p(0.81f, 0.58f), style = thin)

    // Basculante y cadena
    drawLine(line, p(0.19f, 0.58f), p(0.42f, 0.60f), 4.dp.toPx(), StrokeCap.Round)
    drawLine(line.copy(alpha = 0.7f), p(0.20f, 0.555f), p(0.44f, 0.575f), 1.5.dp.toPx())
    drawLine(line.copy(alpha = 0.7f), p(0.20f, 0.61f), p(0.44f, 0.625f), 1.5.dp.toPx())
    drawCircle(line, radius = 0.022f * w, center = p(0.44f, 0.60f), style = thin)

    // Suspensión trasera y tensor
    drawLine(line, p(0.30f, 0.58f), p(0.36f, 0.34f), 2.5.dp.toPx(), StrokeCap.Round)
    drawLine(line.copy(alpha = 0.6f), p(0.08f, 0.50f), p(0.17f, 0.56f), 1.5.dp.toPx())

    // Chasis
    val frame = Path().apply {
        moveTo(0.42f * w, 0.60f * w)
        lineTo(0.38f * w, 0.32f * w)
        lineTo(0.70f * w, 0.22f * w)
        lineTo(0.66f * w, 0.50f * w)
        close()
    }
    drawPath(frame, line, style = thick)

    // Motor
    drawRoundRect(fill, topLeft = p(0.40f, 0.42f), size = Size(0.28f * w, 0.24f * w), cornerRadius = CornerRadius(0.02f * w))
    drawRoundRect(line, topLeft = p(0.40f, 0.42f), size = Size(0.28f * w, 0.24f * w), cornerRadius = CornerRadius(0.02f * w), style = thick)
    // Culata
    drawRoundRect(fill, topLeft = p(0.47f, 0.36f), size = Size(0.14f * w, 0.08f * w), cornerRadius = CornerRadius(0.01f * w))
    drawRoundRect(line, topLeft = p(0.47f, 0.36f), size = Size(0.14f * w, 0.08f * w), cornerRadius = CornerRadius(0.01f * w), style = thin)
    for (i in 0..3) {
        val y = 0.375f + i * 0.017f
        drawLine(line.copy(alpha = 0.5f), p(0.48f, y), p(0.60f, y), 1.dp.toPx())
    }
    // Tapa lateral del motor
    drawCircle(line.copy(alpha = 0.7f), radius = 0.055f * w, center = p(0.55f, 0.56f), style = thin)

    // Escape
    val exhaust = Path().apply {
        moveTo(0.52f * w, 0.66f * w)
        quadraticTo(0.50f * w, 0.71f * w, 0.40f * w, 0.70f * w)
        lineTo(0.10f * w, 0.64f * w)
    }
    drawPath(exhaust, line.copy(alpha = 0.6f), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))

    // Tanque
    val tank = Path().apply {
        moveTo(0.44f * w, 0.30f * w)
        quadraticTo(0.50f * w, 0.18f * w, 0.66f * w, 0.22f * w)
        lineTo(0.66f * w, 0.31f * w)
        quadraticTo(0.56f * w, 0.35f * w, 0.44f * w, 0.33f * w)
        close()
    }
    drawPath(tank, fill)
    drawPath(tank, Orange.copy(alpha = 0.7f), style = thick)

    // Asiento y colín
    val seat = Path().apply {
        moveTo(0.44f * w, 0.30f * w)
        lineTo(0.22f * w, 0.27f * w)
        lineTo(0.06f * w, 0.25f * w)
        lineTo(0.10f * w, 0.32f * w)
        lineTo(0.38f * w, 0.34f * w)
        close()
    }
    drawPath(seat, fill)
    drawPath(seat, line, style = thin)
    drawCircle(ErrorRed.copy(alpha = 0.7f), radius = 0.012f * w, center = p(0.065f, 0.27f))

    // Horquilla, manubrio y farola
    drawLine(line, p(0.70f, 0.20f), p(0.81f, 0.58f), 4.dp.toPx(), StrokeCap.Round)
    drawLine(line, p(0.66f, 0.14f), p(0.73f, 0.19f), 3.dp.toPx(), StrokeCap.Round)
    drawCircle(fill, radius = 0.035f * w, center = p(0.80f, 0.27f))
    drawCircle(line, radius = 0.035f * w, center = p(0.80f, 0.27f), style = thin)
    drawLine(Color(0xFFFFF3B0).copy(alpha = 0.5f), p(0.84f, 0.26f), p(0.97f, 0.24f), 2.dp.toPx())
    drawLine(Color(0xFFFFF3B0).copy(alpha = 0.3f), p(0.84f, 0.29f), p(0.97f, 0.32f), 2.dp.toPx())

    // Carenado
    val fairing = Path().apply {
        moveTo(0.66f * w, 0.30f * w)
        lineTo(0.75f * w, 0.33f * w)
        lineTo(0.72f * w, 0.48f * w)
        lineTo(0.66f * w, 0.48f * w)
        close()
    }
    drawPath(fairing, line.copy(alpha = 0.45f), style = thin)

    // Batería y caja electrónica bajo el asiento
    drawRoundRect(line.copy(alpha = 0.5f), topLeft = p(0.26f, 0.40f), size = Size(0.08f * w, 0.07f * w), style = thin)
    drawRoundRect(line.copy(alpha = 0.5f), topLeft = p(0.16f, 0.33f), size = Size(0.08f * w, 0.05f * w), style = thin)
}
