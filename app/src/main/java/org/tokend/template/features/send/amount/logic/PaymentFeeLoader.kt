package org.tokend.template.features.send.amount.logic

import io.reactivex.Single
import io.reactivex.functions.BiFunction
import org.tokend.template.data.model.history.SimpleFeeRecord
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.send.model.PaymentFee
import org.tokend.template.logic.FeeManager
import java.math.BigDecimal

/**
 * Loads sender's and recipient's fees for payment.
 */
class PaymentFeeLoader(
        private val walletInfoProvider: WalletInfoProvider,
        private val feeManager: FeeManager
) {
    fun load(amount: BigDecimal,
             assetCode: String,
             recipientAccount: String): Single<PaymentFee> {
        val senderAccount = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        return Single.zip(
                feeManager.getPaymentFee(
                        senderAccount,
                        assetCode,
                        amount,
                        true
                ),
                feeManager.getPaymentFee(
                        recipientAccount,
                        recipientAccount,
                        amount,
                        false
                ),
                BiFunction { senderFee: SimpleFeeRecord, recipientFee: SimpleFeeRecord ->
                    PaymentFee(senderFee, recipientFee)
                }
        )
    }
}