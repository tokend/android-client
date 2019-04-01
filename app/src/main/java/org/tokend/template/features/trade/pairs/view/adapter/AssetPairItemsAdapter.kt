package org.tokend.template.features.trade.pairs.view.adapter

import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseRecyclerAdapter
import org.tokend.template.view.util.formatter.AmountFormatter

class AssetPairItemsAdapter(
        private val amountFormatter: AmountFormatter
) : BaseRecyclerAdapter<AssetPairListItem, AssetPairItemViewHolder>() {
    override fun createItemViewHolder(parent: ViewGroup): AssetPairItemViewHolder {
        val view = parent.context.layoutInflater.inflate(R.layout.list_item_asset_pair, parent, false)
        return AssetPairItemViewHolder(view, amountFormatter)
    }

    override fun getDiffCallback(newItems: List<AssetPairListItem>): DiffUtil.Callback? {
        return object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].id == newItems[newItemPosition].id
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

                return first.baseAssetCode == second.baseAssetCode
                        && first.quoteAssetCode == second.quoteAssetCode
                        && first.price == second.price
                        && first.baseAssetLogoUrl == second.baseAssetLogoUrl
            }
        }
    }
}