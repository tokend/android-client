package org.tokend.template.data.repository.balancechanges

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.history.params.ParticipantEffectsPageParams
import org.tokend.sdk.api.v3.history.params.ParticipantEffectsParams
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.converter.ParticipantEffectConverter
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.pagination.PagedDataRepository
import org.tokend.template.di.providers.ApiProvider

/**
 * Holds balance changes (movements) for specific
 * balance if [balanceId] is specified or for account by [accountId] otherwise.
 *
 * Not specifying both [balanceId] and [accountId] is illegal and
 * prosecuted by law
 */
class BalanceChangesRepository(
        private val balanceId: String?,
        private val accountId: String?,
        private val apiProvider: ApiProvider,
        private val participantEffectConverter: ParticipantEffectConverter,
        itemsCache: RepositoryCache<BalanceChange>
) : PagedDataRepository<BalanceChange>(itemsCache) {

    init {
        if (balanceId == null && accountId == null) {
            throw IllegalArgumentException("Balance or account ID must be specified")
        }
    }

    override fun getPage(nextCursor: String?): Single<DataPage<BalanceChange>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi
                .v3
                .history
                // TODO: Change to 'getMovements' after Horizon 3.3 release
                .get(
                        ParticipantEffectsPageParams.Builder()
                                .withInclude(
                                        ParticipantEffectsParams.Includes.EFFECT,
                                        ParticipantEffectsParams.Includes.OPERATION,
                                        ParticipantEffectsParams.Includes.OPERATION_DETAILS
                                )
                                .withPagingParams(
                                        PagingParamsV2(
                                                order = PagingOrder.DESC,
                                                limit = LIMIT,
                                                page = nextCursor
                                        )
                                )
                                .apply {
                                    if (balanceId != null) {
                                        withBalance(balanceId)
                                    } else {
                                        withAccount(accountId!!)
                                    }
                                }
                                .build()
                )
                .map { effectsPage ->
                    DataPage(
                            isLast = effectsPage.isLast,
                            nextCursor = effectsPage.nextCursor,
                            items = participantEffectConverter
                                    .toBalanceChanges(effectsPage.items)
                                    .toList()
                    )
                }
                .toSingle()
    }

    companion object {
        private const val LIMIT = 20
    }
}