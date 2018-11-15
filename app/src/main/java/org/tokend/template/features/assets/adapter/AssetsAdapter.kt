package org.tokend.template.features.assets.adapter

import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseRecyclerAdapter

class AssetsAdapter : BaseRecyclerAdapter<AssetListItem, AssetListItemViewHolder>() {
    override fun createItemViewHolder(parent: ViewGroup): AssetListItemViewHolder {
        val view = parent.context.layoutInflater.inflate(R.layout.list_item_asset,
                parent, false)
        return AssetListItemViewHolder(view)
    }

    override fun getDiffCallback(newItems: List<AssetListItem>): DiffUtil.Callback? {
        return object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].code == newItems[newItemPosition].code
            }

            override fun getOldListSize(): Int {
                return items.size
            }

            override fun getNewListSize(): Int {
                return newItems.size
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val first = items[oldItemPosition]
                val second = newItems[newItemPosition]

                return first.name == second.name && first.balanceExists == second.balanceExists
            }

        }
    }
}