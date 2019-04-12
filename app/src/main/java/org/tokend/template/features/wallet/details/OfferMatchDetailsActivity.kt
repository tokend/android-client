package org.tokend.template.features.wallet.details

import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_details_list.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter

open class OfferMatchDetailsActivity : BalanceChangeDetailsActivity() {
    private val adapter = DetailsItemsAdapter()

    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details_list)

        val details = item.cause as? BalanceChangeCause.MatchedOffer

        if (details == null) {
            finish()
            return
        }

        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter

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
                    )
            )
        }
    }
}