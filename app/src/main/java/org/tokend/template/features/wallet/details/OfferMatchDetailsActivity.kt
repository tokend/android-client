package org.tokend.template.features.wallet.details

import android.support.v4.content.ContextCompat
import org.tokend.template.R
import org.tokend.template.data.model.SimpleAsset
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.details.DetailsItem

open class OfferMatchDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        super.displayDetails(item)

        val details = item.cause as? BalanceChangeCause.MatchedOffer

        if (details == null) {
            finish()
            return
        }

        displayOperationName(getString(R.string.offer_match))
        displayPrice(details)
        displayChargedOrFunded(item, details)
    }

    protected open fun displayPrice(cause: BalanceChangeCause.MatchedOffer) {
        val formattedPrice = amountFormatter
                .formatAssetAmount(cause.price, SimpleAsset(cause.quoteAssetCode))

        val priceString = getString(R.string.template_price_one_equals,
                cause.baseAssetCode, formattedPrice)

        adapter.addData(
                DetailsItem(
                        text = priceString,
                        hint = getString(R.string.price),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_price)
                )
        )
    }

    protected open fun displayChargedOrFunded(item: BalanceChange,
                                              cause: BalanceChangeCause.MatchedOffer) {
        val (total, fee, assetCode) =
                if (item.isReceived == true)
                    cause.charged.let {
                        Triple(it.amount + it.fee.total, it.fee, it.assetCode)
                    }
                else
                    cause.funded.let {
                        Triple(it.amount - it.fee.total, it.fee, it.assetCode)
                    }

        val asset = SimpleAsset(assetCode)

        adapter.addData(
                DetailsItem(
                        text = amountFormatter.formatAssetAmount(total, asset),
                        hint =
                        if (item.isReceived == true)
                            getString(R.string.charged)
                        else
                            getString(R.string.received),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        if (fee.total.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(fee.total, asset),
                            hint = getString(R.string.tx_fee)
                    )
            )
        }
    }
}