package org.tokend.template.view.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.squareup.picasso.Picasso

class PicassoDrawerImageLoader(private val context: Context,
                               private val placeholder: Drawable?): DrawerImageLoader.IDrawerImageLoader {
    override fun placeholder(ctx: Context?): Drawable? {
        return placeholder
    }

    override fun placeholder(ctx: Context?, tag: String?): Drawable? {
        return placeholder
    }

    override fun set(imageView: ImageView?, uri: Uri?, placeholder: Drawable?) {
        Picasso
                .with(context)
                .load(uri)
                .placeholder(placeholder)
                .into(imageView)
    }

    override fun set(imageView: ImageView?, uri: Uri?, placeholder: Drawable?, tag: String?) {
        Picasso
                .with(context)
                .load(uri)
                .placeholder(placeholder)
                .tag(tag)
                .into(imageView)
    }

    override fun cancel(imageView: ImageView?) {
        Picasso
                .with(context)
                .cancelRequest(imageView)
    }
}