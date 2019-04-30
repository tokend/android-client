package org.tokend.template.features.offers.logic

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.template.data.repository.SystemInfoRepository
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.features.offers.model.OfferRequest
import org.tokend.template.features.offers.repository.OffersRepository
import org.tokend.template.logic.transactions.TxManager

/**
 * Submits offer by given [OfferRequest].
 * Creates balances if required.
 * Updates related repositories: order book, balances, offers
 */
class ConfirmOfferRequestUseCase(
        private val request: OfferRequest,
        private val accountProvider: AccountProvider,
        private val repositoryProvider: RepositoryProvider,
        private val txManager: TxManager
) {
    private val offerToCancel = request.offerToCancel

    private val cancellationOnly = request.baseAmount.signum() == 0 && offerToCancel != null

    private val isPrimaryMarket = request.orderBookId != 0L

    private val offersRepository: OffersRepository
        get() = repositoryProvider.offers(isPrimaryMarket)

    private val systemInfoRepository: SystemInfoRepository
        get() = repositoryProvider.systemInfo()

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private lateinit var baseBalanceId: String
    private lateinit var quoteBalanceId: String

    fun perform(): Completable {
        return updateBalances()
                .flatMap {
                    getBalances()
                }
                .doOnSuccess { (baseBalanceId, quoteBalanceId) ->
                    this.baseBalanceId = baseBalanceId
                    this.quoteBalanceId = quoteBalanceId

                    offerToCancel?.baseBalanceId = baseBalanceId
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

        val baseAsset = request.baseAssetCode
        val quoteAsset = request.quoteAssetCode

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
                            request.offerToCancel!!
                    )
                    .toSingleDefault(true)
        else
            offersRepository
                    .create(
                            accountProvider,
                            systemInfoRepository,
                            txManager,
                            baseBalanceId,
                            quoteBalanceId,
                            request,
                            offerToCancel
                    )
                    .toSingleDefault(true)
    }

    private fun updateRepositories(): Single<Boolean> {
        if (!isPrimaryMarket) {
            repositoryProvider.orderBook(request.baseAssetCode, request.quoteAssetCode)
                    .updateIfEverUpdated()
        }
        repositoryProvider.balances().updateIfEverUpdated()

        if (request.isBuy) {
            repositoryProvider.balanceChanges(quoteBalanceId).updateIfEverUpdated()
        } else {
            repositoryProvider.balanceChanges(baseBalanceId).updateIfEverUpdated()
        }

        return Single.just(true)
    }
}