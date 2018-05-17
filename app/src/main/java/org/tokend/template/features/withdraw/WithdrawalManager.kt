package org.tokend.template.features.withdraw

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.responses.SubmitTransactionResponse
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.RepositoryProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.AutoConversionWithdrawalDetails
import org.tokend.wallet.xdr.CreateWithdrawalRequestOp
import org.tokend.wallet.xdr.Fee
import org.tokend.wallet.xdr.Operation

class WithdrawalManager(
        private val repositoryProvider: RepositoryProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val accountProvider: AccountProvider,
        private val txManager: TxManager
) {
    fun submit(request: WithdrawalRequest): Single<SubmitTransactionResponse> {
        return repositoryProvider.systemInfo()
                .getNetworkParams()
                .flatMap { netParams ->
                    val accountId = walletInfoProvider.getWalletInfo()?.accountId
                            ?: throw IllegalStateException("Cannot obtain current account ID")
                    createWithdrawalTransaction(netParams, accountId, request)
                }
                .flatMap {
                    txManager.submit(it)
                }
                .doOnSuccess {
                    repositoryProvider.balances().invalidate()
                    repositoryProvider.transactions(request.asset).invalidate()
                }
    }

    private fun createWithdrawalTransaction(networkParams: NetworkParams,
                                            sourceAccountId: String,
                                            request: WithdrawalRequest): Single<Transaction> {
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
                    TransactionBuilder(networkParams, PublicKeyFactory.fromAccountId(sourceAccountId))
                            .addOperation(Operation.OperationBody.CreateWithdrawalRequest(operation))
                            .build()

            val account = accountProvider.getAccount()
            ?: return@defer Single.error<Transaction>(
                    IllegalStateException("Cannot obtain current account")
            )

            transaction.addSignature(account)

            Single.just(transaction)
        }.subscribeOn(Schedulers.computation())
    }
}