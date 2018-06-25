package org.tokend.template.base.activities.tx_details

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.models.transactions.PaymentTransaction
import org.tokend.template.R
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import java.math.BigDecimal

class PaymentDetailsActivity : TxDetailsActivity<PaymentTransaction>(PaymentTransaction::class) {
    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.payment_details_title)
    }

    override fun displayDetails(item: PaymentTransaction) {
        displayStateIfNeeded(item, cards_layout)
        displayReceiverSender(item)
        displayAmount(item)
        if (!item.subject.isNullOrBlank()) {
            displaySubject(item)
        }
        displayDate(item, cards_layout)
    }

    private fun displayReceiverSender(tx: PaymentTransaction) {
        val receiverSenderCard = InfoCard(cards_layout)
                .setHeading(if (tx.isSent) R.string.tx_recipient else R.string.tx_sender, null)

        if (tx.counterpartyNickname != null) {
            receiverSenderCard.addRow(tx.counterpartyNickname, null)
        }

        receiverSenderCard.addRow(if (tx.isSent) tx.destAccount else tx.sourceAccount, null)
    }

    private fun displayAmount(tx: PaymentTransaction) {
        if (tx.isSent) {
            val paidAmount = getPaidAmount(tx)
            var fixedFee = tx.senderFee.fixed
            var percentFee = tx.senderFee.percent

            if (tx.feePaidBySender) {
                fixedFee += tx.recipientFee.fixed
                percentFee += tx.recipientFee.percent
            }

            InfoCard(cards_layout)
                    .setHeading(R.string.paid,
                            "${AmountFormatter.formatAssetAmount(paidAmount)} ${tx.asset}")
                    .addRow(R.string.amount_sent,
                            "+${AmountFormatter.formatAssetAmount(tx.amount,
                                    minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                            } ${tx.asset}")
                    .addRow(R.string.tx_fixed_fee,
                            "+${AmountFormatter.formatAssetAmount(fixedFee,
                                    minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                            } ${tx.asset}")
                    .addRow(R.string.tx_percent_fee,
                            "+${AmountFormatter.formatAssetAmount(percentFee,
                                    minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                            } ${tx.asset}")
        } else {
            val receivedAmount = getReceivedAmount(tx)
            val infoCard = InfoCard(cards_layout)
                    .setHeading(R.string.received,
                            "${AmountFormatter.formatAssetAmount(receivedAmount)} ${tx.asset}")
                    .addRow(R.string.amount_sent,
                            "+${AmountFormatter.formatAssetAmount(tx.amount,
                                    minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                            } ${tx.asset}")
            if (!tx.feePaidBySender) {
                infoCard
                        .addRow(R.string.tx_fixed_fee,
                                "-${AmountFormatter.formatAssetAmount(tx.recipientFee.fixed,
                                        minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                                } ${tx.asset}")
                        .addRow(R.string.tx_percent_fee,
                                "-${AmountFormatter.formatAssetAmount(tx.recipientFee.percent,
                                        minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                                } ${tx.asset}")
            }
        }
    }

    private fun getReceivedAmount(tx: PaymentTransaction): BigDecimal {
        var amount = tx.amount

        if (!tx.feePaidBySender) {
            amount -= tx.recipientFee.total
        }

        return amount
    }

    private fun getPaidAmount(tx: PaymentTransaction): BigDecimal {
        var amount = tx.amount + tx.senderFee.total

        if (tx.feePaidBySender) {
            amount += tx.recipientFee.total
        }

        return amount
    }

    private fun displaySubject(item: PaymentTransaction) {
        item.subject?.let { subject ->
            InfoCard(cards_layout)
                    .setHeading(R.string.payment_description, null)
                    .addRow(subject, null)
        }
    }
}