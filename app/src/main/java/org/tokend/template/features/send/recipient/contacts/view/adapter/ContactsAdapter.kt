package org.tokend.template.features.send.recipient.contacts.view.adapter

import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseRecyclerAdapter
import org.tokend.template.view.adapter.base.BaseViewHolder

class ContactsAdapter : BaseRecyclerAdapter<ContactListItem, BaseViewHolder<ContactListItem>>() {
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ContactExtraCredentialListItem -> VIEW_TYPE_EXTRA_CREDENTIAL
            is ContactSectionTitleListItem -> VIEW_TYPE_SECTION_TITLE
            else -> super.getItemViewType(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): BaseViewHolder<ContactListItem> {
        return when (viewType) {
            VIEW_TYPE_EXTRA_CREDENTIAL -> createExtraCredentialViewHolder(parent)
            VIEW_TYPE_SECTION_TITLE -> createSectionTitleViewHolder(parent)
            else -> super.onCreateViewHolder(parent, viewType)
        } as BaseViewHolder<ContactListItem>
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ContactListItem>, position: Int) {
        bindItemViewHolder(holder, position)
    }

    override fun createItemViewHolder(parent: ViewGroup): BaseViewHolder<ContactListItem> {
        val view = parent.context.layoutInflater.inflate(R.layout.list_item_contact_main,
                parent, false)
        return ContactMainItemViewHolder(view) as BaseViewHolder<ContactListItem>
    }

    private fun createExtraCredentialViewHolder(parent: ViewGroup): ContactExtraCredentialItemViewHolder {
        val view = parent.context.layoutInflater.inflate(R.layout.list_item_contact_extra_credential,
                parent, false)
        return ContactExtraCredentialItemViewHolder(view)
    }

    private fun createSectionTitleViewHolder(parent: ViewGroup): ContactSectionTitleItemViewHolder {
        val view = parent.context.layoutInflater.inflate(R.layout.list_item_section_title,
                parent, false)
        return ContactSectionTitleItemViewHolder(view)
    }

    override fun getDiffCallback(newItems: List<ContactListItem>): DiffUtil.Callback? {
        return object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == newItems[newItemPosition]
            }

            override fun getOldListSize(): Int {
                return items.size
            }

            override fun getNewListSize(): Int {
                return newItems.size
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val first = items[oldItemPosition]
                val second = newItems[newItemPosition]

                return when {
                    first is ContactMainListItem && second is ContactMainListItem ->
                        first.name == second.name && first.photoUri == second.photoUri
                                && first.credential == second.credential
                    first is ContactExtraCredentialListItem && second is ContactExtraCredentialListItem ->
                        first.credential == second.credential
                    first is ContactSectionTitleListItem && second is ContactSectionTitleListItem ->
                        first.title == second.title
                    else -> false
                }
            }

        }
    }

    private companion object {
        private const val VIEW_TYPE_EXTRA_CREDENTIAL = 1
        private const val VIEW_TYPE_SECTION_TITLE = 2
    }
}