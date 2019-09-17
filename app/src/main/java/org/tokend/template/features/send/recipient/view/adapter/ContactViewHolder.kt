package org.tokend.template.features.send.recipient.view.adapter

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.drawable.BitmapDrawable
import android.view.View
import kotlinx.android.synthetic.main.item_contact.view.*
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.features.assets.LogoFactory
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.adapter.base.SimpleItemClickListener
import org.tokend.template.view.util.ImageViewUtil

class ContactViewHolder(itemView: View) : BaseViewHolder<Any>(itemView) {
    private val photoSize =
            view.context.resources.getDimensionPixelSize(R.dimen.contact_photo_size)
    private val animationDuration =
            view.context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

    private val image = itemView.contact_icon_image_view
    private val name = itemView.contact_name
    private val arrow = itemView.expand_icon_image_view

    private val logoFactory = LogoFactory(itemView.context)

    override fun bind(item: Any, clickListener: SimpleItemClickListener<Any>?) {
        item as ContactListItem
        bind(item)
        view.onClick {
            clickListener?.invoke(view, item)
            animateArrow(item)
        }
    }

    override fun bind(item: Any) {
        item as ContactListItem

        val placeholder = BitmapDrawable(
                itemView.context.resources,
                logoFactory.getWithAutoBackground(item.name, photoSize, item.id)
        )

        ImageViewUtil.loadImageCircle(image, item.photoUri, placeholder)

        name.text = item.name

        arrow.rotation =
                if (item.isExpanded)
                    180f
                else
                    90f
    }

    private fun animateArrow(item: ContactListItem) {
        val rotation = when (item.isExpanded) {
            false -> PropertyValuesHolder.ofFloat(PROPERTY_ROTATION, 90f, 180f)
            else -> PropertyValuesHolder.ofFloat(PROPERTY_ROTATION, 180f, 90f)
        }

        val animator = ValueAnimator()
        animator.setValues(rotation)
        animator.duration = animationDuration
        animator.addUpdateListener { animation ->
            arrow.rotation = animation.getAnimatedValue(PROPERTY_ROTATION) as Float
        }
        animator.start()

        item.isExpanded = !item.isExpanded
    }

    companion object {
        private const val PROPERTY_ROTATION = "property_rotation"
    }
}