package io.tokend.template.features.withdraw.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.di.providers.WalletInfoProvider
import io.tokend.template.features.balances.model.BalanceRecord
import io.tokend.template.features.fees.logic.FeeManager
import io.tokend.template.features.history.model.SimpleFeeRecord
import io.tokend.template.features.withdraw.model.WithdrawalRequest
import java.math.BigDecimal

/**
 * Creates withdrawal request with given params,
 * loads fee
 */
class CreateWithdrawalRequestUseCase(
    private val amount: BigDecimal,
    private val balance: BalanceRecord,
    private val destinationAddress: String,
    private val walletInfoProvider: WalletInfoProvider,
    private val feeManager: FeeManager
) {
    private val asset = balance.asset

    private lateinit var account: String
    private lateinit var fee: SimpleFeeRecord

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

    private fun getFee(): Single<SimpleFeeRecord> {
        return feeManager
            .getWithdrawalFee(
                account,
                asset.code,
                amount
            )
    }

    private fun getWithdrawalRequest(): Single<WithdrawalRequest> {
        return Single.just(
            WithdrawalRequest(
                account,
                amount,
                asset,
                balance.id,
                destinationAddress,
                fee
            )
        )
    }
}