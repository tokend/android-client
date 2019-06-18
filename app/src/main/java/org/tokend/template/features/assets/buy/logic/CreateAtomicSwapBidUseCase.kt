package org.tokend.template.features.assets.buy.logic

import android.util.Log
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.generated.resources.AtomicSwapRequestDetailsResource
import org.tokend.sdk.api.generated.resources.ReviewableRequestResource
import org.tokend.sdk.api.v3.requests.params.RequestParamsV3
import org.tokend.sdk.api.v3.requests.params.RequestsPageParamsV3
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.sdk.utils.extentions.encodeBase64String
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

/**
 * Submits [CreateAtomicSwapBidRequestOp] and waits
 * for invoice to appear in related reviewable request details
 */
class CreateAtomicSwapBidUseCase(
        private val amount: BigDecimal,
        private val quoteAssetCode: String,
        private val askId: String,
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
                        askID = askId.toLong(),
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

        class NoRequestYetException: Exception()

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
                                    details is AtomicSwapRequestDetailsResource
                                            && details.creatorDetails
                                            .get(REFERENCE_KEY).asText() == reference
                                }
                                ?.id
                                ?: throw NoRequestYetException()
                    }
        }

        // Wait for request to appear.
        return Single.defer(getRequestWithReference)
                .retry { error ->
                    if (error is NoRequestYetException) {
                        Log.i(LOG_TAG, "No request yet, retry...")
                        Thread.sleep(POLL_INTERVAL_MS)
                        true
                    } else {
                        false
                    }
                }
    }

    private fun getInvoiceFromRequest(): Single<AtomicSwapInvoice> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        class NoInvoiceYetException: Exception()

        val getInvoiceFromRequest = {
            signedApi
                    .v3
                    .requests
                    .getById(
                            requestId = pendingRequestId
                    )
                    .toSingle()
                    .map { request ->
                        request.externalDetails?.get(INVOICE_KEY)
                                ?: throw NoInvoiceYetException()
                    }
                    .map { invoiceJson ->
                        objectMapper.treeToValue(invoiceJson, AtomicSwapInvoice::class.java)
                    }
        }

        // Wait for invoice to appear in the request.
        return Single.defer(getInvoiceFromRequest)
                .retry { error ->
                    if (error is NoInvoiceYetException) {
                        Log.i(LOG_TAG, "No invoice yet, retry...")
                        Thread.sleep(POLL_INTERVAL_MS)
                        true
                    } else {
                        false
                    }
                }
    }

    companion object {
        private const val REFERENCE_KEY = "reference"
        private const val INVOICE_KEY = "invoice"
        private const val POLL_INTERVAL_MS = 2000L
        private const val LOG_TAG = "BidCreation"
    }
}