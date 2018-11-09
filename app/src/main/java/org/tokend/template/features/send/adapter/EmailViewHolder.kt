package org.tokend.template.features.send.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_email.view.*
import org.tokend.template.base.view.adapter.base.BaseViewHolder
import org.tokend.template.features.send.ContactEmail

class EmailViewHolder(itemView: View) : BaseViewHolder<Any>(itemView) {

    override fun bind(item: Any) {
        item as ContactEmail
        itemView.email_text_view.text = item.email
    }
}