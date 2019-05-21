package org.tokend.template.features.wallet.details

import android.support.v4.content.ContextCompat
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.details.DetailsItem

open class OfferMatchDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        val details = item.cause as? BalanceChangeCause.MatchedOffer

        if (details == null) {
            finish()
            return
        }

        displayEffect(item, adapter)
        displayPrice(details)
        displayDate(item, adapter)
        displayCharged(details)
        displayFunded(details)
    }

    protected open fun displayPrice(cause: BalanceChangeCause.MatchedOffer) {
        val formattedPrice = amountFormatter
                .formatAssetAmount(cause.price, cause.quoteAssetCode)

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

    protected open fun displayCharged(cause: BalanceChangeCause.MatchedOffer) {
        val charged = cause.charged

        adapter.addData(
                DetailsItem(
                        header = getString(R.string.charged),
                        text = amountFormatter.formatAssetAmount(charged.amount, charged.assetCode),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        if (charged.fee.total.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(charged.fee.total, charged.assetCode),
                            hint = getString(R.string.tx_fee)
                    ),
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(
                                    charged.amount + charged.fee.total,
                                    charged.assetCode
                            ),
                            hint = getString(R.string.total_label)
                    )
            )
        }
    }

    protected open fun displayFunded(cause: BalanceChangeCause.MatchedOffer) {
        val funded = cause.funded

        adapter.addData(
                DetailsItem(
                        header = getString(R.string.received),
                        text = amountFormatter.formatAssetAmount(funded.amount, funded.assetCode),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        if (funded.fee.total.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(funded.fee.total, funded.assetCode),
                            hint = getString(R.string.tx_fee)
                    ),
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(
                                    funded.amount - funded.fee.total,
                                    funded.assetCode
                            ),
                            hint = getString(R.string.total_label)
                    )
            )
        }
    }
}