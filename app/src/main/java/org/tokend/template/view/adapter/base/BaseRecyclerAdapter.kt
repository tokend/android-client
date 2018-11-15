package org.tokend.template.view.adapter.base

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

abstract class BaseRecyclerAdapter<T, V> : RecyclerView.Adapter<V>() where V : BaseViewHolder<T> {
    companion object {
        protected const val VIEW_TYPE_ITEM = 0
    }

    protected val items = mutableListOf<T>()

    override fun onBindViewHolder(holder: V, position: Int) {
        val viewType = getItemViewType(position)
        when (viewType) {
            VIEW_TYPE_ITEM -> bindItemViewHolder(holder, position)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): V {
        return createItemViewHolder(parent)
    }

    protected abstract fun createItemViewHolder(parent: ViewGroup): V

    protected open fun bindItemViewHolder(holder: V, position: Int) {
        holder.bind(items[position], onItemClickListener)
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_ITEM
    }

    open fun setData(data: Collection<T>?) {
        val newItems = data?.toList() ?: listOf()
        val diffCallback = getDiffCallback(newItems)

        if (diffCallback == null) {
            items.clear()
            addData(data)
        } else {
            if (items.isEmpty() && newItems.isEmpty()) {
                notifyDataSetChanged()
            } else {
                val diffResult = DiffUtil.calculateDiff(diffCallback)
                items.clear()
                items.addAll(newItems)
                diffResult.dispatchUpdatesTo(this)
            }
        }
    }

    open fun addData(data: Collection<T>?) {
        if (data != null) {
            items.addAll(data)
        }
        notifyDataSetChanged()
    }

    open val hasData: Boolean
        get() = items.isNotEmpty()

    protected var onItemClickListener: SimpleItemClickListener<T>? = null

    open fun onItemClick(listener: SimpleItemClickListener<T>) {
        this.onItemClickListener = listener
    }

    open fun getItemAt(position: Int): T? {
        return if (getItemViewType(position) == VIEW_TYPE_ITEM)
            items.getOrNull(position)
        else null
    }

    protected open fun getDiffCallback(newItems: List<T>): DiffUtil.Callback? {
        return null
    }
}