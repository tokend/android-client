package io.tokend.template.features.offers.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.tokend.template.di.providers.AccountProvider
import io.tokend.template.di.providers.RepositoryProvider
import io.tokend.template.features.balances.storage.BalancesRepository
import io.tokend.template.features.offers.model.OfferRequest
import io.tokend.template.features.offers.repository.OffersRepository
import io.tokend.template.features.systeminfo.storage.SystemInfoRepository
import io.tokend.template.logic.TxManager
import org.tokend.sdk.api.transactions.model.SubmitTransactionResponse
import org.tokend.wallet.NetworkParams

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
        get() = repositoryProvider.systemInfo

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances

    private lateinit var networkParams: NetworkParams
    private lateinit var baseBalanceId: String
    private lateinit var quoteBalanceId: String
    private lateinit var resultMeta: String

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
                getNetworkParams()
            }
            .doOnSuccess { networkParams ->
                this.networkParams = networkParams
            }
            .flatMap {
                submitOfferActions()
            }
            .doOnSuccess { response ->
                this.resultMeta = response.resultMetaXdr!!
            }
            .doOnSuccess {
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

        val baseAsset = request.baseAsset
        val quoteAsset = request.quoteAsset

        val existingBase = balances.find { it.assetCode == baseAsset.code }
        val existingQuote = balances.find { it.assetCode == quoteAsset.code }

        val toCreate = mutableListOf<String>()
        if (existingBase == null) {
            toCreate.add(baseAsset.code)
        }
        if (existingQuote == null) {
            toCreate.add(quoteAsset.code)
        }

        val createMissingBalances =
            if (toCreate.isEmpty())
                Completable.complete()
            else
                balancesRepository.create(
                    accountProvider, systemInfoRepository,
                    txManager, *toCreate.toTypedArray()
                )

        return createMissingBalances
            .andThen(
                Single.defer {
                    val base = balancesRepository.itemsList
                        .find { it.assetCode == baseAsset.code }
                        ?.id
                        ?: throw IllegalStateException(
                            "Unable to create balance for $baseAsset"
                        )
                    val quote = balancesRepository.itemsList
                        .find { it.assetCode == quoteAsset.code }
                        ?.id
                        ?: throw IllegalStateException(
                            "Unable to create balance for $quoteAsset"
                        )

                    Single.just(base to quote)
                }
            )
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider.systemInfo.getNetworkParams()
    }

    private fun submitOfferActions(): Single<SubmitTransactionResponse> {
        return if (cancellationOnly)
            offersRepository
                .cancel(
                    accountProvider,
                    systemInfoRepository,
                    txManager,
                    request.offerToCancel!!
                )
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
    }

    private fun updateRepositories() {
        if (!isPrimaryMarket) {
            repositoryProvider.orderBook(request.baseAsset.code, request.quoteAsset.code)
                .updateIfEverUpdated()
        }
        repositoryProvider.balances.apply {
            if (!updateBalancesByTransactionResultMeta(resultMeta, networkParams))
                updateIfEverUpdated()
        }

        if (request.isBuy) {
            repositoryProvider.balanceChanges(quoteBalanceId).updateIfEverUpdated()
        } else {
            repositoryProvider.balanceChanges(baseBalanceId).updateIfEverUpdated()
        }
    }
}