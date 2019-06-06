package org.tokend.template.features.polls.repository

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.polls.params.PollsPageParams
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.pagination.PagedDataRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.polls.model.PollRecord

class PollsRepository(
        private val ownerAccountId: String,
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        itemsCache: RepositoryCache<PollRecord>
) : PagedDataRepository<PollRecord>(itemsCache) {

    override fun getPage(nextCursor: String?): Single<DataPage<PollRecord>> {
        return apiProvider
                .getApi()
                .v3
                .polls
                .getPolls(
                        PollsPageParams(
                                owner = ownerAccountId,
                                pagingParams = PagingParamsV2(
                                        order = PagingOrder.DESC,
                                        limit = LIMIT,
                                        page = nextCursor
                                )
                        )
                )
                .toSingle()
                .map { pollsPage ->
                    DataPage(
                            isLast = pollsPage.isLast,
                            nextCursor = pollsPage.nextCursor,
                            items = pollsPage.items.map(::PollRecord)
                    )
                }
                .flatMap { pollsPage ->
                    pollsPage
                            .items
                            .map { poll ->
                                getCurrentChoice(poll.id)
                                        .doOnSuccess { currentChoice ->
                                            if (currentChoice > 0) {
                                                poll.currentChoice = currentChoice
                                            }
                                        }
                            }
                            .let { choiceLoadingSingles ->
                                Single.zip(choiceLoadingSingles) { pollsPage }
                            }
                }
    }

    /**
     * @return current choice in poll or 0 if it's missing
     */
    private fun getCurrentChoice(pollId: String): Single<Int> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi
                .v3
                .polls
                .getVoteById(
                        pollId,
                        accountId,
                        null
                )
                .toSingle()
                .map { it.voteData.singleChoice?.toInt() ?: 0 }
                .onErrorReturnItem(0)
    }

    companion object {
        private const val LIMIT = 10
    }
}