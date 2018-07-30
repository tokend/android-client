package org.tokend.template.base.activities.qr

import android.content.Context
import android.graphics.Bitmap
import android.support.v4.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import io.reactivex.Observable
import org.tokend.template.R
import java.util.*

class QrGenerator(context: Context) {
    private val QR_CODE_WRITER = QRCodeWriter()

    private val ERROR_CORRECTION_LV = ErrorCorrectionLevel.H

    private val darkColor = ContextCompat.getColor(context, R.color.primary_text)
    private val lightColor = 0

    private fun generateQrBitmap(content: String, maxSize: Int): Bitmap {
        val code = Encoder.encode(content, ERROR_CORRECTION_LV, null)
        val baseSize = code.matrix.width

        // Make qr-code size multiples of baseSize
        val size = (Math.max(Math.floor((maxSize / baseSize).toDouble()), 1.0) * baseSize).toInt()

        val hints = Hashtable<EncodeHintType, Any>()
        hints.put(EncodeHintType.MARGIN, 0)
        hints.put(EncodeHintType.ERROR_CORRECTION, ERROR_CORRECTION_LV)
        val result = QR_CODE_WRITER.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)

        for (y in 0..height - 1) {
            val offset = y * width
            for (x in 0..width - 1) {
                pixels[offset + x] = if (result.get(x, y)) darkColor else lightColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
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
}