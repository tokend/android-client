package org.tokend.template.data.model.history.details

import org.tokend.sdk.api.generated.resources.OpPaymentDetailsResource
import org.tokend.template.data.model.history.SimpleFeeRecord

class PaymentDetails(
        val sourceAccountId: String,
        val destAccountId: String,
        val sourceFee: SimpleFeeRecord,
        val destFee: SimpleFeeRecord,
        val isDestFeePaidBySource: Boolean,
        val subject: String?,
        val reference: String?,
        val sourceName: String?,
        val destName: String?
) : BalanceChangeDetails() {
    constructor(op: OpPaymentDetailsResource): this(
            sourceAccountId = op.accountFrom.id,
            destAccountId = op.accountTo.id,
            destFee = SimpleFeeRecord(op.destinationFee),
            sourceFee = SimpleFeeRecord(op.sourceFee),
            isDestFeePaidBySource = op.sourcePayForDestination(),
            reference = op.reference,
            subject = op.subject.takeIf { it.isNotEmpty() },
            destName = null,
            sourceName = null
    )

    /**
     * @return true if given [accountId] is the receiver of the payment
     */
    fun isReceived(accountId: String): Boolean {
        return destAccountId == accountId
    }

    /**
     * @return receiver or sender account ID on [yourAccountId]
     *
     * @see isReceived
     */
    fun getCounterpartyAccountId(yourAccountId: String): String {
        return if (isReceived(yourAccountId))
            sourceAccountId
        else
            destAccountId
    }

    /**
     * @return receiver or sender name if it's set based on [yourAccountId]
     *
     * @see isReceived
     */
    fun getCounterpartyName(yourAccountId: String): String? {
        return if (isReceived(yourAccountId))
            sourceName
        else
            destName
    }
}