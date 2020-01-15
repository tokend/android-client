package org.tokend.template.features.withdraw.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.logic.TxManager
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.CreateWithdrawalRequestOp
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
    private lateinit var resultMeta: String

    fun perform(): Completable {
        return getNetworkParams()
                .doOnSuccess { networkParams ->
                    this.networkParams = networkParams
                }
                .flatMap {
                    getTransaction()
                }
                .flatMap { transaction ->
                    txManager.submit(transaction, waitForIngest = false)
                }
                .doOnSuccess { result ->
                    this.resultMeta = result.resultMetaXdr!!
                }
                .doOnSuccess {
                    updateRepositories()
                }
                .ignoreElement()
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider
                .systemInfo()
                .getNetworkParams()
    }

    private fun getTransaction(): Single<Transaction> {
        return Single.defer {
            val precisedAmount = networkParams.amountToPrecised(request.amount)
            val operation = CreateWithdrawalRequestOp(
                    request = org.tokend.wallet.xdr.WithdrawalRequest(
                            balance = PublicKeyFactory.fromBalanceId(request.balanceId),
                            amount = precisedAmount,
                            fee = request.fee.toXdrFee(networkParams),
                            universalAmount = 0,
                            creatorDetails = "{\"address\":\"${request.destinationAddress}\"}",
                            ext = org.tokend.wallet.xdr.WithdrawalRequest
                                    .WithdrawalRequestExt.EmptyVersion()
                    ),
                    ext = CreateWithdrawalRequestOp.CreateWithdrawalRequestOpExt.EmptyVersion(),
                    allTasks = null
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
        repositoryProvider.balances().apply {
            if (!updateBalancesByTransactionResultMeta(resultMeta, networkParams))
                updateIfEverUpdated()
        }
        repositoryProvider.balanceChanges(request.balanceId).updateIfEverUpdated()
        repositoryProvider.balanceChanges(null).updateIfEverUpdated()
    }
}