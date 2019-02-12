package org.tokend.template.di.providers

import android.content.Context
import org.tokend.template.data.model.history.converter.DefaultParticipantEffectConverter
import org.tokend.template.data.repository.*
import org.tokend.template.data.repository.assets.AssetsRepository
import org.tokend.template.data.repository.assets.AssetsRepositoryCache
import org.tokend.template.data.repository.balancechanges.BalanceChangesCache
import org.tokend.template.data.repository.balancechanges.BalanceChangesRepository
import org.tokend.template.data.repository.balances.BalancesCache
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.data.repository.favorites.FavoritesCache
import org.tokend.template.data.repository.favorites.FavoritesRepository
import org.tokend.template.data.repository.offers.OffersCache
import org.tokend.template.data.repository.offers.OffersRepository
import org.tokend.template.data.repository.orderbook.OrderBookCache
import org.tokend.template.data.repository.orderbook.OrderBookRepository
import org.tokend.template.data.repository.pairs.AssetPairsCache
import org.tokend.template.data.repository.pairs.AssetPairsRepository
import org.tokend.template.data.repository.tfa.TfaFactorsCache
import org.tokend.template.data.repository.tfa.TfaFactorsRepository
import org.tokend.template.data.repository.transactions.TxCache
import org.tokend.template.data.repository.transactions.TxRepository
import org.tokend.template.features.invest.repository.SalesCache
import org.tokend.template.features.invest.repository.SalesRepository
import org.tokend.template.features.send.repository.ContactsRepository
import org.tokend.template.features.send.repository.ContactsRepositoryCache

/**
 * @param context if not specified then android-related repositories
 * will be unavailable
 */
class RepositoryProviderImpl(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val urlConfigProvider: UrlConfigProvider,
        private val context: Context? = null
) : RepositoryProvider {
    private val balancesRepository: BalancesRepository by lazy {
        BalancesRepository(apiProvider, walletInfoProvider, urlConfigProvider, BalancesCache())
    }
    private val transactionsRepositoriesByAsset = mutableMapOf<String, TxRepository>()
    private val accountDetails: AccountDetailsRepository by lazy {
        AccountDetailsRepository(apiProvider)
    }
    private val systemInfoRepository: SystemInfoRepository by lazy {
        SystemInfoRepository(apiProvider)
    }
    private val tfaFactorsRepository: TfaFactorsRepository by lazy {
        TfaFactorsRepository(apiProvider, walletInfoProvider, TfaFactorsCache())
    }
    private val assetsRepository: AssetsRepository by lazy {
        AssetsRepository(apiProvider, urlConfigProvider, AssetsRepositoryCache())
    }
    private val orderBookRepositories = mutableMapOf<String, OrderBookRepository>()
    private val assetPairsRepository: AssetPairsRepository by lazy {
        AssetPairsRepository(apiProvider, AssetPairsCache())
    }
    private val offersRepositories = mutableMapOf<String, OffersRepository>()
    private val accountRepository: AccountRepository by lazy {
        AccountRepository(apiProvider, walletInfoProvider)
    }
    private val userRepository: UserRepository by lazy {
        UserRepository(apiProvider, walletInfoProvider)
    }
    private val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepository(apiProvider, walletInfoProvider, FavoritesCache())
    }
    private val salesRepository: SalesRepository by lazy {
        SalesRepository(apiProvider, urlConfigProvider, SalesCache())
    }

    private val filteredSalesRepository: SalesRepository by lazy {
        SalesRepository(apiProvider, urlConfigProvider, SalesCache())
    }

    private val contactsRepository: ContactsRepository by lazy {
        context ?: throw IllegalStateException("This provider has no context " +
                "required to provide contacts repository")
        ContactsRepository(context, ContactsRepositoryCache())
    }

    private val limitsRepository: LimitsRepository by lazy {
        LimitsRepository(apiProvider, walletInfoProvider)
    }

    private val feesRepository: FeesRepository by lazy {
        FeesRepository(apiProvider, walletInfoProvider)
    }

    private val balanceChangesRepositoriesByBalanceId = mutableMapOf<String, BalanceChangesRepository>()

    override fun balances(): BalancesRepository {
        return balancesRepository
    }

    override fun transactions(asset: String): TxRepository {
        return transactionsRepositoriesByAsset.getOrPut(asset) {
            TxRepository(apiProvider, walletInfoProvider, asset, accountDetails(), TxCache())
        }
    }

    override fun accountDetails(): AccountDetailsRepository {
        return accountDetails
    }

    override fun systemInfo(): SystemInfoRepository {
        return systemInfoRepository
    }

    override fun tfaFactors(): TfaFactorsRepository {
        return tfaFactorsRepository
    }

    override fun assets(): AssetsRepository {
        return assetsRepository
    }

    override fun assetPairs(): AssetPairsRepository {
        return assetPairsRepository
    }

    override fun orderBook(baseAsset: String,
                           quoteAsset: String,
                           isBuy: Boolean): OrderBookRepository {
        val key = "$baseAsset.$quoteAsset.$isBuy"
        return orderBookRepositories.getOrPut(key) {
            OrderBookRepository(apiProvider, baseAsset, quoteAsset, isBuy, OrderBookCache(isBuy))
        }
    }

    override fun offers(onlyPrimaryMarket: Boolean): OffersRepository {
        val key = "$onlyPrimaryMarket"
        return offersRepositories.getOrPut(key) {
            OffersRepository(apiProvider, walletInfoProvider, onlyPrimaryMarket, OffersCache())
        }
    }

    override fun account(): AccountRepository {
        return accountRepository
    }

    override fun user(): UserRepository {
        return userRepository
    }

    override fun favorites(): FavoritesRepository {
        return favoritesRepository
    }

    override fun sales(): SalesRepository {
        return salesRepository
    }

    override fun filteredSales(): SalesRepository {
        return filteredSalesRepository
    }

    override fun contacts(): ContactsRepository {
        return contactsRepository
    }

    override fun limits(): LimitsRepository {
        return limitsRepository
    }

    override fun fees(): FeesRepository {
        return feesRepository
    }

    override fun balanceChanges(balanceId: String): BalanceChangesRepository {
        return balanceChangesRepositoriesByBalanceId.getOrPut(balanceId) {
            BalanceChangesRepository(
                    balanceId,
                    apiProvider,
                    DefaultParticipantEffectConverter(balanceId),
                    BalanceChangesCache()
            )
        }
    }
}