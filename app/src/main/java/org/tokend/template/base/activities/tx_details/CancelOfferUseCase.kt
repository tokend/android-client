package org.tokend.template.base.activities.tx_details

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.RepositoryProvider
import org.tokend.template.base.logic.transactions.TxManager

/**
 * Cancels given offer.
 * Updates related repositories: order book, balances, offers
 */
class CancelOfferUseCase(
        private val offer: Offer,
        private val repositoryProvider: RepositoryProvider,
        private val accountProvider: AccountProvider,
        private val txManager: TxManager
) {
    private val isPrimaryMarket = offer.orderBookId != 0L

    fun perform(): Completable {
        return cancelOffer()
                .flatMap {
                    updateRepositories()
                }
                .toCompletable()
    }

    private fun cancelOffer(): Single<Boolean> {
        return repositoryProvider
                .offers(isPrimaryMarket)
                .cancel(
                        accountProvider,
                        repositoryProvider.systemInfo(),
                        txManager,
                        offer
                )
                .toSingleDefault(true)
    }

    private fun updateRepositories(): Single<Boolean> {
        if (!isPrimaryMarket) {
            repositoryProvider.orderBook(
                    offer.baseAsset,
                    offer.quoteAsset,
                    offer.isBuy
            ).update()
        }
        repositoryProvider.balances().update()

        return Single.just(true)
    }
}