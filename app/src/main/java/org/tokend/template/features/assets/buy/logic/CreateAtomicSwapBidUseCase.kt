package org.tokend.template.features.assets.buy.logic

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.requests.model.base.RequestState
import org.tokend.sdk.api.v3.requests.params.RequestParamsV3
import org.tokend.sdk.api.v3.requests.params.RequestsPageParamsV3
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.data.model.AtomicSwapAskRecord
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.assets.buy.model.AtomicSwapInvoice
import org.tokend.template.logic.TxManager
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.Transaction
import org.tokend.wallet.xdr.*
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/**
 * Submits [CreateAtomicSwapBidRequestOp] and waits
 * for invoice to appear in related reviewable request details.
 *
 * Updates asks repository on success
 */
class CreateAtomicSwapBidUseCase(
        private val amount: BigDecimal,
        private val quoteAssetCode: String,
        private val ask: AtomicSwapAskRecord,
        private val apiProvider: ApiProvider,
        private val repositoryProvider: RepositoryProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val accountProvider: AccountProvider,
        private val txManager: TxManager
) {
    private val objectMapper = JsonApiToolsProvider.getObjectMapper()
    private lateinit var accountId: String
    private lateinit var networkParams: NetworkParams
    private lateinit var transaction: Transaction
    private lateinit var transactionResultMetaXdr: String
    private var pendingRequestId: Long = 0L

    fun perform(): Single<AtomicSwapInvoice> {
        return getAccountId()
                .doOnSuccess { accountId ->
                    this.accountId = accountId
                }
                .flatMap {
                    getNetworkParams()
                }
                .doOnSuccess { networkParams ->
                    this.networkParams = networkParams
                }
                .flatMap {
                    getTransaction()
                }
                .doOnSuccess { transaction ->
                    this.transaction = transaction
                }
                .flatMap {
                    getSubmitTransactionResult()
                }
                .doOnSuccess { transactionResultMetaXdr ->
                    this.transactionResultMetaXdr = transactionResultMetaXdr
                }
                .flatMap {
                    getPendingRequestId()
                }
                .doOnSuccess { pendingRequestId ->
                    this.pendingRequestId = pendingRequestId
                }
                .flatMap {
                    getInvoiceFromRequest()
                }
                .doOnSuccess {
                    updateRepositories()
                }
    }

    private fun getAccountId(): Single<String> {
        return walletInfoProvider
                .getWalletInfo()
                ?.accountId
                .toMaybe()
                .switchIfEmpty(Single.error(IllegalStateException("Missing account ID")))
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider
                .systemInfo()
                .getNetworkParams()
    }

    private fun getTransaction(): Single<Transaction> {
        val op = CreateAtomicSwapBidRequestOp(
                request = CreateAtomicSwapBidRequest(
                        askID = ask.id.toLong(),
                        quoteAsset = quoteAssetCode,
                        baseAmount = networkParams.amountToPrecised(amount),
                        creatorDetails = "{}",
                        ext = CreateAtomicSwapBidRequest.CreateAtomicSwapBidRequestExt.EmptyVersion()
                ),
                ext = CreateAtomicSwapBidRequestOp.CreateAtomicSwapBidRequestOpExt.EmptyVersion()
        )

        val account = accountProvider.getAccount()
                ?: return Single.error(IllegalStateException("Cannot obtain current account"))

        return TxManager.createSignedTransaction(networkParams, accountId, account,
                Operation.OperationBody.CreateAtomicSwapBidRequest(op))
    }

    private fun getSubmitTransactionResult(): Single<String> {
        return txManager
                .submit(transaction)
                .map { it.resultMetaXdr!! }
    }

    private fun getPendingRequestId(): Single<Long> = {
        val meta = TransactionMeta.fromBase64(transactionResultMetaXdr)
                as? TransactionMeta.EmptyVersion
                ?: throw IllegalStateException("Unable to parse result meta XDR")

        meta.operations
                .first()
                .changes
                .asSequence()
                .filterIsInstance<LedgerEntryChange.Created>()
                .map { it.created.data }
                .filterIsInstance<LedgerEntry.LedgerEntryData.ReviewableRequest>()
                .map { it.reviewableRequest.requestID }
                .first()
    }.toSingle()

    private fun getInvoiceFromRequest(): Single<AtomicSwapInvoice> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        class NoRequestYetException : Exception()
        class NoInvoiceYetException : Exception()
        class RequestRejectedException : Exception()
        class InvoiceParsingException(cause: Exception) : Exception(cause)

        val getInvoiceFromRequest = {
            signedApi
                    .v3
                    .requests
                    .get(RequestsPageParamsV3(
                            requestor = accountId,
                            type = ReviewableRequestType.CREATE_ATOMIC_SWAP_BID,
                            includes = listOf(RequestParamsV3.Includes.REQUEST_DETAILS),
                            pagingParams = PagingParamsV2(
                                    page = (pendingRequestId - 1).toString(),
                                    order = PagingOrder.ASC,
                                    limit = 1
                            )
                    ))
                    .toSingle()
                    .map { page ->
                        page.items.firstOrNull()
                                ?: throw NoRequestYetException()
                    }
                    .map { request ->
                        if (request.stateI == RequestState.PERMANENTLY_REJECTED.i
                                || request.stateI == RequestState.REJECTED.i) {
                            throw RequestRejectedException()
                        }

                        request.externalDetails
                                ?.withArray(REQUEST_DATA_ARRAY_KEY)
                                ?.get(0)
                                ?.get(INVOICE_KEY)
                                ?: throw NoInvoiceYetException()
                    }
                    .map { invoiceJson ->
                        try {
                            objectMapper.treeToValue(invoiceJson, AtomicSwapInvoice::class.java)
                        } catch (e: Exception) {
                            throw InvoiceParsingException(e)
                        }
                    }
        }

        // Wait for invoice to appear in the request.
        return Single.defer(getInvoiceFromRequest)
                .retryWhen { errors ->
                    errors.flatMap { error ->
                        val retryWithDelay = Flowable.just(true)
                                .delay(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)

                        when (error) {
                            is NoInvoiceYetException -> {
                                Log.i(LOG_TAG, "No invoice yet, retry...")
                                retryWithDelay
                            }
                            is NoRequestYetException -> {
                                Log.i(LOG_TAG, "No request yet, retry...")
                                retryWithDelay
                            }
                            else -> {
                                Flowable.error(error)
                            }
                        }
                    }
                }
    }

    private fun updateRepositories() {
        repositoryProvider.atomicSwapAsks(ask.asset.code).updateIfEverUpdated()
    }

    companion object {
        private const val REQUEST_DATA_ARRAY_KEY = "data"
        private const val INVOICE_KEY = "invoice"
        private const val POLL_INTERVAL_MS = 2000L
        private const val LOG_TAG = "BidCreation"
    }
}