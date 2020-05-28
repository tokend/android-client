package org.tokend.template.view

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet

/**
 * RecyclerView with callback for pagination.
 */
open class PaginationRecyclerView : androidx.recyclerview.widget.RecyclerView {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) :
            super(context, attributeSet, style)

    constructor(context: Context) : super(context)

    protected var countProvider: (() -> Int)? = null
    protected var onBottomReachedListener: (() -> Boolean)? = null

    private var paginationScrollListener: OnScrollListener? = null
    private var bottomReachHandled = false

    override fun setLayoutManager(layoutManager: LayoutManager?) {
        super.setLayoutManager(layoutManager)

        if (layoutManager is androidx.recyclerview.widget.LinearLayoutManager) {
            enablePagination(layoutManager)
        }
    }

    private fun enablePagination(linearLayoutManager: androidx.recyclerview.widget.LinearLayoutManager) {
        paginationScrollListener?.also { removeOnScrollListener(it) }
        paginationScrollListener = getPaginationScrollListener(linearLayoutManager)
                .also { addOnScrollListener(it) }
    }

    private fun getPaginationScrollListener(linearLayoutManager: androidx.recyclerview.widget.LinearLayoutManager)
            : OnScrollListener {
        return object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            private var scrollingDown = true

            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 2) {
                    scrollingDown = true
                } else if (dy < -2) {
                    scrollingDown = false
                }

                val visibleItemCount = linearLayoutManager.childCount
                val totalItemCount = countProvider?.invoke() ?: linearLayoutManager.itemCount

                val firstVisibleItemIndex = linearLayoutManager.findFirstVisibleItemPosition()

                if (scrollingDown) {
                    if ((visibleItemCount + firstVisibleItemIndex) >= (totalItemCount - 2)
                            && firstVisibleItemIndex >= 0) {
                        val listener = onBottomReachedListener
                        if (!bottomReachHandled && listener != null) {
                            bottomReachHandled = listener.invoke()
                        }
                    } else {
                        bottomReachHandled = false
                    }
                }
            }
        }
    }

    /**
     * Sets a listener for 'bottom reach' event.
     *
     * @param countProvider provides real items count for calculation,
     * e.g. if there are loading footers ignore them.
     * Total items count will be used if not provided
     * @param listener receives events,
     * must return true if event was handled for current scroll state and false otherwise.
     */
    fun listenBottomReach(countProvider: (() -> Int)? = null, listener: () -> Boolean) {
        resetBottomReachHandled()
        this.countProvider = countProvider
        this.onBottomReachedListener = listener
    }

    /**
     * Resets handling flag of 'bottom reach' event.
     * It's recommended to call this method on data source change
     */
    fun resetBottomReachHandled() {
        bottomReachHandled = false
    }
}