package io.tokend.template.features.send.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.logic.providers.WalletInfoProvider
import io.tokend.template.features.balances.model.BalanceRecord
import io.tokend.template.features.send.model.PaymentFee
import io.tokend.template.features.send.model.PaymentRecipient
import io.tokend.template.features.send.model.PaymentRequest
import java.math.BigDecimal

/**
 * Creates payment request with given params:
 * resolves recipient's account ID if needed,
 * loads sender's and recipient's fees
 */
class CreatePaymentRequestUseCase(
    private val recipient: PaymentRecipient,
    private val amount: BigDecimal,
    private val balance: BalanceRecord,
    private val subject: String?,
    private val fee: PaymentFee,
    private val walletInfoProvider: WalletInfoProvider
) {
    class SendToYourselfException : Exception()

    private lateinit var senderAccount: String

    fun perform(): Single<PaymentRequest> {
        return getSenderAccount()
            .doOnSuccess { senderAccount ->
                this.senderAccount = senderAccount

                if (senderAccount == recipient.accountId) {
                    throw SendToYourselfException()
                }
            }
            .flatMap {
                getPaymentRequest()
            }
    }

    private fun getSenderAccount(): Single<String> {
        return walletInfoProvider
            .getWalletInfo()
            .accountId
            .toMaybe()
            .switchIfEmpty(Single.error(IllegalStateException("Missing account ID")))
    }

    private fun getPaymentRequest(): Single<PaymentRequest> {
        return Single.just(
            PaymentRequest(
                amount = amount,
                asset = balance.asset,
                senderAccountId = senderAccount,
                senderBalanceId = balance.id,
                recipient = recipient,
                fee = fee,
                paymentSubject = subject
            )
        )
    }
}