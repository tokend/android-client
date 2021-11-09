package io.tokend.template.view.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.ColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import io.tokend.template.util.imagetransform.RemoveAlphaTransform

class GlideDrawerImageLoader(
    private val context: Context,
    private val placeholder: Drawable?,
    @ColorInt
    private val backgroundColor: Int
) : DrawerImageLoader.IDrawerImageLoader {
    override fun placeholder(ctx: Context?): Drawable? {
        return placeholder
    }

    override fun placeholder(ctx: Context?, tag: String?): Drawable? {
        return placeholder
    }

    override fun set(imageView: ImageView?, uri: Uri?, placeholder: Drawable?) {
        imageView?.let {
            Glide
                .with(context)
                .load(uri)
                .placeholder(placeholder)
                .transform(RemoveAlphaTransform(backgroundColor), CircleCrop())
                .into(imageView)
        }
    }

    override fun set(imageView: ImageView?, uri: Uri?, placeholder: Drawable?, tag: String?) {
        imageView?.let {
            Glide
                .with(context)
                .load(uri)
                .placeholder(placeholder)
                .transform(RemoveAlphaTransform(backgroundColor), CircleCrop())
                .into(imageView)
        }
    }

    override fun cancel(imageView: ImageView?) {
        imageView?.let {
            Glide
                .with(context)
                .clear(imageView)
        }
    }
}