package org.tokend.template.features.assets

import android.content.Context
import android.graphics.*
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.PaintCompat
import android.support.v4.util.LruCache
import org.tokend.sdk.utils.HashCodes
import org.tokend.template.R

/**
 * Creates fancy circle logos for assets.
 */
class LogoFactory(private val context: Context) {
    private val colors = listOf(
            "#EF9A9A", "#F48FB1", "#CE93D8",
            "#FF8A80", "#FF80AB", "#90CAF9",
            "#B39DDB", "#9FA8DA", "#8C9EFF",
            "#80DEEA", "#80CBC4", "#82B1FF",
            "#A5D6A7", "#C5E1A5", "#DCE775",
            "#FBC02D", "#FFD54F", "#FFCC80",
            "#FFAB91", "#BCAAA4", "#BDBDBD",
            "#B0BEC5"
    )
            .map { Color.parseColor(it) }

    fun getWithAutoBackground(mainValue: String,
                              size: Int,
                              vararg values: Any? = emptyArray(),
                              @ColorInt
                              fontColor: Int = ContextCompat.getColor(context, R.color.white)
    ): Bitmap {
        val code = 70507 % (HashCodes.ofMany(mainValue, *values) and 0xffff)

        val background = colors[code % colors.size]
        return getForValue(mainValue, size, background, fontColor)
    }

    /**
     * Returns [Bitmap] logo for given asset code by first letter.
     * If first letter cannot be displayed it will be replaced with emoji.
     */
    fun getForValue(value: String,
                    size: Int,
                    @ColorInt
                   backgroundColor: Int,
                    @ColorInt
                   fontColor: Int
    ): Bitmap {
        val letter = value.firstOrNull()?.toString()
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