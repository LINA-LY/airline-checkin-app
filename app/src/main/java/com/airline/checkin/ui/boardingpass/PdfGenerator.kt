package com.airline.checkin.ui.boardingpass

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.airline.checkin.domain.model.Booking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {
    suspend fun generateAndSavePdf(
        context: Context,
        booking: Booking,
        qrCodeBitmap: Bitmap?
    ): String? = withContext(Dispatchers.IO) {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            // Make the canvas much taller (1400) so the QR code never cuts off
            val pageWidth = 800
            val pageHeight = 1400
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // --- Setup Paints ---
            val backgroundPaint = Paint().apply { color = Color.parseColor("#F3F4F6"); style = Paint.Style.FILL } // Light gray
            val cardPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }
            val headerPaint = Paint().apply { color = Color.parseColor("#8B5CF6"); style = Paint.Style.FILL; isAntiAlias = true }

            val whiteTitlePaint = Paint().apply { color = Color.WHITE; textSize = 36f; isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.CENTER }
            val labelPaint = Paint().apply { color = Color.parseColor("#9CA3AF"); textSize = 20f; isAntiAlias = true }
            val valuePaint = Paint().apply { color = Color.BLACK; textSize = 28f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
            val hugeRoutePaint = Paint().apply { color = Color.BLACK; textSize = 70f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
            val planePaint = Paint().apply { color = Color.parseColor("#8B5CF6"); textSize = 60f; isAntiAlias = true; textAlign = Paint.Align.CENTER }

            val dashPaint = Paint().apply {
                color = Color.parseColor("#E5E7EB")
                style = Paint.Style.STROKE
                strokeWidth = 5f
                pathEffect = DashPathEffect(floatArrayOf(25f, 25f), 0f)
            }

            // --- Draw Background ---
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), backgroundPaint)

            // --- Draw White Ticket Card (Rounded) ---
            canvas.drawRoundRect(40f, 40f, 760f, 1360f, 40f, 40f, cardPaint)

            // --- Draw Purple Header ---
            canvas.drawRoundRect(40f, 40f, 760f, 200f, 40f, 40f, headerPaint)
            canvas.drawRect(40f, 100f, 760f, 200f, headerPaint) // Fill bottom corners to make it flat at the bottom
            canvas.drawText("BOARDING PASS", 400f, 135f, whiteTitlePaint)

            // --- Draw Flight Route (Origin -> Destination) ---
            canvas.drawText(booking.departure.ifEmpty { "—" }, 100f, 320f, hugeRoutePaint)
            canvas.drawText("✈", 400f, 305f, planePaint) // Airplane symbol centered
            canvas.drawText(booking.destination.ifEmpty { "—" }, 540f, 320f, hugeRoutePaint)

            // --- Draw Grid Details ---
            var currentY = 460f

            // Row 1: Passenger
            canvas.drawText("Traveler Name", 100f, currentY, labelPaint)
            currentY += 40f
            val travelerName = listOf(booking.firstName, booking.lastName).joinToString(" ").trim()
            canvas.drawText(travelerName.ifEmpty { "—" }, 100f, currentY, valuePaint)

            currentY += 80f
            // Row 2: Date & Class
            canvas.drawText("Date", 100f, currentY, labelPaint)
            canvas.drawText("Class", 450f, currentY, labelPaint)
            currentY += 40f
            // Try formatting boarding time to get the date portion.
            val displayDate = try {
                if (booking.departureTime.isNotBlank()) {
                    val instant = java.time.Instant.parse(booking.departureTime.let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it })
                    val localDate = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                    localDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
                } else "—"
            } catch (e: Exception) {
                "—"
            }
            canvas.drawText(displayDate, 100f, currentY, valuePaint)
            canvas.drawText("Economy", 450f, currentY, valuePaint)

            currentY += 80f
            // Row 3: Departure & Arrival
            canvas.drawText("Departure", 100f, currentY, labelPaint)
            canvas.drawText("Arrival", 450f, currentY, labelPaint)
            currentY += 40f
            val displayTime = try {
                if (booking.departureTime.isNotBlank()) {
                    val instant = java.time.Instant.parse(booking.departureTime.let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it })
                    val localDate = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                    localDate.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                } else "—"
            } catch (e: Exception) {
                "—"
            }
            canvas.drawText(displayTime, 100f, currentY, valuePaint)
            canvas.drawText("TBD", 450f, currentY, valuePaint)

            currentY += 80f
            // Row 4: Flight, Gate, Seat
            canvas.drawText("Flight no", 100f, currentY, labelPaint)
            canvas.drawText("Gate", 350f, currentY, labelPaint)
            canvas.drawText("Seat", 550f, currentY, labelPaint)
            currentY += 40f
            canvas.drawText(booking.flightNumber.ifEmpty { "N/A" }, 100f, currentY, valuePaint)
            canvas.drawText("TBD", 350f, currentY, valuePaint)
            canvas.drawText(booking.seat?.seatNumber?.ifEmpty { "N/A" } ?: "N/A", 550f, currentY, valuePaint)

            // --- Draw Dashed Divider ---
            currentY += 100f
            canvas.drawLine(80f, currentY, 720f, currentY, dashPaint)

            // --- Draw Large Centered QR Code ---
            currentY += 60f
            qrCodeBitmap?.let {
                val qrSize = 340 // Massive QR code
                val scaledQr = Bitmap.createScaledBitmap(it, qrSize, qrSize, false)
                val leftCenter = (800f - qrSize) / 2f
                canvas.drawBitmap(scaledQr, leftCenter, currentY, null)
            }

            pdfDocument.finishPage(page)

            // --- Save Logic ---
            val fileName = "BoardingPass_${booking.id}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    return@withContext uri.toString()
                }
            } else {
                val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!directory.exists()) directory.mkdirs()
                val file = File(directory, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                return@withContext file.absolutePath
            }
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            pdfDocument?.close()
        }
    }
}