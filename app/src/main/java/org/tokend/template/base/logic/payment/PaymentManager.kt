package org.tokend.template.base.logic.payment

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.responses.SubmitTransactionResponse
import org.tokend.template.base.logic.FeeManager
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.RepositoryProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.*
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
            val operation = PaymentOpV2(
                    amount = networkParams.amountToPrecised(request.amount),
                    sourceBalanceID = PublicKeyFactory.fromBalanceId(request.senderBalanceId),
                    destination = PaymentOpV2.PaymentOpV2Destination.Account(PublicKeyFactory.fromAccountId(request.recipientAccountId)),
                    subject = request.paymentSubject ?: "",
                    feeData = PaymentFeeDataV2(
                            sourceFee = FeeDataV2(
                                    fixedFee = networkParams.amountToPrecised(request.senderFee.fixed),
                                    ext = FeeDataV2.FeeDataV2Ext.EmptyVersion(),
                                    maxPaymentFee = networkParams.amountToPrecised(request.senderFee.percent.add(request.senderFee.fixed)),
                                    feeAsset = request.senderFee.asset
                            ),
                            destinationFee = FeeDataV2(
                                    fixedFee = networkParams.amountToPrecised(request.recipientFee.fixed),
                                    ext = FeeDataV2.FeeDataV2Ext.EmptyVersion(),
                                    maxPaymentFee = networkParams.amountToPrecised(request.recipientFee.percent.add(request.recipientFee.fixed)),
                                    feeAsset = request.recipientFee.asset
                            ),
                            sourcePaysForDest = request.senderPaysRecipientFee,
                            ext = PaymentFeeDataV2.PaymentFeeDataV2Ext.EmptyVersion()
                    ),
                    ext = PaymentOpV2.PaymentOpV2Ext.EmptyVersion(),
                    reference = ""
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