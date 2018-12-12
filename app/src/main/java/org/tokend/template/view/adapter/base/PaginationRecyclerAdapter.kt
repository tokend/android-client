package org.tokend.template.view.adapter.base

import android.view.ViewGroup

/**
 * Abstract [BaseRecyclerAdapter] with ability to show loading footer for pagination.
 */
abstract class PaginationRecyclerAdapter<ItemType, ViewHolderType>
    : BaseRecyclerAdapter<ItemType, ViewHolderType>()
        where ViewHolderType : BaseViewHolder<ItemType> {
    companion object {
        protected const val VIEW_TYPE_LOADING_FOOTER = 1
    }

    protected var needLoadingFooter = false

    /**
     * Shows the loading footer – a progress bar as a last item
     */
    fun showLoadingFooter() {
        if (!needLoadingFooter) {
            needLoadingFooter = true
            notifyItemInserted(itemCount - 1)
        }
    }

    /**
     * Hides loading footer
     */
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderType {
        return when (viewType) {
            VIEW_TYPE_LOADING_FOOTER -> createFooterViewHolder(parent)
            else -> createItemViewHolder(parent)
        }
    }

    override fun onBindViewHolder(holder: ViewHolderType, position: Int) {
        val viewType = getItemViewType(position)
        when (viewType) {
            VIEW_TYPE_LOADING_FOOTER -> bindFooterViewHolder(holder)
            else -> super.onBindViewHolder(holder, position)
        }
    }

    /**
     * @return visible items count including loading footer.
     * If you need real items count use [getDataItemCount]
     */
    override fun getItemCount(): Int {
        return getDataItemCount() + if (needLoadingFooter) 1 else 0
    }

    /**
     * @return real items count ignoring loading footer
     */
    open fun getDataItemCount(): Int {
        return items.size
    }

    abstract fun createFooterViewHolder(parent: ViewGroup): ViewHolderType
    abstract fun bindFooterViewHolder(holder: ViewHolderType)
}