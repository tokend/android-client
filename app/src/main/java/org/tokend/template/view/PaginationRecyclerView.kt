package org.tokend.template.view

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet

/**
 * RecyclerView with callback for pagination.
 */
open class PaginationRecyclerView : RecyclerView {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) :
            super(context, attributeSet, style)

    constructor(context: Context) : super(context)

    protected var countProvider: (() -> Int)? = null
    protected var onBottomReachedListener: (() -> Boolean)? = null

    protected var paginationScrollListener: OnScrollListener? = null
    private var bottomReachHandled = false

    override fun setLayoutManager(layoutManager: LayoutManager?) {
        super.setLayoutManager(layoutManager)

        if (layoutManager is LinearLayoutManager) {
            enablePagination(layoutManager)
        }
    }

    private fun enablePagination(linearLayoutManager: LinearLayoutManager) {
        if (paginationScrollListener != null) {
            removeOnScrollListener(paginationScrollListener)
        }

        paginationScrollListener = getPaginationScrollListener(linearLayoutManager)
        addOnScrollListener(paginationScrollListener)
    }

    protected fun getPaginationScrollListener(linearLayoutManager: LinearLayoutManager)
            : OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            private var scrollingDown = true

            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
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