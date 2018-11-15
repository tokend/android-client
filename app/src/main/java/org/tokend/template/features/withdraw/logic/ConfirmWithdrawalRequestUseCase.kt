package org.tokend.template.features.withdraw.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.logic.transactions.TxManager
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.AutoConversionWithdrawalDetails
import org.tokend.wallet.xdr.CreateWithdrawalRequestOp
import org.tokend.wallet.xdr.Fee
import org.tokend.wallet.xdr.Operation

/**
 * Sends withdrawal request.
 *
 * Updates related repositories: balances, transactions
 */
class ConfirmWithdrawalRequestUseCase(
        private val request: WithdrawalRequest,
        private val accountProvider: AccountProvider,
        private val repositoryProvider: RepositoryProvider,
        private val txManager: TxManager
) {
    private lateinit var networkParams: NetworkParams

    fun perform(): Completable {
        return getNetworkParams()
                .doOnSuccess { networkParams ->
                    this.networkParams = networkParams
                }
                .flatMap {
                    getTransaction()
                }
                .flatMap { transaction ->
                    txManager.submit(transaction)
                }
                .doOnSuccess {
                    updateRepositories()
                }
                .toCompletable()
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider
                .systemInfo()
                .getNetworkParams()
    }

    private fun getTransaction(): Single<Transaction> {
        return Single.defer {
            val balanceId = repositoryProvider.balances().itemsSubject.value
                    .find { it.asset == request.asset }
                    ?.balanceId
                    ?: return@defer Single.error<Transaction>(
                            IllegalStateException("Cannot obtain balance ID for ${request.asset}")
                    )

            val precisedAmount = networkParams.amountToPrecised(request.amount)
            val operation = CreateWithdrawalRequestOp(
                    request = org.tokend.wallet.xdr.WithdrawalRequest(
                            balance = PublicKeyFactory.fromBalanceId(balanceId),
                            amount = precisedAmount,
                            fee = Fee(
                                    fixed = networkParams.amountToPrecised(request.fee.fixed),
                                    percent = networkParams.amountToPrecised(request.fee.percent),
                                    ext = Fee.FeeExt.EmptyVersion()
                            ),
                            details = org.tokend.wallet.xdr.WithdrawalRequest
                                    .WithdrawalRequestDetails.AutoConversion(
                                    AutoConversionWithdrawalDetails(
                                            destAsset = request.asset,
                                            expectedAmount = precisedAmount,
                                            ext = AutoConversionWithdrawalDetails
                                                    .AutoConversionWithdrawalDetailsExt.EmptyVersion()
                                    )
                            ),
                            externalDetails = "{\"address\":\"${request.destinationAddress}\"}",
                            preConfirmationDetails = "",
                            universalAmount = 0L,
                            ext = org.tokend.wallet.xdr.WithdrawalRequest
                                    .WithdrawalRequestExt.EmptyVersion()
                    ),
                    ext = CreateWithdrawalRequestOp.CreateWithdrawalRequestOpExt.EmptyVersion()
            )

            val transaction =
                    TransactionBuilder(networkParams, request.accountId)
                            .addOperation(Operation.OperationBody.CreateWithdrawalRequest(operation))
                            .build()

            val account = accountProvider.getAccount()
                    ?: return@defer Single.error<Transaction>(
                            IllegalStateException("Cannot obtain current account")
                    )

            transaction.addSignature(account)

            Single.just(transaction)
        }.subscribeOn(Schedulers.newThread())
    }

    private fun updateRepositories() {
        repositoryProvider.balances().updateIfEverUpdated()
        repositoryProvider.transactions(request.asset).updateIfEverUpdated()
    }
}