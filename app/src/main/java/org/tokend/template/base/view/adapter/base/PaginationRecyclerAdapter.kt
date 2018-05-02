package org.tokend.template.base.view.adapter.base

import android.view.ViewGroup

abstract class PaginationRecyclerAdapter<T, V> : BaseRecyclerAdapter<T, V>()
        where V : BaseViewHolder<T> {
    companion object {
        protected const val VIEW_TYPE_LOADING_FOOTER = 1
    }

    protected var needLoadingFooter = false

    fun showLoadingFooter() {
        if (!needLoadingFooter) {
            needLoadingFooter = true
            notifyItemInserted(itemCount - 1)
        }
    }

    fun hideLoadingFooter() {
        if (needLoadingFooter) {
            val footerPosition = itemCount - 1
            needLoadingFooter = false
            notifyItemRemoved(footerPosition)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1 && needLoadingFooter)
            VIEW_TYPE_LOADING_FOOTER
        else
            super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): V {
        return when (viewType) {
            VIEW_TYPE_LOADING_FOOTER -> createFooterViewHolder(parent)
            else -> createItemViewHolder(parent)
        }
    }

    override fun onBindViewHolder(holder: V, position: Int) {
        val viewType = getItemViewType(position)
        when (viewType) {
            VIEW_TYPE_LOADING_FOOTER -> bindFooterViewHolder(holder)
            else -> super.onBindViewHolder(holder, position)
        }
    }

    override fun getItemCount(): Int {
        return getDataItemCount() + if (needLoadingFooter) 1 else 0
    }

    open fun getDataItemCount(): Int {
        return items.size
    }

    abstract fun createFooterViewHolder(parent: ViewGroup): V
    abstract fun bindFooterViewHolder(holder: V)
}