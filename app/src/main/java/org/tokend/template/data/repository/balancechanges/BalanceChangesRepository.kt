package org.tokend.template.data.repository.balancechanges

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.history.params.ParticipantEffectsPageParams
import org.tokend.sdk.api.v3.history.params.ParticipantEffectsParams
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.BalanceChangeAction
import org.tokend.template.data.model.history.SimpleFeeRecord
import org.tokend.template.data.model.history.converter.ParticipantEffectConverter
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.data.repository.AccountDetailsRepository
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.pagination.PagedDataRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.features.send.model.PaymentRequest
import java.util.*

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
        private val accountDetailsRepository: AccountDetailsRepository?,
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
                .getMovements(
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
                .flatMap(this::loadAndSetEmails)
    }

    private fun loadAndSetEmails(changesPage: DataPage<BalanceChange>): Single<DataPage<BalanceChange>> {
        val payments = changesPage
                .items
                .map(BalanceChange::cause)
                .filterIsInstance(BalanceChangeCause.Payment::class.java)

        val accounts = payments
                .map(BalanceChangeCause.Payment::sourceAccountId)
                .toMutableList()
        accounts.addAll(payments.map(BalanceChangeCause.Payment::destAccountId))

        return if (accounts.isNotEmpty() && accountDetailsRepository != null) {
            accountDetailsRepository
                    .getEmailsByAccountIds(accounts)
                    .onErrorReturnItem(emptyMap())
                    .map { emailsMap ->
                        payments.forEach { payment ->
                            payment.sourceName = emailsMap[payment.sourceAccountId]
                            payment.destName = emailsMap[payment.destAccountId]
                        }
                    }
                    .map { changesPage }
        } else {
            Single.just(changesPage)
        }
    }

    fun addPayment(request: PaymentRequest) {
        val balanceChange = BalanceChange(
                id = System.currentTimeMillis(),
                amount = request.amount,
                date = Date(),
                asset = request.asset,
                balanceId = request.senderBalanceId,
                action = BalanceChangeAction.CHARGED,
                fee = SimpleFeeRecord(
                        percent = request.fee.totalPercentSenderFee,
                        fixed = request.fee.totalFixedSenderFee
                ),
                cause = BalanceChangeCause.Payment(request)
        )

        itemsCache.add(balanceChange)
        broadcast()
    }

    companion object {
        private const val LIMIT = 20
    }
}