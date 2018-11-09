package org.tokend.template.features.send.adapter

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.view.View
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_contact.view.*
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.base.view.adapter.base.BaseViewHolder
import org.tokend.template.base.view.adapter.base.SimpleItemClickListener
import org.tokend.template.features.explore.AssetLogoFactory
import org.tokend.template.features.send.Contact
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.graphics.drawable.RoundedBitmapDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap



class ContactViewHolder(itemView: View) : BaseViewHolder<Any>(itemView) {

    private val logoSize: Int by lazy {
        view.context.resources.getDimensionPixelSize(R.dimen.double_margin)
    }

    private var expand = false

    private val image = itemView.contact_icon_image_view
    private val name = itemView.contact_name
    private val arrow = itemView.expand_icon_image_view

    override fun bind(item: Any, clickListener: SimpleItemClickListener<Any>?) {
        bind(item)
        view.onClick {
            clickListener?.invoke(view, item)
            animateArrow()
        }
    }

    override fun bind(item: Any) {
        item as Contact
        if (item.photo_uri != null) {
            Picasso.with(view.context)
                    .load(item.photo_uri)
                    .placeholder(R.color.white)
                    .resize(logoSize, logoSize)
                    .centerInside()
                    .into(image, object : Callback {
                        override fun onSuccess() {
                            val imageBitmap = (image.drawable as BitmapDrawable).bitmap
                            val imageDrawable = RoundedBitmapDrawableFactory.create(view.resources, imageBitmap)
                            imageDrawable.isCircular = true
                            imageDrawable.cornerRadius = Math.min(imageBitmap.width, imageBitmap.height) / 2.0f
                            image.setImageDrawable(imageDrawable)
                        }
                        override fun onError() { }
                    })
        } else {
            image.setImageBitmap(AssetLogoFactory(view.context)
                    .getForCode(item.name, logoSize))
        }

        name.text = item.name
    }

    private fun animateArrow() {

        val rotation = when(expand) {
            false -> PropertyValuesHolder.ofFloat(PROPERTY_ROTATION, 90f, 180f)
            else -> PropertyValuesHolder.ofFloat(PROPERTY_ROTATION, 180f, 90f)
        }

        val animator = ValueAnimator()
        animator.setValues(rotation)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            arrow.rotation = animation.getAnimatedValue(PROPERTY_ROTATION) as Float
        }
        animator.start()
        expand = !expand
    }

    companion object {
        private const val PROPERTY_ROTATION = "property_rotation"
    }
}