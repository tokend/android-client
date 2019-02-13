package org.tokend.template.features.send.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.Fee
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.PaymentFeeDataV2
import org.tokend.wallet.xdr.PaymentOpV2

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
            val operation = PaymentOpV2(
                    sourceBalanceID = PublicKeyFactory.fromBalanceId(request.senderBalanceId),
                    destination = PaymentOpV2.PaymentOpV2Destination.Account(
                            PublicKeyFactory.fromAccountId(request.recipientAccountId)
                    ),
                    amount = networkParams.amountToPrecised(request.amount),
                    subject = request.paymentSubject ?: "",
                    reference = request.reference,
                    feeData = PaymentFeeDataV2(
                            sourceFee = Fee(
                                    fixed = networkParams.amountToPrecised(
                                            request.senderFee.fixed
                                    ),
                                    percent = networkParams.amountToPrecised(
                                            request.senderFee.total
                                    ),
                                    ext = Fee.FeeExt.EmptyVersion()
                            ),
                            destinationFee = Fee(
                                    fixed = networkParams.amountToPrecised(
                                            request.recipientFee.fixed
                                    ),
                                    percent = networkParams.amountToPrecised(
                                            request.recipientFee.total
                                    ),
                                    ext = Fee.FeeExt.EmptyVersion()
                            ),
                            sourcePaysForDest = request.senderPaysRecipientFee,
                            ext = PaymentFeeDataV2.PaymentFeeDataV2Ext.EmptyVersion()
                    ),
                    ext = PaymentOpV2.PaymentOpV2Ext.EmptyVersion()
            )

            val transaction =
                    TransactionBuilder(networkParams, request.senderAccountId)
                            .addOperation(Operation.OperationBody.PaymentV2(operation))
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