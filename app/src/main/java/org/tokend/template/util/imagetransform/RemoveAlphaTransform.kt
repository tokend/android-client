package org.tokend.template.util.imagetransform

import android.graphics.*
import androidx.annotation.ColorInt
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/**
 * Removes alpha channel, replaces it with given background color
 */
class RemoveAlphaTransform(
    @ColorInt
    private val backgroundColor: Int = Color.WHITE
) : BitmapTransformation() {

    override fun transform(pool: BitmapPool, source: Bitmap, width: Int, height: Int): Bitmap {
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

        return bitmap
    }

    override fun updateDiskCacheKey(digest: MessageDigest) {
        digest.update((ID + backgroundColor.toString()).toByteArray(Charsets.UTF_8))
    }

    override fun equals(other: Any?): Boolean {
        return other is RemoveAlphaTransform && other.backgroundColor == this.backgroundColor
    }

    override fun hashCode(): Int {
        return ID.hashCode() and backgroundColor.hashCode()
    }

    private companion object {
        const val ID = "org.tokend.template.util.imagetransform"
    }
}