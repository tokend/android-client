package org.tokend.template.view

import android.content.Context
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextSwitcher
import org.tokend.template.R
import org.tokend.template.view.adapter.history.TxHistoryAdapter
import org.tokend.template.view.adapter.history.TxHistoryItem
import java.util.*

/**
 * TextSwitcher which acts like Telegram chat date cloud
 */
class TxDateTextSwitcher : TextSwitcher {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context) : super(context)

    private val HEADER_HIDE_TIMEOUT = 2000

    private var slideInFromBottom: Animation? = null
    private var slideOutToTop: Animation? = null
    private var slideInFromTop: Animation? = null
    private var slideOutToBottom: Animation? = null

    private var prevHeaderId = java.lang.Long.MAX_VALUE
    private var prevScrollState: Int = 0
    private var canHide: Boolean = false
    private var isVisible: Boolean = false
    private var lastTimeoutStartTime: Long = 0

    private var layoutManager: LinearLayoutManager? = null
    private var txAdapter: TxHistoryAdapter? = null

    init {
        setFactory {
            LayoutInflater.from(context)
                    .inflate(R.layout.layout_tx_date_text_switcher, null)
        }

        initAnimations()
    }

    private fun initAnimations() {
        slideInFromBottom = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom)
        slideOutToTop = AnimationUtils.loadAnimation(context, R.anim.slide_out_top)
        slideInFromTop = AnimationUtils.loadAnimation(context, R.anim.slide_in_top)
        slideOutToBottom = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom)
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        private var scrollState: Int = 0

        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            updateScrollState(scrollState)
            val firstVisibleItemIndex =
                    layoutManager?.findFirstCompletelyVisibleItemPosition() ?: return
            val firstVisibleItem = txAdapter?.getItemAt(firstVisibleItemIndex) ?: return
            updateFirstVisibleTx(firstVisibleItem)
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            scrollState = newState
            if (newState != RecyclerView.SCROLL_STATE_IDLE
                    && recyclerView!!.computeVerticalScrollOffset() == 0) {
                return
            }
            updateScrollState(scrollState)
        }
    }

    fun init(recyclerView: RecyclerView, adapter: TxHistoryAdapter) {
        prevHeaderId = java.lang.Long.MAX_VALUE
        prevScrollState = 0
        animateHide()

        recyclerView.removeOnScrollListener(scrollListener)
        recyclerView.addOnScrollListener(scrollListener)

        this.layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        this.txAdapter = adapter
    }

    fun updateFirstVisibleTx(tx: TxHistoryItem) {
        val currentDateId = getTxDateId(tx)
        if (currentDateId != prevHeaderId) {
            updateDateString(getTxDateString(tx), currentDateId.compareTo(prevHeaderId))
            prevHeaderId = currentDateId
        }
    }

    private fun getTxDateId(tx: TxHistoryItem): Long {
        val calendar = Calendar.getInstance()
        calendar.time = tx.date

        val sectionId = String.format("%d%02d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))

        return java.lang.Long.parseLong(sectionId)
    }

    private fun getTxDateString(tx: TxHistoryItem): String {
        val nowCalendar = Calendar.getInstance()
        val txCalendar = Calendar.getInstance()
        txCalendar.time = tx.date

        val withYear = nowCalendar.get(Calendar.YEAR) != txCalendar.get(Calendar.YEAR)
        val month = txCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)
        return month + if (withYear) ", " + txCalendar.get(Calendar.YEAR) else ""
    }

    private fun updateDateString(headerName: String, direction: Int) {
        post({
            when (direction) {
                -1 -> updateTextFromBottom(headerName)
                1 -> updateTextFromTop(headerName)
            }
        })
    }

    private fun updateTextFromBottom(headerName: String) {
        setBottomToTopAnimation()
        setText(headerName)
    }

    private fun updateTextFromTop(headerName: String) {
        setTopToBottomAnimation()
        setText(headerName)
    }

    private fun setBottomToTopAnimation() {
        inAnimation = slideInFromBottom
        outAnimation = slideOutToTop
    }

    private fun setTopToBottomAnimation() {
        inAnimation = slideInFromTop
        outAnimation = slideOutToBottom
    }

    fun updateScrollState(scrollState: Int) {
        if (scrollState != prevScrollState) {
            when (scrollState) {
                RecyclerView.SCROLL_STATE_IDLE -> {
                    canHide = true
                    setHideTimeout(Date().time)
                }
                else -> {
                    canHide = false
                    if (!isVisible) {
                        animateShow()
                    }
                }
            }
            prevScrollState = scrollState
        }
    }

    private fun setHideTimeout(startTime: Long) {
        lastTimeoutStartTime = startTime
        Handler().postDelayed({
            // Only the last running callback can hide the header.
            if (canHide && startTime == lastTimeoutStartTime) {
                animateHide()
            }
        }, HEADER_HIDE_TIMEOUT.toLong())
    }

    private fun animateShow() {
        if (isVisible) {
            return
        }

        visibility = View.VISIBLE
        alpha = 0f
        animate().alpha(1f).start()
        isVisible = true
    }

    private fun animateHide() {
        if (!isVisible) {
            return
        }

        alpha = 1f
        animate().alpha(0f).start()
        isVisible = false
    }
}