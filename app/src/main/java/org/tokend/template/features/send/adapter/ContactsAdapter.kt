package org.tokend.template.features.send.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.adapter.base.SimpleItemClickListener
import org.tokend.template.features.send.model.Contact
import org.tokend.template.features.send.model.ContactEmail

class ContactsAdapter : RecyclerView.Adapter<BaseViewHolder<Any>>() {

    private val items = mutableListOf<Any>()

    var onEmailClickListener: SimpleItemClickListener<Any>? = null

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: BaseViewHolder<Any>, position: Int) {
        when(getItemViewType(position)) {
            TYPE_CONTACT -> (holder as ContactViewHolder)
                    .bind(items[position] as Contact, onContactClickListener)
            else -> (holder as EmailViewHolder)
                    .bind(items[position] as ContactEmail, onEmailClickListener)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Any> {
        val inflater = parent.context.layoutInflater
        return when(viewType) {
            TYPE_CONTACT -> ContactViewHolder(inflater.inflate(R.layout.item_contact, parent, false))
            else -> EmailViewHolder(inflater.inflate(R.layout.item_email, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(items[position]) {
            is Contact -> TYPE_CONTACT
            else -> TYPE_EMAIL
        }
    }

    companion object {
        private const val TYPE_CONTACT = 131
        private const val TYPE_EMAIL = 141
    }

    fun addData(data: Collection<Contact>?) {
        if (data != null) {
            items.addAll(data)
        }
        notifyDataSetChanged()
    }

    private val onContactClickListener = object : SimpleItemClickListener<Any> {
        override fun invoke(view: View?, item: Any) {
            item as Contact
            val emails = item.emails
            if(items.containsAll(emails)) {
                notifyItemRangeRemoved(items.indexOf(emails[0]), emails.size)
                items.removeAll(emails)
            } else {
                val index = items.indexOf(item)
                if(index == items.size) {
                    items.addAll(emails)
                } else {
                    items.addAll(index + 1, emails)
                }
                notifyItemRangeInserted(index + 1, emails.size)
            }
        }
    }
}