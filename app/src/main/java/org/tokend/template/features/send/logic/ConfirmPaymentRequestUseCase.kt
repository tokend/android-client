package org.tokend.template.features.send.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.PaymentFeeData
import org.tokend.wallet.xdr.op_extensions.SimplePaymentOp

/**
 * Sends payment identified by given payment request.
 * Updates related repositories: balances, transactions
 */
class ConfirmPaymentRequestUseCase(
        private val request: PaymentRequest,
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
                .ignoreElement()
    }

    private fun getTransaction(): Single<Transaction> {
        return Single.defer {
            val operation = SimplePaymentOp(
                    sourceBalanceId = request.senderBalanceId,
                    destAccountId = request.recipientAccountId,
                    amount = networkParams.amountToPrecised(request.amount),
                    subject = request.paymentSubject ?: "",
                    reference = request.reference,
                    feeData = PaymentFeeData(
                            sourceFee = request.senderFee.toXdrFee(networkParams),
                            destinationFee = request.recipientFee.toXdrFee(networkParams),
                            sourcePaysForDest = request.senderPaysRecipientFee,
                            ext = PaymentFeeData.PaymentFeeDataExt.EmptyVersion()
                    )
            )

            val transaction =
                    TransactionBuilder(networkParams, request.senderAccountId)
                            .addOperation(Operation.OperationBody.Payment(operation))
                            .build()

            val account = accountProvider.getAccount()
                    ?: return@defer Single.error<Transaction>(
                            IllegalStateException("Cannot obtain current account")
                    )

            transaction.addSignature(account)

            Single.just(transaction)
        }.subscribeOn(Schedulers.newThread())
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider
                .systemInfo()
                .getNetworkParams()
    }

    private fun updateRepositories() {
        repositoryProvider.balances().updateIfEverUpdated()
        repositoryProvider.balanceChanges(request.senderBalanceId).updateIfEverUpdated()
    }
}