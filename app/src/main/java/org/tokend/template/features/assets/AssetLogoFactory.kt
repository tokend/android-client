package org.tokend.template.features.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.PaintCompat
import android.support.v4.util.LruCache
import org.tokend.template.R

/**
 * Creates fancy circle logos for assets.
 */
class AssetLogoFactory(private val context: Context) {
    /**
     * Returns [Bitmap] logo for given asset code by first letter.
     * If first letter cannot be displayed it will be replaced with emoj.
     */
    fun getForCode(assetCode: String,
                   size: Int,
                   @ColorInt
                   backgroundColor: Int = ContextCompat.getColor(context, R.color.accent),
                   @ColorInt
                   fontColor: Int = ContextCompat.getColor(context, R.color.white)
    ): Bitmap {
        val letter = assetCode.firstOrNull()?.toString()
        val key = "${letter}_$size"
        val cached = cache.get(key)
        return cached
                ?: generate(letter, size, backgroundColor, fontColor).also { cache.put(key, it) }
    }

    private fun generate(content: String?,
                         size: Int,
                         backgroundColor: Int,
                         fontColor: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        val paint = Paint()

        val center = size / 2f

        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        canvas.drawCircle(center, center, center, paint)

        paint.color = fontColor
        paint.style = Paint.Style.FILL
        paint.textSize = size * 0.65f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.SANS_SERIF

        content
                ?.takeIf {
                    it.isNotBlank() &&
                            PaintCompat.hasGlyph(paint, it)
                }
                .let {
                    it
                            ?: "💰".also { paint.textSize = size * 0.45f }
                }
                .let { toDraw ->
                    canvas.drawText(toDraw, center,
                            center - ((paint.descent() + paint.ascent()) / 2), paint)
                }

        return bitmap
    }

    private companion object {
        private val cache = LruCache<String, Bitmap>(25)
    }
}