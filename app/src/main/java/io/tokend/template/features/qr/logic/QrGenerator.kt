package io.tokend.template.features.qr.logic

import android.graphics.*
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle

class QrGenerator {
    private val darkColor = Color.parseColor("#4A5050")
    private val lightColor = Color.WHITE

    private fun generateQrBitmap(content: String, maxSizePx: Int): Bitmap {
        val code = Encoder.encode(content, ERROR_CORRECTION_LV, null)
        val matrixSize = code.matrix.width
        val squareSizePx = maxSizePx.toFloat() / matrixSize

        val bitmap = Bitmap.createBitmap(maxSizePx, maxSizePx, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawRect(
            Rect(0, 0, maxSizePx, maxSizePx),
            Paint().apply { color = lightColor }
        )

        val squarePaint = Paint().apply { color = darkColor }
        val squareByte = 1.toByte()

        for (x in 0 until matrixSize) {
            for (y in 0 until matrixSize) {
                if (code.matrix[x, y] == squareByte) {
                    val xPixel = x * squareSizePx
                    val yPixel = y * squareSizePx
                    canvas.drawRect(
                        RectF(xPixel, yPixel, xPixel + squareSizePx, yPixel + squareSizePx),
                        squarePaint
                    )
                }
            }
        }

        return bitmap
    }

    fun bitmap(content: String, maxSize: Int): Single<Bitmap> {
        return Single.defer {
            try {
                generateQrBitmap(content, maxSize).toSingle()
            } catch (e: Exception) {
                Single.error(e)
            }
        }
    }

    private companion object {
        private val ERROR_CORRECTION_LV = ErrorCorrectionLevel.Q
    }
}