package org.tokend.template.base.activities.tx_details

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.api.base.model.operations.PaymentOperation
import org.tokend.template.R
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import java.math.BigDecimal

class PaymentDetailsActivity : TxDetailsActivity<PaymentOperation>(PaymentOperation::class) {
    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        setTitle(R.string.payment_details_title)
    }

    override fun displayDetails(item: PaymentOperation) {
        displayStateIfNeeded(item, cards_layout)
        displayReceiverSender(item)
        displayAmount(item)
        if (!item.subject.isNullOrBlank()) {
            displaySubject(item)
        }
        displayDate(item, cards_layout)
    }

    private fun displayReceiverSender(tx: PaymentOperation) {
        val receiverSenderCard = InfoCard(cards_layout)
                .setHeading(if (tx.isSent) R.string.tx_recipient else R.string.tx_sender, null)

        if (tx.counterpartyNickname != null) {
            receiverSenderCard.addRow(tx.counterpartyNickname, null)
        }

        receiverSenderCard.addRow(if (tx.isSent) tx.destAccount else tx.sourceAccount, null)
    }

    private fun displayAmount(tx: PaymentOperation) {
        if (tx.isSent) {
            val infoCard = InfoCard(cards_layout)
                    .setHeading(R.string.amount,
                            "${AmountFormatter.formatAssetAmount(tx.amount)} ${tx.asset}")
                    .addRow(R.string.tx_fee,
                            "${AmountFormatter.formatAssetAmount(tx.senderFee.total)
                            } ${tx.senderFee.asset}")
            if (tx.feePaidBySender) {
                infoCard
                        .addRow(R.string.tx_recipient_fee,
                                "${AmountFormatter.formatAssetAmount(tx.recipientFee.total)
                                } ${tx.recipientFee.asset}")
            }
        } else {
            val fee =
                    if (tx.feePaidBySender)
                        BigDecimal.ZERO
                    else
                        tx.recipientFee.total

            InfoCard(cards_layout)
                    .setHeading(R.string.amount,
                            "${AmountFormatter.formatAssetAmount(tx.amount)} ${tx.asset}")
                    .addRow(R.string.tx_fee,
                            "${AmountFormatter.formatAssetAmount(fee)
                            } ${tx.recipientFee.asset}")
        }
    }

    private fun displaySubject(item: PaymentOperation) {
        item.subject?.let { subject ->
            InfoCard(cards_layout)
                    .setHeading(R.string.payment_description, null)
                    .addRow(subject, null)
        }
    }
}