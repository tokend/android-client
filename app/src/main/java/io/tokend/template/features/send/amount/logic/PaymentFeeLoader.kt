package io.tokend.template.features.send.amount.logic

import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.tokend.template.logic.providers.WalletInfoProvider
import io.tokend.template.features.fees.logic.FeeManager
import io.tokend.template.features.history.model.SimpleFeeRecord
import io.tokend.template.features.send.model.PaymentFee
import java.math.BigDecimal

/**
 * Loads sender's and recipient's fees for payment.
 */
class PaymentFeeLoader(
    private val walletInfoProvider: WalletInfoProvider,
    private val feeManager: FeeManager
) {
    /**
     * Performs loading for non-zero amounts,
     * returns zero fees for 0
     */
    fun load(
        amount: BigDecimal,
        assetCode: String,
        recipientAccount: String
    ): Single<PaymentFee> {
        val senderAccount = walletInfoProvider.getWalletInfo().accountId

        if (amount.signum() == 0) {
            return Single.just(PaymentFee(SimpleFeeRecord.ZERO, SimpleFeeRecord.ZERO))
        }

        return Single.zip(
            feeManager.getPaymentFee(
                senderAccount,
                assetCode,
                amount,
                true
            ),
            feeManager.getPaymentFee(
                recipientAccount,
                assetCode,
                amount,
                false
            ),
            { senderFee: SimpleFeeRecord, recipientFee: SimpleFeeRecord ->
                PaymentFee(senderFee, recipientFee)
            }
        )
    }
}