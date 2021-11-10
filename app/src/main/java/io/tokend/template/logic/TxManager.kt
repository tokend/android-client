package io.tokend.template.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.util.confirmation.ConfirmationProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.transactions.model.SubmitTransactionResponse
import org.tokend.wallet.*
import org.tokend.wallet.xdr.Operation

/**
 * Manages transactions sending
 */
class TxManager(
    private val apiProvider: ApiProvider,
    private val confirmationProvider: ConfirmationProvider<Transaction>? = null
) {
    fun submit(
        transaction: Transaction,
        waitForIngest: Boolean = true
    ): Single<SubmitTransactionResponse> {
        val confirmationCompletable =
            confirmationProvider?.requestConfirmation(transaction)
                ?: Completable.complete()

        return confirmationCompletable
            .andThen(
                apiProvider.getApi()
                    .v3
                    .transactions
                    .submit(transaction, waitForIngest)
                    .toSingle()
            )
    }

    companion object {
        /**
         * @return transaction with given [operations] for [sourceAccountId] signed by [signer]
         */
        fun createSignedTransaction(
            networkParams: NetworkParams,
            sourceAccountId: String,
            signer: Account,
            vararg operations: Operation.OperationBody
        ): Single<Transaction> {
            return Single.defer {
                val transaction =
                    TransactionBuilder(
                        networkParams,
                        PublicKeyFactory.fromAccountId(sourceAccountId)
                    )
                        .addOperations(operations.toList())
                        .addSigner(signer)
                        .build()

                Single.just(transaction)
            }.subscribeOn(Schedulers.newThread())
        }
    }
}