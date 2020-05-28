package org.tokend.template.view.util

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScrollOnTopItemUpdateAdapterObserver(
    private val layoutManager: androidx.recyclerview.widget.LinearLayoutManager
) : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
    constructor(recyclerView: androidx.recyclerview.widget.RecyclerView) : this(
        ((recyclerView.layoutManager
            ?: throw IllegalArgumentException("RecyclerView has no LayoutManager"))
                as? androidx.recyclerview.widget.LinearLayoutManager)
            ?: throw IllegalArgumentException("Recycler view has non-linear LayoutManager ")
    )

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        super.onItemRangeMoved(fromPosition, toPosition, itemCount)
        if (toPosition == 0) {
            scrollToTopIfRequired()
        }
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        super.onItemRangeInserted(positionStart, itemCount)
        if (positionStart == 0) {
            scrollToTopIfRequired()
        }
    }

    private fun scrollToTopIfRequired() {
        if (layoutManager.findFirstVisibleItemPosition() == 0) {
            layoutManager.scrollToPosition(0)
        }
    }
}