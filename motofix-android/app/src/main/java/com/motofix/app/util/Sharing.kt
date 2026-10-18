package com.motofix.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.motofix.app.data.Certificate
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val certDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"))

/** La Activity que contiene una pantalla de Compose (necesaria para Google y GitHub). */
tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("No se encontró la Activity")
}

/** Comparte un texto con cualquier app (WhatsApp, correo, notas...). */
fun shareText(context: Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir con"))
}

/** Genera el certificado en PDF (A4) y abre el menú para guardarlo o compartirlo. */
fun shareCertificatePdf(context: Context, certificate: Certificate) {
    val file = writeCertificatePdf(context, certificate)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Certificado técnico MotoFix #${certificate.id}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Guardar o compartir certificado"))
}

fun writeCertificatePdf(context: Context, cert: Certificate): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 48f
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
    val canvas = page.canvas

    val orange = Color.rgb(255, 106, 19)
    val dark = Color.rgb(14, 15, 17)
    val gray = Color.rgb(110, 116, 125)
    val green = Color.rgb(22, 163, 74)

    fun paint(size: Float, color: Int = dark, bold: Boolean = false, mono: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(if (mono) Typeface.MONOSPACE else Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    // Encabezado
    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, Paint().apply { color = dark })
    canvas.drawText("MOTOFIX", margin, 58f, paint(34f, orange, bold = true))
    canvas.drawText("el arte de la mecánica sobre dos ruedas", margin, 82f, paint(11f, Color.rgb(34, 211, 238), mono = true))
    val idPaint = paint(11f, Color.WHITE, mono = true).apply { textAlign = Paint.Align.RIGHT }
    canvas.drawText("REGISTRO OFICIAL", pageWidth - margin, 58f, idPaint)
    canvas.drawText("#${cert.id}", pageWidth - margin, 78f, idPaint)

    var y = 160f
    canvas.drawText("CERTIFICADO TÉCNICO DE REPARACIÓN", margin, y, paint(20f, dark, bold = true))
    y += 34f
    canvas.drawText("Se certifica que", margin, y, paint(12f, gray))
    y += 26f
    canvas.drawText(cert.mechanicName, margin, y, paint(22f, orange, bold = true))
    y += 26f
    canvas.drawText("completó con éxito la reparación \"${cert.title}\" (${cert.category})", margin, y, paint(12f, dark))
    y += 18f
    canvas.drawText("en el taller virtual de MotoFix sobre una ${cert.virtualMoto}.", margin, y, paint(12f, dark))

    // Datos
    y += 32f
    val box = RectF(margin, y, pageWidth - margin, y + 64f)
    canvas.drawRoundRect(box, 8f, 8f, Paint().apply { color = Color.rgb(244, 245, 247) })
    val cols = listOf(
        "FALLA" to cert.faultCode,
        "FECHA" to LocalDate.ofEpochDay(cert.dateEpochDay).format(certDate),
        "CALIFICACIÓN" to "${"★".repeat(cert.stars)}${"☆".repeat(3 - cert.stars)}",
    )
    val colWidth = (box.width()) / cols.size
    cols.forEachIndexed { i, (label, value) ->
        val x = box.left + 14f + i * colWidth
        canvas.drawText(label, x, box.top + 24f, paint(9f, gray, mono = true))
        val valuePaint = paint(11f, dark, bold = true)
        var text = value
        while (valuePaint.measureText(text) > colWidth - 20f && text.length > 4) text = text.dropLast(2) + "…"
        canvas.drawText(text, x, box.top + 46f, valuePaint)
    }
    y = box.bottom + 18f
    canvas.drawText(
        "Tiempo: %02d:%02d   ·   Errores: %d".format(cert.timeUsedSec / 60, cert.timeUsedSec % 60, cert.mistakes),
        margin, y, paint(10f, gray, mono = true),
    )

    // Pasos
    y += 34f
    canvas.drawText("DESGLOSE PASO A PASO", margin, y, paint(12f, dark, bold = true))
    y += 10f
    cert.steps.forEachIndexed { i, step ->
        y += 26f
        canvas.drawCircle(margin + 10f, y - 4f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = orange })
        canvas.drawText("${i + 1}", margin + 10f, y, paint(10f, Color.WHITE, bold = true).apply { textAlign = Paint.Align.CENTER })
        canvas.drawText(step.title, margin + 30f, y, paint(12f, dark, bold = true))
        y += 16f
        canvas.drawText("Herramienta: ${step.tool}", margin + 30f, y, paint(10f, gray, mono = true))
        y += 15f
        canvas.drawText(step.note, margin + 30f, y, paint(10f, dark))
    }

    // Verificación
    y = pageHeight - 150f
    canvas.drawLine(margin, y, pageWidth - margin, y, Paint().apply { color = Color.rgb(220, 222, 226) })
    y += 30f
    canvas.drawText("✔ VERIFICADO POR MOTOFIX", margin, y, paint(12f, green, bold = true))
    y += 20f
    canvas.drawText("Certificado emitido por el simulador de MotoFix. Sirve como respaldo del aprendizaje técnico", margin, y, paint(9f, gray))
    y += 14f
    canvas.drawText("del usuario; no reemplaza la certificación de un centro autorizado.", margin, y, paint(9f, gray))
    canvas.drawText("MOTOFIX ENGINE · ${cert.id}", margin, pageHeight - 40f, paint(8f, gray, mono = true))

    doc.finishPage(page)
    val dir = File(context.cacheDir, "certificados").apply { mkdirs() }
    val file = File(dir, "certificado-${cert.id}.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    return file
}
