package com.airline.checkin.ui.boardingpass

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrCodeGenerator {

    private const val TAG = "QrCodeGenerator"

    suspend fun generateQrCodeBitmap(data: String, size: Int = 800): Bitmap? {
        return withContext(Dispatchers.Default) {
            try {
                val bitMatrix = MultiFormatWriter().encode(
                    data,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
                )

                val pixels = IntArray(size * size)
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        pixels[y * size + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    }
                }

                Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
            } catch (e: WriterException) {
                Log.e(TAG, "Error generating QR code: ${e.message}", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error generating QR code: ${e.message}", e)
                null
            }
        }
    }
}