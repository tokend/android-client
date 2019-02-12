package org.tokend.template.features.wallet.details

import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.PaymentDetails
import org.tokend.template.view.InfoCard
import org.tokend.template.view.util.formatter.AmountFormatter
import java.math.BigDecimal

class PaymentDetailsActivity : BalanceChangeDetailsActivity() {
    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.payment_details_title)

        val details = item.details as? PaymentDetails

        if (details == null) {
            finish()
            return
        }

        val accountId = walletInfoProvider.getWalletInfo()?.accountId

        if (accountId == null) {
            finish()
            return
        }

        displayCounterparty(details, accountId)
        displayPaidOrReceived(item, details, accountId)
        displaySubject(details)
        displayDate(item, cards_layout)
    }

    private fun displayCounterparty(details: PaymentDetails, accountId: String) {
        val counterpartyAccount = details.getCounterpartyAccountId(accountId)
        val counterpartyName = details.getCounterpartyName(accountId)

        val isReceived = details.isReceived(accountId)

        val titleStringRes =
                if (isReceived)
                    R.string.tx_sender
                else
                    R.string.tx_recipient

        InfoCard(cards_layout)
                .setHeading(titleStringRes, null)
                .apply {
                    if (counterpartyName != null) {
                        addRow(counterpartyName, null)
                    }
                }
                .addRow(counterpartyAccount, null)
    }

    private fun displayPaidOrReceived(item: BalanceChange, details: PaymentDetails,
                                      accountId: String) {
        val isReceived = details.isReceived(accountId)

        if (isReceived) {
            displayReceived(item, details)
        } else {
            displayPaid(item, details)
        }
    }

    private fun displayReceived(item: BalanceChange, details: PaymentDetails) {
        val feePaidBySender = details.isDestFeePaidBySource

        val paidFeePercent =
                if (feePaidBySender)
                    BigDecimal.ZERO
                else
                    details.destFee.percent

        val paidFeeFixed =
                if (feePaidBySender)
                    BigDecimal.ZERO
                else
                    details.destFee.fixed

        val receivedTotal = item.amount - paidFeeFixed - paidFeePercent

        InfoCard(cards_layout)
                .setHeading(R.string.received,
                        amountFormatter.formatAssetAmount(receivedTotal, item.assetCode))
                .addRow(R.string.amount,
                        "+" + amountFormatter.formatAssetAmount(item.amount, item.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
                .addRow(R.string.fixed_fee,
                        "-" + amountFormatter.formatAssetAmount(paidFeeFixed, item.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
                .addRow(R.string.percent_fee,
                        "-" + amountFormatter.formatAssetAmount(paidFeePercent, item.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
                .apply {
                    if (feePaidBySender) {
                        addRow(R.string.fee_paid_by_sender_explanation, null)
                    }
                }
    }

    private fun displayPaid(item: BalanceChange, details: PaymentDetails) {
        val feePaidBySender = details.isDestFeePaidBySource

        val paidFeePercent =
                if (feePaidBySender)
                    details.sourceFee.percent
                else
                    details.sourceFee.percent + details.destFee.percent

        val paidFeeFixed =
                if (feePaidBySender)
                    details.sourceFee.fixed
                else
                    details.sourceFee.fixed + details.destFee.fixed

        val paidTotal = item.amount + paidFeeFixed + paidFeePercent

        InfoCard(cards_layout)
                .setHeading(R.string.paid,
                        amountFormatter.formatAssetAmount(paidTotal, item.assetCode))
                .addRow(R.string.amount,
                        "+" + amountFormatter.formatAssetAmount(item.amount, item.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
                .addRow(R.string.fixed_fee,
                        "+" + amountFormatter.formatAssetAmount(paidFeeFixed, item.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
                .addRow(R.string.percent_fee,
                        "+" + amountFormatter.formatAssetAmount(paidFeePercent, item.assetCode,
                                minDecimalDigits = AmountFormatter.DEFAULT_ASSET_DECIMAL_DIGITS))
                .apply {
                    if (feePaidBySender) {
                        addRow(R.string.fee_paid_by_sender_explanation, null)
                    }
                }
    }

    private fun displaySubject(details: PaymentDetails) {
        val subject = details.subject?.takeIf { it.isNotBlank() }
                ?: return

        InfoCard(cards_layout)
                .setHeading(R.string.payment_description, null)
                .addRow(subject, null)
    }
}