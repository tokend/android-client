package org.tokend.template.features.send.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.logic.TxManager
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
    private lateinit var resultMetaXdr: String

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
                .doOnSuccess { response ->
                    this.resultMetaXdr = response.resultMetaXdr!!
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
                    destAccountId = request.recipient.accountId,
                    amount = networkParams.amountToPrecised(request.amount),
                    subject = request.paymentSubject ?: "",
                    reference = request.reference,
                    feeData = PaymentFeeData(
                            sourceFee = request.fee.senderFee.toXdrFee(networkParams),
                            destinationFee = request.fee.recipientFee.toXdrFee(networkParams),
                            sourcePaysForDest = request.fee.senderPaysForRecipient,
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
        repositoryProvider.balances().apply {
            if (!updateBalancesByTransactionResultMeta(resultMetaXdr, networkParams))
                updateIfEverUpdated()
        }
        repositoryProvider.balanceChanges(request.senderBalanceId).addPayment(request)
        repositoryProvider.balanceChanges(null).addPayment(request)
    }
}