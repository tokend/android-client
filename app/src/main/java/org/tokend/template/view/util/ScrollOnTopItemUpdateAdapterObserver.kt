package org.tokend.template.view.util

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

class ScrollOnTopItemUpdateAdapterObserver(
    private val layoutManager: LinearLayoutManager
) : RecyclerView.AdapterDataObserver() {
    constructor(recyclerView: RecyclerView) : this(
        ((recyclerView.layoutManager
            ?: throw IllegalArgumentException("RecyclerView has no LayoutManager"))
                as? LinearLayoutManager)
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