package org.tokend.template.features.polls.view.adapter

import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseRecyclerAdapter
import org.tokend.template.view.adapter.base.BaseViewHolder

class PollsAdapter : BaseRecyclerAdapter<PollListItem, BaseViewHolder<PollListItem>>() {
    private var onPollActionListener: PollActionListener? = null

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
}