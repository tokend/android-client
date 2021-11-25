package io.tokend.template.view.adapter.base

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * Base abstract [RecyclerView.Adapter] with single item type,
 * item model of type [ItemType], view holder of type [ViewHolderType],
 * click listener and optional [DiffUtil.Callback].
 */
abstract class BaseRecyclerAdapter<ItemType, ViewHolderType>
    : RecyclerView.Adapter<ViewHolderType>() where ViewHolderType : BaseViewHolder<ItemType> {
    companion object {
        protected const val VIEW_TYPE_ITEM = 0
    }

    protected val items = mutableListOf<ItemType>()

    override fun onBindViewHolder(holder: ViewHolderType, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_ITEM -> bindItemViewHolder(holder, position)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderType {
        return createItemViewHolder(parent)
    }

    protected abstract fun createItemViewHolder(parent: ViewGroup): ViewHolderType

    protected open fun bindItemViewHolder(holder: ViewHolderType, position: Int) {
        holder.bind(items[position], onItemClickListener)
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_ITEM
    }

    /**
     * Updates data of the adapter.
     * If [getDiffCallback] is implemented the update will be performed with [DiffUtil],
     * otherwise current data will be simple overwritten
     */
    open fun setData(data: Collection<ItemType>?) {
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

    /**
     * Appends given data to the current one
     */
    open fun addData(data: Collection<ItemType>?) {
        if (data != null) {
            items.addAll(data)
        }
        notifyDataSetChanged()
    }

    /**
     * Appends given items to the current data list
     */
    open fun addData(vararg items: ItemType) {
        addData(items.toList())
    }

    open fun clearData() {
        items.clear()
        notifyDataSetChanged()
    }

    /**
     * @returns true if adapter has data, false otherwise
     */
    open val hasData: Boolean
        get() = items.isNotEmpty()

    protected var onItemClickListener: SimpleItemClickListener<ItemType>? = null

    /**
     * Sets item click listener for the adapter
     */
    open fun onItemClick(listener: SimpleItemClickListener<ItemType>) {
        this.onItemClickListener = listener
    }

    /**
     * @return item on given position if it's exists, null otherwise
     */
    open fun getItemAt(position: Int): ItemType? {
        return if (getItemViewType(position) == VIEW_TYPE_ITEM)
            items.getOrNull(position)
        else null
    }

    /**
     * @return optional [DiffUtil.Callback] to be used in [setData]
     */
    protected open fun getDiffCallback(newItems: List<ItemType>): DiffUtil.Callback? {
        return null
    }
}