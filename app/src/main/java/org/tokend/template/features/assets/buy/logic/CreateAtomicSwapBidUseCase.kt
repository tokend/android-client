package org.tokend.template.features.assets.buy.logic

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.generated.resources.AtomicSwapBidRequestDetailsResource
import org.tokend.sdk.api.generated.resources.ReviewableRequestResource
import org.tokend.sdk.api.requests.model.base.RequestState
import org.tokend.sdk.api.v3.requests.params.RequestParamsV3
import org.tokend.sdk.api.v3.requests.params.RequestsPageParamsV3
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.sdk.utils.extentions.encodeBase64String
import org.tokend.template.data.model.AtomicSwapAskRecord
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.assets.buy.model.AtomicSwapInvoice
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.Transaction
import org.tokend.wallet.xdr.CreateAtomicSwapBidRequest
import org.tokend.wallet.xdr.CreateAtomicSwapBidRequestOp
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.ReviewableRequestType
import java.math.BigDecimal
import java.security.SecureRandom
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
    private lateinit var reference: String
    private lateinit var transaction: Transaction
    private lateinit var pendingRequestId: String

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
                    getReference()
                }
                .doOnSuccess { reference ->
                    this.reference = reference
                }
                .flatMap {
                    getTransaction()
                }
                .doOnSuccess { transaction ->
                    this.transaction = transaction
                }
                .flatMap {
                    submitTransaction()
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

    private fun getReference(): Single<String> {
        return {
            SecureRandom.getSeed(12).encodeBase64String()
        }.toSingle().subscribeOn(Schedulers.newThread())
    }

    private fun getTransaction(): Single<Transaction> {
        val op = CreateAtomicSwapBidRequestOp(
                request = CreateAtomicSwapBidRequest(
                        askID = ask.id.toLong(),
                        quoteAsset = quoteAssetCode,
                        baseAmount = networkParams.amountToPrecised(amount),
                        // Unique data is required to identify the request
                        creatorDetails = "{\"$REFERENCE_KEY\":\"$reference\"}",
                        ext = CreateAtomicSwapBidRequest.CreateAtomicSwapBidRequestExt.EmptyVersion()
                ),
                ext = CreateAtomicSwapBidRequestOp.CreateAtomicSwapBidRequestOpExt.EmptyVersion()
        )

        val account = accountProvider.getAccount()
                ?: return Single.error(IllegalStateException("Cannot obtain current account"))

        return TxManager.createSignedTransaction(networkParams, accountId, account,
                Operation.OperationBody.CreateAtomicSwapBidRequest(op))
    }

    private fun submitTransaction(): Single<Boolean> {
        return txManager
                .submit(transaction)
                .map { true }
    }

    private fun getPendingRequestId(): Single<String> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        class NoRequestYetException : Exception()

        val getRequestWithReference = {
            signedApi
                    .v3
                    .requests
                    .get(
                            RequestsPageParamsV3(
                                    requestor = accountId,
                                    type = ReviewableRequestType.CREATE_ATOMIC_SWAP_BID,
                                    includes = listOf(RequestParamsV3.Includes.REQUEST_DETAILS),
                                    pagingParams = PagingParamsV2(
                                            order = PagingOrder.DESC,
                                            limit = 3
                                    )
                            )
                    )
                    .toSingle()
                    .map(DataPage<ReviewableRequestResource>::items)
                    .map { recentRequests ->
                        recentRequests
                                .find { request ->
                                    val details = request.requestDetails
                                    details is AtomicSwapBidRequestDetailsResource
                                            && details.creatorDetails
                                            .get(REFERENCE_KEY).asText() == reference
                                }
                                ?.id
                                ?: throw NoRequestYetException()
                    }
        }

        // Wait for request to appear.
        return Single.defer(getRequestWithReference)
                .retryWhen { errors ->
                    errors.flatMap { error ->
                        if (error is NoRequestYetException) {
                            Log.i(LOG_TAG, "No request yet, retry...")
                            Flowable.just(true)
                                    .delay(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                        } else {
                            Flowable.error(error)
                        }
                    }
                }
    }

    private fun getInvoiceFromRequest(): Single<AtomicSwapInvoice> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        class NoInvoiceYetException : Exception()
        class RequestRejectedException : Exception()
        class InvoiceParsingException(cause: Exception) : Exception(cause)

        val getInvoiceFromRequest = {
            signedApi
                    .v3
                    .requests
                    .getById(
                            requestorAccount = accountId,
                            requestId = pendingRequestId
                    )
                    .toSingle()
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
                        if (error is NoInvoiceYetException) {
                            Log.i(LOG_TAG, "No invoice yet, retry...")
                            Flowable.just(true)
                                    .delay(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                        } else {
                            Flowable.error(error)
                        }
                    }
                }
    }

    private fun updateRepositories() {
        repositoryProvider.atomicSwapAsks(ask.asset.code).updateIfEverUpdated()
    }

    companion object {
        private const val REFERENCE_KEY = "reference"
        private const val REQUEST_DATA_ARRAY_KEY = "data"
        private const val INVOICE_KEY = "invoice"
        private const val POLL_INTERVAL_MS = 2000L
        private const val LOG_TAG = "BidCreation"
    }
}