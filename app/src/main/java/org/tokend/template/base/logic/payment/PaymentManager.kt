package org.tokend.template.base.logic.payment

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.responses.SubmitTransactionResponse
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.RepositoryProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.FeeDataV2
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.PaymentFeeDataV2
import org.tokend.wallet.xdr.op_extensions.SimplePaymentOpV2

class PaymentManager(
        private val repositoryProvider: RepositoryProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val accountProvider: AccountProvider,
        private val txManager: TxManager
) {
    /**
     * Submits given request as a transaction,
     * updates payments-related repositories.
     */
    fun submit(request: PaymentRequest): Single<SubmitTransactionResponse> {
        return repositoryProvider.systemInfo()
                .getNetworkParams()
                .flatMap { netParams ->
                    val accountId = walletInfoProvider.getWalletInfo()?.accountId
                            ?: throw IllegalStateException("Cannot obtain current account ID")
                    createPaymentTransaction(netParams, accountId, request)
                }
                .flatMap {
                    txManager.submit(it)
                }
                .doOnSuccess {
                    repositoryProvider.balances().invalidate()
                    repositoryProvider.transactions(request.asset).invalidate()
                }
    }

    private fun createPaymentTransaction(networkParams: NetworkParams,
                                         sourceAccountId: String,
                                         request: PaymentRequest): Single<Transaction> {
        return Single.defer {
            val operation = SimplePaymentOpV2(
                    sourceBalanceId = request.senderBalanceId,
                    destAccountId = request.recipientAccountId,
                    amount = networkParams.amountToPrecised(request.amount),
                    subject = request.paymentSubject ?: "",
                    feeData = PaymentFeeDataV2(
                            sourceFee = FeeDataV2(
                                    fixedFee = networkParams.amountToPrecised(
                                            request.senderFee.fixed
                                    ),
                                    maxPaymentFee = networkParams.amountToPrecised(
                                            request.senderFee.total
                                    ),
                                    feeAsset = request.senderFee.asset,
                                    ext = FeeDataV2.FeeDataV2Ext.EmptyVersion()
                            ),
                            destinationFee = FeeDataV2(
                                    fixedFee = networkParams.amountToPrecised(
                                            request.recipientFee.fixed
                                    ),
                                    maxPaymentFee = networkParams.amountToPrecised(
                                            request.recipientFee.total
                                    ),
                                    feeAsset = request.recipientFee.asset,
                                    ext = FeeDataV2.FeeDataV2Ext.EmptyVersion()
                            ),
                            sourcePaysForDest = request.senderPaysRecipientFee,
                            ext = PaymentFeeDataV2.PaymentFeeDataV2Ext.EmptyVersion()
                    )
            )

            val transaction =
                    TransactionBuilder(networkParams, PublicKeyFactory.fromAccountId(sourceAccountId))
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
}