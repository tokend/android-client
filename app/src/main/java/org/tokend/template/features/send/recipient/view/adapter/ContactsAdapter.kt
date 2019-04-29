package org.tokend.template.features.send.recipient.view.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.features.send.recipient.model.ContactEmail
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.adapter.base.SimpleItemClickListener

class ContactsAdapter : RecyclerView.Adapter<BaseViewHolder<Any>>() {

    private val items = mutableListOf<Any>()

    var onEmailClickListener: SimpleItemClickListener<Any>? = null

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: BaseViewHolder<Any>, position: Int) {
        when (getItemViewType(position)) {
            TYPE_CONTACT -> (holder as ContactViewHolder)
                    .bind(items[position] as ContactListItem, onContactClickListener)
            else -> (holder as EmailViewHolder)
                    .bind(items[position] as ContactEmail, onEmailClickListener)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Any> {
        val inflater = parent.context.layoutInflater
        return when (viewType) {
            TYPE_CONTACT -> ContactViewHolder(inflater.inflate(R.layout.item_contact, parent, false))
            else -> EmailViewHolder(inflater.inflate(R.layout.item_email, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ContactListItem -> TYPE_CONTACT
            else -> TYPE_EMAIL
        }
    }

    companion object {
        private const val TYPE_CONTACT = 131
        private const val TYPE_EMAIL = 141
    }

    fun addData(data: Collection<ContactListItem>?) {
        if (data != null) {
            items.addAll(data)
        }
        notifyDataSetChanged()
    }

    fun setData(data: Collection<ContactListItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    private val onContactClickListener = object : SimpleItemClickListener<Any> {
        override fun invoke(view: View?, item: Any) {
            item as ContactListItem
            val emails = item.emails
            if (items.containsAll(emails)) {
                notifyItemRangeRemoved(items.indexOf(emails[0]), emails.size)
                items.removeAll(emails)
            } else {
                val index = items.indexOf(item)
                if (index == items.size) {
                    items.addAll(emails)
                } else {
                    items.addAll(index + 1, emails)
                }
                notifyItemRangeInserted(index + 1, emails.size)
            }
        }
    }
}