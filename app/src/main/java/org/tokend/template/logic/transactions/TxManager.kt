package org.tokend.template.logic.transactions

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.transactions.model.SubmitTransactionResponse
import org.tokend.template.di.providers.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.template.util.confirmation.ConfirmationProvider
import org.tokend.wallet.*
import org.tokend.wallet.xdr.Operation
import java.util.concurrent.TimeUnit

/**
 * Manages transactions sending
 */
class TxManager(
        private val apiProvider: ApiProvider,
        private val confirmationProvider: ConfirmationProvider<Transaction>? = null
) {
    fun submit(transaction: Transaction): Single<SubmitTransactionResponse> {
        val confirmationCompletable =
                confirmationProvider?.requestConfirmation(transaction)
                        ?: Completable.complete()

        return confirmationCompletable
                .andThen(
                        apiProvider.getApi()
                                .transactions
                                .submit(transaction.getEnvelope().toBase64())
                                .toSingle()
                )
                // Magic delay is required because
                // API doesn't sync with Horizon immediately.
                .delay(1, TimeUnit.SECONDS)
    }

    companion object {
        /**
         * @return transaction with given [operations] for [sourceAccountId] signed by [signer]
         */
        fun createSignedTransaction(networkParams: NetworkParams,
                                    sourceAccountId: String,
                                    signer: Account,
                                    vararg operations: Operation.OperationBody
        ): Single<Transaction> {
            return Single.defer {
                val transaction =
                        TransactionBuilder(networkParams,
                                PublicKeyFactory.fromAccountId(sourceAccountId))
                                .apply {
                                    operations.forEach {
                                        addOperation(it)
                                    }
                                }
                                .build()

                transaction.addSignature(signer)

                Single.just(transaction)
            }.subscribeOn(Schedulers.newThread())
        }
    }
}