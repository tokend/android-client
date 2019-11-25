package org.tokend.template.features.send.recipient.contacts.view.adapter

import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.tokend.template.R
import org.tokend.template.view.util.LogoFactory
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.ImageViewUtil

class ContactMainItemViewHolder(view: View) : BaseViewHolder<ContactMainListItem>(view) {
    private val credentialTextView: TextView = view.findViewById(R.id.contact_credential_text_view)
    private val nameTextView: TextView = view.findViewById(R.id.contact_name_text_view)
    private val photoImageView: ImageView = view.findViewById(R.id.contact_photo_image_view)

    private val photoSize =
            view.context.resources.getDimensionPixelSize(R.dimen.contact_photo_size)
    private val logoFactory = LogoFactory(itemView.context)

    override fun bind(item: ContactMainListItem) {
        val placeholder = BitmapDrawable(
                itemView.context.resources,
                logoFactory.getWithAutoBackground(item.name, photoSize, item.id)
        )

        ImageViewUtil.loadImageCircle(photoImageView, item.photoUri, placeholder)

        credentialTextView.text = item.credential
        nameTextView.text = item.name
    }
}

class ContactExtraCredentialItemViewHolder(view: View) : BaseViewHolder<ContactExtraCredentialListItem>(view) {
    private val credentialTextView: TextView = view.findViewById(R.id.contact_credential_text_view)

    override fun bind(item: ContactExtraCredentialListItem) {
        credentialTextView.text = item.credential
    }
}

class ContactSectionTitleItemViewHolder(view: View) : BaseViewHolder<ContactSectionTitleListItem>(view) {
    private val sectionTitleTextView = view as TextView

    override fun bind(item: ContactSectionTitleListItem) {
        sectionTitleTextView.text = item.title
    }
}