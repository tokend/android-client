package org.tokend.template.features.qr.logic

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import io.reactivex.Observable
import java.util.*

class QrGenerator {
    private val qrCodeWriter = QRCodeWriter()

    private val darkColor = Color.BLACK
    private val lightColor = Color.WHITE

    private fun generateQrBitmap(content: String, maxSize: Int): Bitmap {
        val code = Encoder.encode(content, ERROR_CORRECTION_LV, null)
        val baseSize = code.matrix.width

        // Make qr-code size multiples of baseSize
        val size = (Math.max(Math.floor((maxSize / baseSize).toDouble()), 1.0) * baseSize).toInt()

        val hints = Hashtable<EncodeHintType, Any>()
        hints[EncodeHintType.MARGIN] = 0
        hints[EncodeHintType.ERROR_CORRECTION] = ERROR_CORRECTION_LV
        val result = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (result.get(x, y)) darkColor else lightColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    fun bitmap(content: String, maxSize: Int): Observable<Bitmap> {
        return Observable.defer {
            try {
                Observable.just(generateQrBitmap(content, maxSize))
            } catch (e: Exception) {
                Observable.error<Bitmap>(e)
            }
        }
    }

    private companion object {
        private val ERROR_CORRECTION_LV = ErrorCorrectionLevel.H
    }
}