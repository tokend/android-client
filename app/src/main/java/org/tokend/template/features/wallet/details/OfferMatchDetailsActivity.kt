package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeDetails
import org.tokend.template.view.InfoCard
import org.tokend.template.view.util.formatter.AmountFormatter

open class OfferMatchDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(getTitleString())

        val details = item.details as? BalanceChangeDetails.MatchedOffer

        if (details == null) {
            finish()
            return
        }

        displayPrice(details)
        displayCharged(details)
        displayFunded(details)
        displayDate(item, cards_layout)
    }

    protected open fun getTitleString(): String {
        return getString(R.string.offer_match_details_title)
    }

    protected open fun displayPrice(details: BalanceChangeDetails.MatchedOffer) {
        val formattedPrice = amountFormatter
                .formatAssetAmount(details.price, details.quoteAssetCode)

        val priceString = getString(R.string.template_price_one_equals,
                details.baseAssetCode, formattedPrice)

        InfoCard(cards_layout)
                .setHeading(R.string.price, null)
                .addRow(priceString, null)
    }

    protected open fun displayCharged(details: BalanceChangeDetails.MatchedOffer) {
        val charged = details.charged

        val chargedTotal = charged.amount + charged.fee.total

        InfoCard(cards_layout)
                .setHeading(R.string.charged,
                        amountFormatter.formatAssetAmount(chargedTotal, charged.assetCode))
                .addRow(R.string.amount,
                        "+" + amountFormatter.formatAssetAmount(charged.amount, charged.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
                .addRow(R.string.fixed_fee,
                        "+" + amountFormatter.formatAssetAmount(charged.fee.fixed, charged.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
                .addRow(R.string.percent_fee,
                        "+" + amountFormatter.formatAssetAmount(charged.fee.percent, charged.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
    }

    protected open fun displayFunded(details: BalanceChangeDetails.MatchedOffer) {
        val funded = details.funded

        val fundedTotal = funded.amount - funded.fee.total

        InfoCard(cards_layout)
                .setHeading(R.string.received,
                        amountFormatter.formatAssetAmount(fundedTotal, funded.assetCode))
                .addRow(R.string.amount,
                        "+" + amountFormatter.formatAssetAmount(funded.amount, funded.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
                .addRow(R.string.fixed_fee,
                        "-" + amountFormatter.formatAssetAmount(funded.fee.fixed, funded.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
                .addRow(R.string.percent_fee,
                        "-" + amountFormatter.formatAssetAmount(funded.fee.percent, funded.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
    }
}