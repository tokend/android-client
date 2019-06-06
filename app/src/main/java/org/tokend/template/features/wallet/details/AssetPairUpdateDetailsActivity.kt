package org.tokend.template.features.wallet.details

import android.support.v4.content.ContextCompat
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.details.DetailsItem

class AssetPairUpdateDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        super.displayDetails(item)

        val details = item.cause as? BalanceChangeCause.AssetPairUpdate

        if (details == null) {
            finish()
            return
        }

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
                                details.baseAsset.code,
                                details.quoteAsset.code
                        ),
                        hint = getString(R.string.asset_pair_code),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_asset_pair)
                ),
                DetailsItem(
                        text = getString(
                                R.string.template_price_one_equals,
                                details.baseAsset,
                                amountFormatter.formatAssetAmount(
                                        details.physicalPrice,
                                        details.quoteAsset
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