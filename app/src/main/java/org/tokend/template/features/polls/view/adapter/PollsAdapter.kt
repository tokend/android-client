package org.tokend.template.features.polls.view.adapter

import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.adapter.base.PaginationRecyclerAdapter

class PollsAdapter : PaginationRecyclerAdapter<PollListItem, BaseViewHolder<PollListItem>>() {
    class FooterViewHolder(v: View) : BaseViewHolder<PollListItem>(v) {
        override fun bind(item: PollListItem) {}
    }

    protected var onPollActionListener: PollActionListener? = null

    /**
     * Sets poll ac click listener for the adapter
     *
     * @param listener receives current list item and selected choice
     * or null for choice removal
     */
    fun onPollAction(listener: PollActionListener) {
        this.onPollActionListener = listener
    }

    override fun createItemViewHolder(parent: ViewGroup): PollItemViewHolder {
        val view = parent.context.layoutInflater
                .inflate(R.layout.list_item_poll, parent, false)
        return PollItemViewHolder(view)
    }

    override fun bindItemViewHolder(holder: BaseViewHolder<PollListItem>, position: Int) {
        (holder as PollItemViewHolder).bindWithActionListener(items[position], onPollActionListener)
    }

    override fun createFooterViewHolder(parent: ViewGroup): BaseViewHolder<PollListItem> {
        val view = parent.context
                .layoutInflater.inflate(R.layout.list_item_loading_footer, parent, false)
        return FooterViewHolder(view)
    }

    override fun bindFooterViewHolder(holder: BaseViewHolder<PollListItem>) {}

//    override fun getDiffCallback(newItems: List<PollListItem>): DiffUtil.Callback? {
//        return object : DiffUtil.Callback() {
//            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//                return items[oldItemPosition] == newItems[newItemPosition]
//            }
//
//            override fun getOldListSize(): Int {
//                return items.size
//            }
//
//            override fun getNewListSize(): Int {
//                return newItems.size
//            }
//
//            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//                val new = newItems[newItemPosition]
//                val old = items[oldItemPosition]
//                return new.subject == old.subject
//                        && new.currentChoice == old.currentChoice
//                        && new.choices.size == old.choices.size
//                        &&
//                        (0 until new.choices.size).fold(true) { acc, i ->
//                            acc && new.choices[i] == old.choices[i]
//                        }
//            }
//        }
//    }
}