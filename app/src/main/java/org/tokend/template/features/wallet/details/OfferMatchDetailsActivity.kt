package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.InfoCard

open class OfferMatchDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(getTitleString())

        val details = item.cause as? BalanceChangeCause.MatchedOffer

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

    protected open fun displayPrice(cause: BalanceChangeCause.MatchedOffer) {
        val formattedPrice = amountFormatter
                .formatAssetAmount(cause.price, cause.quoteAssetCode)

        val priceString = getString(R.string.template_price_one_equals,
                cause.baseAssetCode, formattedPrice)

        InfoCard(cards_layout)
                .setHeading(R.string.price, null)
                .addRow(priceString, null)
    }

    protected open fun displayCharged(cause: BalanceChangeCause.MatchedOffer) {
        val charged = cause.charged
        val chargedTotal = charged.amount + charged.fee.total
        val minDecimals = amountFormatter.getDecimalDigitsCount(charged.assetCode)

        InfoCard(cards_layout)
                .setHeading(R.string.charged,
                        amountFormatter.formatAssetAmount(chargedTotal, charged.assetCode, minDecimals))
                .addRow(R.string.amount,
                        "+" + amountFormatter.formatAssetAmount(charged.amount, charged.assetCode,
                                minDecimals))
                .addRow(R.string.fixed_fee,
                        "+" + amountFormatter.formatAssetAmount(charged.fee.fixed, charged.assetCode,
                                minDecimals))
                .addRow(R.string.percent_fee,
                        "+" + amountFormatter.formatAssetAmount(charged.fee.percent, charged.assetCode,
                                minDecimals))
    }

    protected open fun displayFunded(cause: BalanceChangeCause.MatchedOffer) {
        val funded = cause.funded
        val fundedTotal = funded.amount - funded.fee.total
        val minDecimals = amountFormatter.getDecimalDigitsCount(funded.assetCode)

        InfoCard(cards_layout)
                .setHeading(R.string.received,
                        amountFormatter.formatAssetAmount(fundedTotal, funded.assetCode, minDecimals))
                .addRow(R.string.amount,
                        "+" + amountFormatter.formatAssetAmount(funded.amount, funded.assetCode,
                                minDecimals))
                .addRow(R.string.fixed_fee,
                        "-" + amountFormatter.formatAssetAmount(funded.fee.fixed, funded.assetCode,
                                minDecimals))
                .addRow(R.string.percent_fee,
                        "-" + amountFormatter.formatAssetAmount(funded.fee.percent, funded.assetCode,
                                minDecimals))
    }
}