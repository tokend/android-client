package org.tokend.template.data.model.history.details

import org.tokend.sdk.api.generated.resources.OpPayoutDetailsResource
import org.tokend.template.data.model.history.SimpleFeeRecord
import java.math.BigDecimal

class PayoutDetails(
        /**
         *  Max amount of asset, that owner wants to pay out
         */
        val maxPayoutAmount: BigDecimal,
        /**
         * Min tokens amount which will be payed for one balance
         */
        val minPayoutAmount: BigDecimal,
        /**
         * Min tokens amount for which holder will received dividends
         */
        val minAssetHolderAmount: BigDecimal,

        /**
         * Amount of tokens that were actually payed out
         */
        val actualPayoutAmount: BigDecimal,

        val expectedFee: SimpleFeeRecord,
        val actualFee: SimpleFeeRecord,
        val assetCode: String,
        val sourceAccountId: String
) : BalanceChangeDetails() {
    constructor(op: OpPayoutDetailsResource) : this(
            maxPayoutAmount = op.maxPayoutAmount,
            minPayoutAmount = op.minPayoutAmount,
            minAssetHolderAmount = op.minAssetHolderAmount,
            expectedFee = SimpleFeeRecord(op.expectedFee),
            actualFee = SimpleFeeRecord(op.actualFee),
            actualPayoutAmount = op.actualPayoutAmount,
            assetCode = op.asset.id,
            sourceAccountId = op.sourceAccount.id
    )

    /**
     * @return true if account with given ID is an issuer of the payout
     */
    fun isIssuer(accountId: String): Boolean {
        return sourceAccountId == accountId
    }
}