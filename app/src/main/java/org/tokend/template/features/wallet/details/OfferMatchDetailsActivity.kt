package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.OfferMatchDetails
import org.tokend.template.view.InfoCard
import org.tokend.template.view.util.formatter.AmountFormatter

class OfferMatchDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.offer_match_details_title)

        val details = item.details as? OfferMatchDetails

        if (details == null) {
            finish()
            return
        }

        displayPrice(details)
        displayCharged(details)
        displayFunded(details)
        displayDate(item, cards_layout)
    }

    private fun displayPrice(details: OfferMatchDetails) {
        val formattedPrice = amountFormatter
                .formatAssetAmount(details.price, details.quoteAssetCode)

        val priceString = getString(R.string.template_price_one_equals,
                details.baseAssetCode, formattedPrice)

        InfoCard(cards_layout)
                .setHeading(R.string.price, null)
                .addRow(priceString, null)
    }

    private fun displayCharged(details: OfferMatchDetails) {
        val charged = details.charged

        val chargedTotal =
                if (details.chargedInQuote)
                    charged.amount + charged.fee.total
                else
                    charged.amount

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

    private fun displayFunded(details: OfferMatchDetails) {
        val funded = details.funded

        val fundedTotal =
                if (details.fundedInQuote)
                    funded.amount - funded.fee.total
                else
                    funded.amount

        InfoCard(cards_layout)
                .setHeading(R.string.charged,
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