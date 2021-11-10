package io.tokend.template.features.history.storage

import io.reactivex.Single
import io.tokend.template.data.repository.AccountDetailsRepository
import io.tokend.template.data.storage.repository.pagination.advanced.AdvancedCursorPagedDataRepository
import io.tokend.template.data.storage.repository.pagination.advanced.CursorPagedDataCache
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.features.history.logic.ParticipantEffectConverter
import io.tokend.template.features.history.model.BalanceChange
import io.tokend.template.features.history.model.BalanceChangeAction
import io.tokend.template.features.history.model.SimpleFeeRecord
import io.tokend.template.features.history.model.details.BalanceChangeCause
import io.tokend.template.features.send.model.PaymentRequest
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV3
import org.tokend.sdk.api.v3.history.params.ParticipantEffectsPageParams
import org.tokend.sdk.api.v3.history.params.ParticipantEffectsParams
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
    cache: CursorPagedDataCache<BalanceChange>,
) : AdvancedCursorPagedDataRepository<BalanceChange>(
    pagingOrder = PagingOrder.DESC,
    cache = cache
) {
    init {
        if (balanceId == null && accountId == null) {
            throw IllegalArgumentException("Balance or account ID must be specified")
        }
    }

    override fun getRemotePage(
        limit: Int,
        cursor: Long?,
        requiredOrder: PagingOrder,
    ): Single<DataPage<BalanceChange>> {
        val signedApi = apiProvider.getSignedApi()

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
                        PagingParamsV3.withCursor(
                            order = requiredOrder,
                            limit = limit,
                            cursor = cursor?.toString()
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

        mItems.add(0, balanceChange)
        broadcast()
    }
}