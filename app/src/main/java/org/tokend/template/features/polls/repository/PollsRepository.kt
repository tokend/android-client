package org.tokend.template.features.polls.repository

import io.reactivex.Single
import io.reactivex.functions.BiFunction
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.polls.params.PollsPageParams
import org.tokend.sdk.api.v3.polls.params.VotesPageParams
import org.tokend.sdk.utils.SimplePagedResourceLoader
import org.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import org.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.MultipleItemsRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.extensions.tryOrNull
import org.tokend.template.features.polls.model.PollRecord

class PollsRepository(
        private val ownerAccountId: String,
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val keyValueEntriesRepository: KeyValueEntriesRepository,
        itemsCache: RepositoryCache<PollRecord>
) : MultipleItemsRepository<PollRecord>(itemsCache) {
    override fun getItems(): Single<List<PollRecord>> {
        return Single.zip(
                getPolls(),
                getVotes(),
                BiFunction { polls: List<PollRecord>, votes: Map<String, Int> ->
                    polls.forEach {
                        it.currentChoice = votes[it.id]
                    }
                    polls.sortedBy { !it.isEnded }
                }
        )
    }

    private fun getPolls(): Single<List<PollRecord>> {
        val loader = SimplePagedResourceLoader({ nextCursor ->
            apiProvider
                    .getApi()
                    .v3
                    .polls
                    .getPolls(
                            PollsPageParams(
                                    owner = ownerAccountId,
                                    pagingParams = PagingParamsV2(
                                            order = PagingOrder.DESC,
                                            page = nextCursor
                                    )
                            )
                    )
                    .map { pollsPage ->
                        val choiceChangeAllowedType = (
                                keyValueEntriesRepository
                                        .getEntry(KEY_POLL_TYPE_CHOICE_CHANGE_ALLOWED)
                                        as? KeyValueEntryRecord.Number
                                )
                                ?.value

                        pollsPage.mapItemsNotNull {
                            tryOrNull {
                                // Ignore cancelled polls.
                                if (it.pollState.value == 4) {
                                    return@tryOrNull null
                                }

                                PollRecord.fromResource(
                                        it,
                                        choiceChangeAllowedType
                                )
                            }
                        }
                    }
        })

        return keyValueEntriesRepository
                .ensureEntries(listOf(KEY_POLL_TYPE_CHOICE_CHANGE_ALLOWED))
                .flatMap {
                    loader
                            .loadAll()
                            .toSingle()
                }
    }

    private fun getVotes(): Single<Map<String, Int>> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        val loader = SimplePagedResourceLoader({ nextCursor ->
            signedApi
                    .v3
                    .polls
                    .getVotesByVoter(
                            accountId,
                            VotesPageParams(
                                    pagingParams = PagingParamsV2(page = nextCursor)
                            )
                    )
        })

        return loader
                .loadAll()
                .toSingle()
                .map { votesList ->
                    votesList
                            .associateBy(
                                    keySelector = {
                                        it.poll.id
                                    },
                                    valueTransform = {
                                        (it.voteData.singleChoice?.toInt() ?: 0) - 1
                                    }
                            )
                            .filterValues { it >= 0 }
                }
    }

    fun updatePollChoiceLocally(pollId: String,
                                choice: Int?) {
        itemsList
                .find { it.id == pollId }
                ?.also { pollToUpdate ->
                    pollToUpdate.currentChoice = choice
                    broadcast()
                }
    }

    private companion object {
        private const val KEY_POLL_TYPE_CHOICE_CHANGE_ALLOWED = "poll_type:unrestricted"
    }
}