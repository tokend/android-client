package org.tokend.template.features.withdraw.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.template.logic.FeeManager
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.fees.model.FeeRecord
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import java.math.BigDecimal

/**
 * Creates withdrawal request with given params,
 * loads fee
 */
class CreateWithdrawalRequestUseCase(
        private val amount: BigDecimal,
        private val asset: String,
        private val destinationAddress: String,
        private val walletInfoProvider: WalletInfoProvider,
        private val feeManager: FeeManager
) {
    private lateinit var account: String
    private lateinit var fee: FeeRecord

    fun perform(): Single<WithdrawalRequest> {
        return getAccount()
                .doOnSuccess { account ->
                    this.account = account
                }
                .flatMap {
                    getFee()
                }
                .doOnSuccess { fee ->
                    this.fee = fee
                }
                .flatMap {
                    getWithdrawalRequest()
                }

    }

    private fun getAccount(): Single<String> {
        return walletInfoProvider
                .getWalletInfo()
                ?.accountId
                .toMaybe()
                .switchIfEmpty(Single.error(IllegalStateException("Missing account ID")))
    }

    private fun getFee(): Single<FeeRecord> {
        return feeManager
                .getWithdrawalFee(
                        account,
                        asset,
                        amount
                )
    }

    private fun getWithdrawalRequest(): Single<WithdrawalRequest> {
        return Single.just(
                WithdrawalRequest(
                        account,
                        amount,
                        asset,
                        destinationAddress,
                        fee
                )
        )
    }
}