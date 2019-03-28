package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.InfoCard

class AssetPairUpdateDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.asset_pair_update_details_title)

        val details = item.cause as? BalanceChangeCause.AssetPairUpdate

        if (details == null) {
            finish()
            return
        }

        displayEffect(item, cards_layout)
        displayBalanceChange(item, cards_layout)
        displayAssetPairDetails(details)
        displayDate(item, cards_layout)
    }

    private fun displayAssetPairDetails(
            details: BalanceChangeCause.AssetPairUpdate
    ) {
        InfoCard(cards_layout)
                .setHeading(R.string.asset_pair_title, null)
                .addRow(R.string.asset_pair_code,
                        getString(
                                R.string.template_asset_pair,
                                details.baseAssetCode,
                                details.quoteAssetCode
                        )
                )
                .addRow(
                        R.string.asset_pair_physical_price,
                        getString(
                                R.string.template_price_one_equals,
                                details.baseAssetCode,
                                amountFormatter.formatAssetAmount(
                                        details.physicalPrice,
                                        details.quoteAssetCode
                                )
                        )
                )
                .addRow("", null)
                .addRow(
                        if (details.isRestrictedByPhysicalPrice)
                            R.string.asset_pair_restricted_by_physical_price
                        else
                            R.string.asset_pair_not_restricted_by_physical_price,
                        null
                )
                .addRow(
                        if (details.isRestrictedByCurrentPrice)
                            R.string.asset_pair_restricted_by_current_price
                        else
                            R.string.asset_pair_not_restricted_by_current_price,
                        null
                )
                .addRow(
                        if (details.isTradeable)
                            R.string.asset_pair_can_be_traded
                        else
                            R.string.asset_pair_can_not_be_traded,
                        null
                )

    }
}