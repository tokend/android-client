package org.tokend.template.base.logic.payment

import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.toMaybe
import org.tokend.sdk.api.fees.model.Fee
import org.tokend.template.base.logic.FeeManager
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.repository.AccountDetailsRepository
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.wallet.Base32Check
import java.math.BigDecimal

class CreatePaymentRequestUseCase(
        private val recipient: String,
        private val amount: BigDecimal,
        private val asset: String,
        private val subject: String?,
        private val walletInfoProvider: WalletInfoProvider,
        private val feeManager: FeeManager,
        private val balancesRepository: BalancesRepository,
        private val accountDetailsRepository: AccountDetailsRepository?
) {
    class SendToYourselfException : Exception()

    private lateinit var senderAccount: String
    private lateinit var senderBalance: String
    private lateinit var recipientAccount: String
    private lateinit var senderFee: Fee
    private lateinit var recipientFee: Fee

    fun perform(): Single<PaymentRequest> {
        return getAccounts()
                .doOnSuccess { (senderAccount, recipientAccount) ->
                    this.senderAccount = senderAccount
                    this.recipientAccount = recipientAccount

                    if (senderAccount == recipientAccount) {
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

    private fun getAccounts(): Single<Pair<String, String>> {
        return Single.zip(
                getSenderAccount(),
                getRecipientAccount(),
                BiFunction { senderAccount: String, recipientAccount: String ->
                    senderAccount to recipientAccount
                }
        )
    }

    private fun getSenderAccount(): Single<String> {
        return walletInfoProvider
                .getWalletInfo()
                ?.accountId
                .toMaybe()
                .switchIfEmpty(Single.error(IllegalStateException("Missing account ID")))
    }

    private fun getRecipientAccount(): Single<String> {
        return if (Base32Check.isValid(Base32Check.VersionByte.ACCOUNT_ID,
                        recipient.toCharArray()))
            Single.just(recipient)
        else
            accountDetailsRepository
                    ?.getAccountIdByEmail(recipient)
                    ?: Single.error(
                            IllegalStateException("Account details repository is required" +
                                    " to get recipient's account ID")
                    )
    }

    private fun getSenderBalance(): Single<String> {
        return balancesRepository
                .updateIfNotFreshDeferred()
                .toSingleDefault(true)
                .flatMapMaybe {
                    balancesRepository
                            .itemsSubject
                            .value
                            .find { it.asset == asset }
                            ?.balanceId
                            .toMaybe()
                }
                .switchIfEmpty(Single.error(
                        IllegalStateException("No balance ID found for $asset")
                ))
    }

    private fun getFees(): Single<Pair<Fee, Fee>> {
        return Single.zip(
                feeManager.getPaymentFee(
                        senderAccount,
                        asset,
                        amount,
                        true
                ),
                feeManager.getPaymentFee(
                        recipientAccount,
                        asset,
                        amount,
                        false
                ),
                BiFunction { senderFee: Fee, recipientFee: Fee ->
                    senderFee to recipientFee
                }
        )
    }

    private fun getPaymentRequest(): Single<PaymentRequest> {
        return Single.just(
                PaymentRequest(
                        amount = amount,
                        asset = asset,
                        senderBalanceId = senderBalance,
                        senderFee = senderFee,
                        recipientAccountId = recipientAccount,
                        recipientFee = recipientFee,
                        recipientNickname = recipient,
                        paymentSubject = subject
                )
        )
    }
}