package org.tokend.template.features.send.recipient.view.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_email.view.*
import org.tokend.template.features.send.recipient.model.ContactEmail
import org.tokend.template.view.adapter.base.BaseViewHolder

class EmailViewHolder(itemView: View) : BaseViewHolder<Any>(itemView) {

    override fun bind(item: Any) {
        item as ContactEmail
        itemView.email_text_view.text = item.email
    }
}