package org.tokend.template.features.offers.logic

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.data.repository.SystemInfoRepository
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.offers.repository.OffersRepository
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.logic.transactions.TxManager

/**
 * Sends given offer.
 *
 * Updates related repositories: order book, balances, offers
 */
class ConfirmOfferUseCase(
        private val offer: OfferRecord,
        private val offerToCancel: OfferRecord?,
        private val repositoryProvider: RepositoryProvider,
        private val accountProvider: AccountProvider,
        private val txManager: TxManager
) {
    private val cancellationOnly = offer.baseAmount.signum() == 0 && offerToCancel != null
    private val isPrimaryMarket = offer.orderBookId != 0L

    private val offersRepository: OffersRepository
        get() = repositoryProvider.offers(isPrimaryMarket)

    private val systemInfoRepository: SystemInfoRepository
        get() = repositoryProvider.systemInfo()

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    fun perform(): Completable {
        return updateBalances()
                .flatMap {
                    getBalances()
                }
                .doOnSuccess { (baseBalanceId, quoteBalanceId) ->
                    offer.baseBalanceId = baseBalanceId
                    offerToCancel?.baseBalanceId = baseBalanceId

                    offer.quoteBalanceId = quoteBalanceId
                    offerToCancel?.quoteBalanceId = quoteBalanceId
                }
                .flatMap {
                    submitOfferActions()
                }
                .flatMap {
                    updateRepositories()
                }
                .ignoreElement()
    }

    private fun updateBalances(): Single<Boolean> {
        return balancesRepository
                .updateIfNotFreshDeferred()
                .toSingleDefault(true)
    }

    private fun getBalances(): Single<Pair<String, String>> {
        val balances = balancesRepository.itemsList

        val baseAsset = offer.baseAssetCode
        val quoteAsset = offer.quoteAssetCode

        val existingBase = balances.find { it.assetCode == baseAsset }
        val existingQuote = balances.find { it.assetCode == quoteAsset }

        val toCreate = mutableListOf<String>()
        if (existingBase == null) {
            toCreate.add(baseAsset)
        }
        if (existingQuote == null) {
            toCreate.add(quoteAsset)
        }

        val createMissingBalances =
                if (toCreate.isEmpty())
                    Completable.complete()
                else
                    balancesRepository.create(accountProvider, systemInfoRepository,
                            txManager, *toCreate.toTypedArray())

        return createMissingBalances
                .andThen(
                        Single.defer {
                            val base = balancesRepository.itemsList
                                    .find { it.assetCode == baseAsset }
                                    ?.id
                                    ?: throw IllegalStateException(
                                            "Unable to create balance for $baseAsset"
                                    )
                            val quote = balancesRepository.itemsList
                                    .find { it.assetCode == quoteAsset }
                                    ?.id
                                    ?: throw IllegalStateException(
                                            "Unable to create balance for $quoteAsset"
                                    )

                            Single.just(base to quote)
                        }
                )
    }

    private fun submitOfferActions(): Single<Boolean> {
        return if (cancellationOnly)
            offersRepository
                    .cancel(accountProvider,
                            systemInfoRepository,
                            txManager,
                            offerToCancel!!
                    )
                    .toSingleDefault(true)
        else
            offersRepository
                    .create(
                            accountProvider,
                            systemInfoRepository,
                            txManager,
                            offer,
                            offerToCancel
                    )
                    .toSingleDefault(true)
    }

    private fun updateRepositories(): Single<Boolean> {
        if (!isPrimaryMarket) {
            listOf(repositoryProvider.orderBook(
                    offer.baseAssetCode,
                    offer.quoteAssetCode,
                    true
            ), repositoryProvider.orderBook(
                    offer.baseAssetCode,
                    offer.quoteAssetCode,
                    false
            )).forEach { it.updateIfEverUpdated() }
        }
        repositoryProvider.balances().updateIfEverUpdated()

        if (offer.isBuy) {
            repositoryProvider.balanceChanges(offer.quoteBalanceId).updateIfEverUpdated()
        } else {
            repositoryProvider.balanceChanges(offer.baseBalanceId).updateIfEverUpdated()
        }

        return Single.just(true)
    }
}