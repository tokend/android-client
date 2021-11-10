package io.tokend.template.features.offers.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.RepositoryProvider
import io.tokend.template.features.offers.model.OfferRecord
import io.tokend.template.logic.TxManager
import org.tokend.sdk.api.transactions.model.SubmitTransactionResponse
import org.tokend.wallet.NetworkParams

/**
 * Cancels given offer.
 * Updates related repositories: order book, balances, offers
 */
class CancelOfferUseCase(
    private val offer: OfferRecord,
    private val repositoryProvider: RepositoryProvider,
    private val accountProvider: AccountProvider,
    private val txManager: TxManager
) {
    private val isPrimaryMarket = offer.orderBookId != 0L
    private lateinit var networkParams: NetworkParams
    private lateinit var resultMeta: String

    fun perform(): Completable {
        return getNetworkParams()
            .doOnSuccess { networkParams ->
                this.networkParams = networkParams
            }
            .flatMap {
                cancelOffer()
            }
            .doOnSuccess { response ->
                this.resultMeta = response.resultMetaXdr!!
            }
            .doOnSuccess {
                updateRepositories()
            }
            .ignoreElement()
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider.systemInfo.getNetworkParams()
    }

    private fun cancelOffer(): Single<SubmitTransactionResponse> {
        return repositoryProvider
            .offers(isPrimaryMarket)
            .cancel(
                accountProvider,
                repositoryProvider.systemInfo,
                txManager,
                offer
            )
    }

    private fun updateRepositories() {
        if (!isPrimaryMarket) {
            repositoryProvider.orderBook(
                offer.baseAsset.code,
                offer.quoteAsset.code
            ).updateIfEverUpdated()
        }
        repositoryProvider.balances.apply {
            if (!updateBalancesByTransactionResultMeta(resultMeta, networkParams)) {
                updateIfEverUpdated()
            }
        }

        if (offer.isBuy) {
            repositoryProvider.balanceChanges(offer.quoteBalanceId).updateIfEverUpdated()
        } else {
            repositoryProvider.balanceChanges(offer.baseBalanceId).updateIfEverUpdated()
        }
    }
}