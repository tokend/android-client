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
import org.tokend.wallet.xdr.FeeData
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.PaymentFeeData
import org.tokend.wallet.xdr.op_extensions.SimplePaymentOp

class PaymentManager(
        private val repositoryProvider: RepositoryProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val accountProvider: AccountProvider,
        private val txManager: TxManager
) {
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
            val operation = SimplePaymentOp(
                    sourceBalanceID = request.senderBalanceId,
                    destinationBalanceID = request.recipientBalanceId,
                    amount = networkParams.amountToPrecised(request.amount),
                    feeData = PaymentFeeData(
                            sourceFee = FeeData(
                                    paymentFee = networkParams.amountToPrecised(
                                            request.senderFee.percent
                                    ),
                                    fixedFee = networkParams.amountToPrecised(
                                            request.senderFee.fixed
                                    ),
                                    ext = FeeData.FeeDataExt.EmptyVersion()
                            ),
                            destinationFee = FeeData(
                                    paymentFee = networkParams.amountToPrecised(
                                            request.recipientFee.percent
                                    ),
                                    fixedFee = networkParams.amountToPrecised(
                                            request.recipientFee.fixed
                                    ),
                                    ext = FeeData.FeeDataExt.EmptyVersion()
                            ),
                            sourcePaysForDest = request.senderPaysRecipientFee,
                            ext = PaymentFeeData.PaymentFeeDataExt.EmptyVersion()
                    ),
                    subject = request.paymentSubject ?: ""
            )

            val transaction =
                    TransactionBuilder(networkParams, PublicKeyFactory.fromAccountId(sourceAccountId))
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
}