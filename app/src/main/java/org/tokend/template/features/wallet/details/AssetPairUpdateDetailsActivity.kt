package org.tokend.template.features.wallet.details

import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_details_list.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter

class AssetPairUpdateDetailsActivity : BalanceChangeDetailsActivity() {
    private val adapter = DetailsItemsAdapter()

    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details_list)

        val details = item.cause as? BalanceChangeCause.AssetPairUpdate

        if (details == null) {
            finish()
            return
        }

        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter

        displayEffect(item, adapter)
        displayBalanceChange(item, adapter)
        displayDate(item, adapter)
        displayAssetPairDetails(details)
    }

    private fun displayAssetPairDetails(
            details: BalanceChangeCause.AssetPairUpdate
    ) {
        adapter.addData(
                DetailsItem(
                        header = getString(R.string.asset_pair_title),
                        text = getString(
                                R.string.template_asset_pair,
                                details.baseAssetCode,
                                details.quoteAssetCode
                        ),
                        hint = getString(R.string.asset_pair_code),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_asset_pair)
                ),
                DetailsItem(
                        text = getString(
                                R.string.template_price_one_equals,
                                details.baseAssetCode,
                                amountFormatter.formatAssetAmount(
                                        details.physicalPrice,
                                        details.quoteAssetCode
                                )
                        ),
                        hint = getString(R.string.asset_pair_physical_price)
                ),
                DetailsItem(
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_policies),
                        text = getString(
                                if (details.isRestrictedByPhysicalPrice)
                                    R.string.asset_pair_restricted_by_physical_price
                                else
                                    R.string.asset_pair_not_restricted_by_physical_price
                        )
                ),
                DetailsItem(
                        text = getString(
                                if (details.isRestrictedByCurrentPrice)
                                    R.string.asset_pair_restricted_by_current_price
                                else
                                    R.string.asset_pair_not_restricted_by_current_price
                        )
                ),
                DetailsItem(
                        text = getString(
                                if (details.isTradeable)
                                    R.string.asset_pair_can_be_traded
                                else
                                    R.string.asset_pair_can_not_be_traded
                        )
                )
        )
    }
}