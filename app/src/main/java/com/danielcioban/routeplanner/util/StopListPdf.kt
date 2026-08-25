package com.danielcioban.routeplanner.util

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.danielcioban.routeplanner.RoutePlannerApplication
import com.danielcioban.routeplanner.data.local.RouteWithStops
import java.io.File

object StopListPdf {
    fun write(context: Context, route: RouteWithStops): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "stops-${route.route.id}.pdf")
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        var pageIndex = 1
        var page = doc.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create(),
        )
        var canvas = page.canvas
        val titlePaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            color = 0xFF222222.toInt()
        }
        val bodyPaint = Paint().apply {
            textSize = 11f
            color = 0xFF333333.toInt()
        }
        var y = margin
        canvas.drawText(route.route.name.ifBlank { "Route" }, margin, y, titlePaint)
        y += 22f
        val meta = listOf(route.route.vanName, route.route.shiftName)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (meta.isNotBlank()) {
            canvas.drawText(meta, margin, y, bodyPaint)
            y += 18f
        }
        val use24Hour = (context.applicationContext as? RoutePlannerApplication)
            ?.latestSettings?.clockFormat?.is24Hour(context) ?: true
        route.deliveryStops.forEachIndexed { index, stop ->
            if (y > pageHeight - 60f) {
                doc.finishPage(page)
                pageIndex += 1
                page = doc.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create(),
                )
                canvas = page.canvas
                y = margin
            }
            val status = when {
                stop.failureReason != null -> "FAIL"
                stop.isCompleted -> "DONE"
                else -> "OPEN"
            }
            val line = "${index + 1}. $status  ${stop.name}"
            canvas.drawText(line, margin, y, bodyPaint)
            y += 14f
            val extra = buildList {
                if (stop.addressHint.isNotBlank()) add(stop.addressHint.trim())
                if (stop.phone.isNotBlank()) add(stop.phone.trim())
                if (stop.codAmount > 0.0) {
                    add("COD ${"%.2f".format(stop.codAmount)}")
                }
                stop.arriveByMinutes?.let { add(GeoUtils.formatClockMinutes(it, use24Hour)) }
            }.joinToString(" · ")
            if (extra.isNotBlank()) {
                canvas.drawText(extra.take(90), margin + 12f, y, bodyPaint)
                y += 16f
            } else {
                y += 6f
            }
        }
        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
