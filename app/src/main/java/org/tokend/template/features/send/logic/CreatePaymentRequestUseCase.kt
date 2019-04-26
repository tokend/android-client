package org.tokend.template.features.send.logic

import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.toMaybe
import org.tokend.template.data.model.history.SimpleFeeRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.send.model.PaymentRecipient
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.logic.FeeManager
import java.math.BigDecimal

/**
 * Creates payment request with given params:
 * resolves recipient's account ID if needed,
 * loads sender's and recipient's fees
 */
class CreatePaymentRequestUseCase(
        private val recipient: PaymentRecipient,
        private val amount: BigDecimal,
        private val asset: String,
        private val subject: String?,
        private val walletInfoProvider: WalletInfoProvider,
        private val feeManager: FeeManager,
        private val balancesRepository: BalancesRepository
) {
    class SendToYourselfException : Exception()

    private lateinit var senderAccount: String
    private lateinit var senderBalance: String
    private lateinit var senderFee: SimpleFeeRecord
    private lateinit var recipientFee: SimpleFeeRecord

    fun perform(): Single<PaymentRequest> {
        return getSenderAccount()
                .doOnSuccess { senderAccount ->
                    this.senderAccount = senderAccount

                    if (senderAccount == recipient.accountId) {
                        throw SendToYourselfException()
                    }
                }
                .flatMap {
                    getSenderBalance()
                }
                .doOnSuccess { senderBalance ->
                    this.senderBalance = senderBalance
                }
                .flatMap {
                    getFees()
                }
                .doOnSuccess { (senderFee, recipientFee) ->
                    this.senderFee = senderFee
                    this.recipientFee = recipientFee
                }
                .flatMap {
                    getPaymentRequest()
                }
    }

    private fun getSenderAccount(): Single<String> {
        return walletInfoProvider
                .getWalletInfo()
                ?.accountId
                .toMaybe()
                .switchIfEmpty(Single.error(IllegalStateException("Missing account ID")))
    }

    private fun getSenderBalance(): Single<String> {
        return balancesRepository
                .updateIfNotFreshDeferred()
                .toSingleDefault(true)
                .flatMapMaybe {
                    balancesRepository
                            .itemsList
                            .find { it.assetCode == asset }
                            ?.id
                            .toMaybe()
                }
                .switchIfEmpty(Single.error(
                        IllegalStateException("No balance ID found for $asset")
                ))
    }

    private fun getFees(): Single<Pair<SimpleFeeRecord, SimpleFeeRecord>> {
        return Single.zip(
                feeManager.getPaymentFee(
                        senderAccount,
                        asset,
                        amount,
                        true
                ),
                feeManager.getPaymentFee(
                        recipient.accountId,
                        asset,
                        amount,
                        false
                ),
                BiFunction { senderFee: SimpleFeeRecord, recipientFee: SimpleFeeRecord ->
                    senderFee to recipientFee
                }
        )
    }

    private fun getPaymentRequest(): Single<PaymentRequest> {
        return Single.just(
                PaymentRequest(
                        amount = amount,
                        asset = asset,
                        senderAccountId = senderAccount,
                        senderBalanceId = senderBalance,
                        recipient = recipient,
                        senderFee = senderFee,
                        recipientFee = recipientFee,
                        paymentSubject = subject
                )
        )
    }
}