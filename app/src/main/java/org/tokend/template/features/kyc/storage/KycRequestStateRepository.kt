package org.tokend.template.features.kyc.storage

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.TokenDApi
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.generated.resources.ChangeRoleRequestResource
import org.tokend.sdk.api.generated.resources.ReviewableRequestResource
import org.tokend.sdk.api.v3.requests.model.RequestState
import org.tokend.sdk.api.v3.requests.params.ChangeRoleRequestPageParams
import org.tokend.sdk.api.v3.requests.params.RequestParamsV3
import org.tokend.sdk.utils.extentions.getTypedRequestDetails
import org.tokend.template.data.repository.BlobsRepository
import org.tokend.template.data.storage.repository.SingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.kyc.model.KycForm
import org.tokend.template.features.kyc.model.KycRequestState

/**
 * Holds user's KYC request state
 */
class KycRequestStateRepository(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val blobsRepository: BlobsRepository
) : SingleItemRepository<KycRequestState>() {
    private class NoRequestFoundException : Exception()

    private data class KycRequestAttributes(
            val state: RequestState,
            val rejectReason: String,
            val blobId: String?,
            val roleToSet: Long
    )

    override fun getItem(): Single<KycRequestState> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        var requestId: Long = 0

        data class FinalComposite(
                val state: RequestState,
                val rejectReason: String,
                val kycForm: KycForm,
                val roleToSet: Long
        )

        return getLastKycRequest(signedApi, accountId)
                .switchIfEmpty(Single.error(NoRequestFoundException()))
                .doOnSuccess { request ->
                    requestId = request.id.toLong()
                }
                .map { request ->
                    getKycRequestAttributes(request)
                            ?: throw InvalidKycDataException()
                }
                .flatMap { (state, rejectReason, blobId, roleToSet) ->
                    loadKycFormFromBlob(blobId)
                            .map { kycForm ->
                                FinalComposite(state, rejectReason, kycForm, roleToSet)
                            }
                }
                .map<KycRequestState> { (state, rejectReason, kycForm, roleToSet) ->
                    when (state) {
                        RequestState.REJECTED ->
                            KycRequestState.Submitted.Rejected(kycForm, requestId, roleToSet, rejectReason)
                        RequestState.APPROVED ->
                            KycRequestState.Submitted.Approved(kycForm, requestId, roleToSet)
                        else ->
                            KycRequestState.Submitted.Pending(kycForm, requestId, roleToSet)
                    }
                }
                .onErrorResumeNext { error ->
                    if (error is NoRequestFoundException)
                        Single.just(KycRequestState.Empty)
                    else
                        Single.error(error)
                }
    }

    private fun getLastKycRequest(signedApi: TokenDApi,
                                  accountId: String): Maybe<ReviewableRequestResource> {
        return signedApi
                .v3
                .requests
                .getChangeRoleRequests(
                        ChangeRoleRequestPageParams(
                                requestor = accountId,
                                includes = listOf(RequestParamsV3.Includes.REQUEST_DETAILS),
                                pagingParams = PagingParamsV2(
                                        order = PagingOrder.DESC,
                                        limit = 1
                                )
                        )
                )
                .toSingle()
                .flatMapMaybe { page ->
                    page.items.firstOrNull().toMaybe()
                }
    }

    private fun getKycRequestAttributes(request: ReviewableRequestResource): KycRequestAttributes? {
        return try {
            val state = RequestState.fromI(request.stateI)
            val blobId = request.getTypedRequestDetails<ChangeRoleRequestResource>()
                    .creatorDetails
                    .get("blob_id")
                    ?.asText()
            val rejectReason = request.rejectReason ?: ""
            val roleToSet = request.getTypedRequestDetails<ChangeRoleRequestResource>().accountRoleToSet

            KycRequestAttributes(state, rejectReason, blobId, roleToSet)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadKycFormFromBlob(blobId: String?): Single<KycForm> {
        if (blobId == null) {
            return Single.just(KycForm.Empty)
        }

        return blobsRepository
                .getById(blobId, true)
                .map { blob ->
                    try {
                        KycForm.fromBlob(blob)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        KycForm.Empty
                    }
                }
    }
}