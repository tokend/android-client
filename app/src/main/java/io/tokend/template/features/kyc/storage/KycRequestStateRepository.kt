package io.tokend.template.features.kyc.storage

import com.fasterxml.jackson.databind.JsonNode
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.data.repository.BlobsRepository
import io.tokend.template.data.storage.repository.SingleItemRepository
import io.tokend.template.features.account.data.model.AccountRole
import io.tokend.template.features.account.data.model.ResolvedAccountRole
import io.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import io.tokend.template.features.kyc.model.KycForm
import io.tokend.template.features.kyc.model.KycRequestState
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.TokenDApi
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.model.generated.resources.ChangeRoleRequestResource
import org.tokend.sdk.api.v3.model.generated.resources.ReviewableRequestResource
import org.tokend.sdk.api.v3.requests.model.RequestState
import org.tokend.sdk.api.v3.requests.params.ChangeRoleRequestPageParams
import org.tokend.sdk.api.v3.requests.params.RequestParamsV3
import org.tokend.sdk.utils.extentions.getTypedRequestDetails

/**
 * Holds user's KYC request state
 */
class KycRequestStateRepository(
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider,
    private val blobsRepository: BlobsRepository,
    private val keyValueEntriesRepository: KeyValueEntriesRepository,
) : SingleItemRepository<KycRequestState>() {
    private class NoRequestFoundException : Exception()

    private data class KycRequestAttributes(
        val state: RequestState,
        val rejectReason: String?,
        val blockReason: String?,
        val blobId: String?,
        val roleToSet: ResolvedAccountRole
    )

    override fun getItem(): Single<KycRequestState> {
        val signedApi = apiProvider.getSignedApi()
        val accountId = walletInfoProvider.getWalletInfo().accountId

        var requestId: Long = 0

        data class FinalComposite(
            val state: RequestState,
            val rejectReason: String?,
            val blockReason: String?,
            val kycForm: KycForm,
            val roleToSet: ResolvedAccountRole
        )

        return getLastKycRequest(signedApi, accountId)
            .switchIfEmpty(Single.error(NoRequestFoundException()))
            .doOnSuccess { request ->
                requestId = request.id.toLong()
            }
            .flatMap { request ->
                getKycRequestAttributes(request)
            }
            .flatMap { (state, rejectReason, blockReason, blobId, roleToSet) ->
                loadKycFormFromBlob(blobId, roleToSet.role)
                    .map { kycForm ->
                        FinalComposite(state, rejectReason, blockReason, kycForm, roleToSet)
                    }
            }
            .map<KycRequestState> { (state, rejectReason, blockReason, kycForm, roleToSet) ->
                when (state) {
                    RequestState.REJECTED ->
                        KycRequestState.Submitted.Rejected(
                            kycForm,
                            requestId,
                            roleToSet,
                            rejectReason
                        )
                    RequestState.PERMANENTLY_REJECTED ->
                        KycRequestState.Submitted.PermanentlyRejected(
                            kycForm,
                            requestId,
                            roleToSet,
                            rejectReason
                        )
                    RequestState.APPROVED ->
                        if (roleToSet.role == AccountRole.BLOCKED)
                            KycRequestState.Submitted.ApprovedToBlock(
                                requestId,
                                roleToSet,
                                blockReason,
                            )
                        else
                            KycRequestState.Submitted.Approved(
                                kycForm,
                                requestId,
                                roleToSet
                            )
                    else ->
                        KycRequestState.Submitted.Pending(
                            kycForm,
                            requestId,
                            roleToSet
                        )
                }
            }
            .onErrorResumeNext { error ->
                if (error is NoRequestFoundException)
                    Single.just(KycRequestState.Empty)
                else
                    Single.error(error)
            }
    }

    private fun getLastKycRequest(
        signedApi: TokenDApi,
        accountId: String
    ): Maybe<ReviewableRequestResource> {
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

    private fun getKycRequestAttributes(request: ReviewableRequestResource): Single<KycRequestAttributes> {
        val roleIdToSet =
            request.getTypedRequestDetails<ChangeRoleRequestResource>().accountRoleToSet

        return keyValueEntriesRepository
            .ensureEntries(AccountRole.keyValues())
            .map { keyValueEntriesMap ->
                val creatorDetails = request.getTypedRequestDetails<ChangeRoleRequestResource>()
                    .creatorDetails

                KycRequestAttributes(
                    state = RequestState.fromI(request.stateI),
                    blobId = creatorDetails
                        // Classics.
                        ?.run { get("blob_id") ?: get("blobId") }
                        ?.takeIf(JsonNode::isTextual)
                        ?.asText()
                        ?.takeIf(String::isNotEmpty),
                    rejectReason = request
                        .rejectReason
                        ?.takeIf(String::isNotEmpty),
                    blockReason = creatorDetails
                        .get("blockReason")
                        ?.takeIf(JsonNode::isTextual)
                        ?.asText()
                        ?.takeIf(String::isNotEmpty),
                    roleToSet = ResolvedAccountRole(roleIdToSet, keyValueEntriesMap.values)
                )
            }
    }

    private fun loadKycFormFromBlob(
        blobId: String?,
        accountRole: AccountRole
    ): Single<KycForm> {
        if (blobId == null) {
            return Single.just(KycForm.Empty)
        }

        return blobsRepository
            .getById(blobId, true)
            .map { blob ->
                try {
                    KycForm.fromBlob(blob, accountRole)
                } catch (e: Exception) {
                    e.printStackTrace()
                    KycForm.Empty
                }
            }
    }
}