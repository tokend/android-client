package io.tokend.template.features.polls.repository

import io.reactivex.Single
import io.tokend.template.data.storage.repository.MultipleItemsRepository
import io.tokend.template.data.storage.repository.RepositoryCache
import io.tokend.template.extensions.tryOrNull
import io.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import io.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import io.tokend.template.features.polls.model.PollRecord
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.polls.model.PollState
import org.tokend.sdk.api.v3.polls.params.PollsPageParams
import org.tokend.sdk.api.v3.polls.params.VotesPageParams
import org.tokend.sdk.utils.SimplePagedResourceLoader

class PollsRepository(
    private val ownerAccountId: String,
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider,
    private val keyValueEntriesRepository: KeyValueEntriesRepository,
    itemsCache: RepositoryCache<PollRecord>,
) : MultipleItemsRepository<PollRecord>(itemsCache) {
    override fun getItems(): Single<List<PollRecord>> {
        return Single.zip(
            getPolls(),
            getVotesByPollId(),
            { polls: List<PollRecord>, voteByPollId: Map<String, Int> ->
                polls.apply {
                    forEach {
                        it.currentChoiceIndex = voteByPollId[it.id]
                    }
                }
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
                            if (it.pollState.value == PollState.CANCELLED.value) {
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

    private fun getVotesByPollId(): Single<Map<String, Int>> {
        val accountId = walletInfoProvider.getWalletInfo().accountId
        val signedApi = apiProvider.getSignedApi()

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

    fun updatePollChoiceLocally(
        pollId: String,
        choiceIndex: Int?,
    ) {
        itemsList
            .find { it.id == pollId }
            ?.also { pollToUpdate ->
                pollToUpdate.currentChoiceIndex = choiceIndex
                broadcast()
            }
    }

    private companion object {
        private const val KEY_POLL_TYPE_CHOICE_CHANGE_ALLOWED = "poll_type:unrestricted"
    }
}