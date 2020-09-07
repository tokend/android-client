package org.tokend.template.features.localaccount.view.util

import android.graphics.*
import androidx.collection.LruCache
import org.tokend.sdk.utils.extentions.encodeHexString
import org.tokend.wallet.utils.Hashing
import kotlin.math.abs

class PixelSquareLogoGenerator {
    private val colors = listOf(
            "#EDF2FE",
            "#538AF0",
            "#60BBF0",
            "#836CF1"
    )
            .map { Color.parseColor(it) }

    fun generate(seed: ByteArray, sizePx: Int): Bitmap {
        val cellSize = (sizePx.toFloat() / CELLS).toInt()

        val hash = Hashing.sha256(seed)
        val cacheKey = "${hash.encodeHexString()}_$sizePx"

        val cached = cache[cacheKey]
        if (cached != null) {
            return cached
        }

        val colorMatrix = (0 until CELLS / 2).map { rowIndex ->
            (0 until CELLS).map { cellIndex ->
                val value = hash[abs("$rowIndex$cellIndex".hashCode()) % hash.size].toInt()
                colors[abs(value) % colors.size]
            }
        }

        val sideBitmap = drawSide(colorMatrix, cellSize)
        val squareBitmap = drawMirrorSidesSquare(sideBitmap)
        sideBitmap.recycle()

        return squareBitmap
    }

    private fun drawSide(colorMatrix: List<List<Int>>,
                         cellSizePx: Int): Bitmap {
        val width = colorMatrix.size
        val height = colorMatrix.minBy(Collection<*>::size)?.size
                ?: throw IllegalArgumentException("Side rows can't be empty")

        val bitmap = Bitmap.createBitmap(
                width * cellSizePx,
                height * cellSizePx,
                Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val paint = Paint()

        (0 until width).forEach { x ->
            (0 until height).forEach { y ->
                val cellX = x * cellSizePx
                val cellY = y * cellSizePx

                paint.color = colorMatrix[x][y]
                canvas.drawRect(Rect(cellX, cellY, cellX + cellSizePx, cellY + cellSizePx), paint)
            }
        }

        return bitmap
    }

    private fun drawMirrorSidesSquare(sideBitmap: Bitmap): Bitmap {
        val bitmap = Bitmap.createBitmap(
                sideBitmap.width * 2,
                sideBitmap.height,
                Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        val mirroredSideMatrix = Matrix().apply {
            setScale(-1f, 1f)
            postTranslate(sideBitmap.width.toFloat() * 2, 0f)
        }

        canvas.drawBitmap(sideBitmap, Matrix(), null)
        canvas.drawBitmap(sideBitmap, mirroredSideMatrix, null)

        return bitmap
    }

    private companion object {
        // Must be even number.
        private const val CELLS = 6

        private val cache = LruCache<String, Bitmap>(25)
    }
}