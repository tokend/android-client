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
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.data.repository.AccountDetailsRepository
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.pagination.PagedDataRepository
import org.tokend.template.di.providers.ApiProvider

class BalanceChangesRepository(
        private val balanceId: String,
        private val apiProvider: ApiProvider,
        private val participantEffectConverter: ParticipantEffectConverter,
        private val accountDetailsRepository: AccountDetailsRepository?,
        itemsCache: RepositoryCache<BalanceChange>
) : PagedDataRepository<BalanceChange>(itemsCache) {

    override fun getPage(nextCursor: String?): Single<DataPage<BalanceChange>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi
                .v3
                .history
                .get(
                        ParticipantEffectsPageParams(
                                balance = balanceId,
                                include = listOf(
                                        ParticipantEffectsParams.Includes.EFFECT,
                                        ParticipantEffectsParams.Includes.OPERATION,
                                        ParticipantEffectsParams.Includes.OPERATION_DETAILS
                                ),
                                pagingParams = PagingParamsV2(
                                        order = PagingOrder.DESC,
                                        limit = LIMIT,
                                        page = nextCursor
                                )
                        )
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
                .flatMap(this::loadAndSetNicknames)
    }

    private fun loadAndSetNicknames(page: DataPage<BalanceChange>): Single<DataPage<BalanceChange>> {
        val accountsToLoad = mutableListOf<String>()

        val payments = page
                .items
                .mapNotNull { it.cause as? BalanceChangeCause.Payment }

        payments.forEach { payment ->
            accountsToLoad.add(payment.sourceAccountId)
            accountsToLoad.add(payment.destAccountId)
        }

        if (accountDetailsRepository == null || accountsToLoad.isEmpty()) {
            return Single.just(page)
        }

        return accountDetailsRepository
                .getDetails(accountsToLoad)
                .doOnSuccess { detailsMap ->
                    payments.forEach { payment ->
                        payment.sourceName = detailsMap[payment.sourceAccountId]?.email
                        payment.destName = detailsMap[payment.destAccountId]?.email
                    }
                }
                .map { page }
                .onErrorReturnItem(page)
    }

    companion object {
        private const val LIMIT = 20
    }
}