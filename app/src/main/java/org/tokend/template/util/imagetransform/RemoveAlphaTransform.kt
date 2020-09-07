package org.tokend.template.util.imagetransform

import android.graphics.*
import androidx.annotation.ColorInt
import com.squareup.picasso.Transformation

/**
 * Removes alpha channel, replaces it with given background color
 */
class RemoveAlphaTransform(
        @ColorInt
        private val backgroundColor: Int = Color.WHITE
) : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val bitmap = Bitmap.createBitmap(source.width, source.height, source.config)

        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = backgroundColor

        canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), paint)

        val overlayPaint = Paint()
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        overlayPaint.shader = shader
        overlayPaint.isAntiAlias = true

        canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), overlayPaint)

        source.recycle()

        return bitmap
    }

    override fun key(): String {
        return "remove_alpha"
    }
}