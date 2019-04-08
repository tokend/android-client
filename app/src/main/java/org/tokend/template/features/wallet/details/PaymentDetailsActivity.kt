package org.tokend.template.features.wallet.details

import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_details_list.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter
import java.math.BigDecimal

class PaymentDetailsActivity : BalanceChangeDetailsActivity() {
    private val adapter = DetailsItemsAdapter()

    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details_list)

        val details = item.cause as? BalanceChangeCause.Payment

        if (details == null) {
            finish()
            return
        }

        val accountId = walletInfoProvider.getWalletInfo()?.accountId

        if (accountId == null) {
            finish()
            return
        }

        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter

        displayEffect(item, adapter)
        displayCounterparty(details, accountId)
        displayPaidOrReceived(item, details, accountId)
        displaySubject(details)
        displayDate(item, adapter)
    }

    private fun displayCounterparty(cause: BalanceChangeCause.Payment,
                                    accountId: String) {
        val counterpartyAccount = cause.getCounterpartyAccountId(accountId)
        val counterpartyName = cause.getCounterpartyName(accountId)

        val isReceived = cause.isReceived(accountId)

        val titleStringRes =
                if (isReceived)
                    R.string.tx_sender
                else
                    R.string.tx_recipient

        val counterpary =
                if (counterpartyName != null)
                    counterpartyName + "\n" + counterpartyAccount
                else
                    counterpartyAccount

        adapter.addData(
                DetailsItem(
                        text = counterpary,
                        hint = getString(titleStringRes),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_account)
                )
        )
    }

    private fun displayPaidOrReceived(item: BalanceChange,
                                      cause: BalanceChangeCause.Payment,
                                      accountId: String) {
        val isReceived = cause.isReceived(accountId)

        if (isReceived) {
            displayReceived(item, cause)
        } else {
            displayPaid(item, cause)
        }
    }

    private fun displayReceived(item: BalanceChange,
                                cause: BalanceChangeCause.Payment) {
        val minDecimals = amountFormatter.getDecimalDigitsCount(item.assetCode)

        val feePaidBySender = cause.isDestFeePaidBySource

        val paidFeeTotal =
                if (feePaidBySender)
                    BigDecimal.ZERO
                else
                    cause.destFee.total

        adapter.addData(
                DetailsItem(
                        text = amountFormatter.formatAssetAmount(item.amount, item.assetCode,
                                minDecimals),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        if (paidFeeTotal.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(paidFeeTotal, item.assetCode,
                                    minDecimals),
                            hint = getString(R.string.tx_fee)
                    )
            )
        }

        if (feePaidBySender && cause.destFee.total.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = getString(R.string.fee_paid_by_sender_explanation)
                    )
            )
        }
    }

    private fun displayPaid(item: BalanceChange,
                            cause: BalanceChangeCause.Payment) {
        val minDecimals = amountFormatter.getDecimalDigitsCount(item.assetCode)

        val feePaidBySender = cause.isDestFeePaidBySource

        val paidFeeTotal =
                if (feePaidBySender)
                    cause.sourceFee.total + cause.destFee.total
                else
                    cause.sourceFee.total

        adapter.addData(
                DetailsItem(
                        text = amountFormatter.formatAssetAmount(item.amount, item.assetCode,
                                minDecimals),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        if (paidFeeTotal.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(paidFeeTotal, item.assetCode,
                                    minDecimals),
                            hint = getString(R.string.tx_fee)
                    )
            )

            if (feePaidBySender) {
                adapter.addData(
                        DetailsItem(
                                text = getString(R.string.fee_paid_by_sender_explanation)
                        )
                )
            }
        }
    }

    private fun displaySubject(cause: BalanceChangeCause.Payment) {
        val subject = cause.subject?.takeIf { it.isNotBlank() }
                ?: return

        adapter.addData(
                DetailsItem(
                        text = subject,
                        hint = getString(R.string.payment_description),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_label_outline)
                )
        )
    }
}