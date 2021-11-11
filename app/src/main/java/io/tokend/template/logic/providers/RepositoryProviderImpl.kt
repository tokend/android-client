package io.tokend.template.logic.providers

import android.content.Context
import android.content.SharedPreferences
import androidx.collection.LruCache
import com.fasterxml.jackson.databind.ObjectMapper
import io.tokend.template.BuildConfig
import io.tokend.template.data.repository.AccountDetailsRepository
import io.tokend.template.data.repository.AtomicSwapAsksRepository
import io.tokend.template.data.repository.BlobsRepository
import io.tokend.template.data.storage.persistence.MemoryOnlyObjectPersistence
import io.tokend.template.data.storage.persistence.ObjectPersistence
import io.tokend.template.data.storage.persistence.ObjectPersistenceOnPrefs
import io.tokend.template.data.storage.repository.MemoryOnlyRepositoryCache
import io.tokend.template.data.storage.repository.pagination.advanced.MemoryOnlyCursorCursorPagedDataCache
import io.tokend.template.db.AppDatabase
import io.tokend.template.extensions.getOrPut
import io.tokend.template.features.account.data.model.AccountRecord
import io.tokend.template.features.account.data.storage.AccountRepository
import io.tokend.template.features.assets.storage.AssetChartRepository
import io.tokend.template.features.assets.storage.AssetsDbCache
import io.tokend.template.features.assets.storage.AssetsRepository
import io.tokend.template.features.balances.storage.BalancesDbCache
import io.tokend.template.features.balances.storage.BalancesRepository
import io.tokend.template.features.fees.repository.FeesRepository
import io.tokend.template.features.history.logic.DefaultParticipantEffectConverter
import io.tokend.template.features.history.storage.BalanceChangesPagedDbCache
import io.tokend.template.features.history.storage.BalanceChangesRepository
import io.tokend.template.features.invest.model.SaleRecord
import io.tokend.template.features.invest.repository.InvestmentInfoRepository
import io.tokend.template.features.invest.repository.SalesRepository
import io.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import io.tokend.template.features.kyc.storage.ActiveKycPersistence
import io.tokend.template.features.kyc.storage.ActiveKycRepository
import io.tokend.template.features.kyc.storage.KycRequestStateRepository
import io.tokend.template.features.limits.repository.LimitsRepository
import io.tokend.template.features.localaccount.model.LocalAccount
import io.tokend.template.features.localaccount.storage.LocalAccountRepository
import io.tokend.template.features.offers.repository.OffersRepository
import io.tokend.template.features.polls.repository.PollsCache
import io.tokend.template.features.polls.repository.PollsRepository
import io.tokend.template.features.send.recipient.contacts.repository.ContactsRepository
import io.tokend.template.features.systeminfo.model.SystemInfoRecord
import io.tokend.template.features.systeminfo.storage.SystemInfoRepository
import io.tokend.template.features.tfa.repository.TfaFactorsRepository
import io.tokend.template.features.trade.history.repository.TradeHistoryRepository
import io.tokend.template.features.trade.orderbook.repository.OrderBookRepository
import io.tokend.template.features.trade.pairs.repository.AssetPairsRepository

/**
 * @param context if not specified then android-related repositories
 * will be unavailable
 */
class RepositoryProviderImpl(
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider,
    private val urlConfigProvider: UrlConfigProvider,
    private val mapper: ObjectMapper,
    private val context: Context? = null,
    private val activeKycPersistence: ActiveKycPersistence? = null,
    private val localAccountPersistence: ObjectPersistence<LocalAccount>? = null,
    private val persistencePreferences: SharedPreferences? = null,
    private val database: AppDatabase? = null
) : RepositoryProvider {
    private val conversionAssetCode =
        if (BuildConfig.ENABLE_BALANCES_CONVERSION)
            BuildConfig.BALANCES_CONVERSION_ASSET
        else
            null

    private val assetsCache by lazy {
        database?.let { AssetsDbCache(it.assets) }
            ?: MemoryOnlyRepositoryCache()
    }

    private val balancesCache by lazy {
        database?.let { BalancesDbCache(it.balances, assetsCache) }
            ?: MemoryOnlyRepositoryCache()
    }

    override val balances: BalancesRepository by lazy {
        BalancesRepository(
            apiProvider,
            walletInfoProvider,
            urlConfigProvider,
            mapper,
            conversionAssetCode,
            balancesCache
        )
    }

    override val accountDetails: AccountDetailsRepository by lazy {
        AccountDetailsRepository(apiProvider)
    }

    override val systemInfo: SystemInfoRepository by lazy {
        val persistence =
            if (persistencePreferences != null)
                ObjectPersistenceOnPrefs.forType(
                    persistencePreferences,
                    "system_info"
                )
            else
                MemoryOnlyObjectPersistence<SystemInfoRecord>()
        SystemInfoRepository(apiProvider, persistence)
    }

    override val tfaFactors: TfaFactorsRepository by lazy {
        TfaFactorsRepository(apiProvider, walletInfoProvider, MemoryOnlyRepositoryCache())
    }

    override val assets: AssetsRepository by lazy {
        AssetsRepository(apiProvider, urlConfigProvider, mapper, assetsCache)
    }

    override val assetPairs: AssetPairsRepository by lazy {
        AssetPairsRepository(
            apiProvider, urlConfigProvider, mapper,
            conversionAssetCode, MemoryOnlyRepositoryCache()
        )
    }

    override val account: AccountRepository by lazy {
        val persistence =
            if (persistencePreferences != null)
                ObjectPersistenceOnPrefs.forType(
                    persistencePreferences,
                    "account_record"
                )
            else
                MemoryOnlyObjectPersistence<AccountRecord>()

        AccountRepository(apiProvider, walletInfoProvider, keyValueEntries, persistence)
    }

    override val sales: SalesRepository by lazy {
        SalesRepository(
            walletInfoProvider,
            apiProvider,
            urlConfigProvider,
            mapper
        )
    }

    override val filteredSales: SalesRepository by lazy {
        SalesRepository(
            walletInfoProvider,
            apiProvider,
            urlConfigProvider,
            mapper
        )
    }

    override val contacts: ContactsRepository by lazy {
        context ?: throw IllegalStateException(
            "This provider has no context " +
                    "required to provide contacts repository"
        )
        ContactsRepository(context, MemoryOnlyRepositoryCache())
    }

    override val limits: LimitsRepository by lazy {
        LimitsRepository(apiProvider, walletInfoProvider)
    }

    override val fees: FeesRepository by lazy {
        FeesRepository(apiProvider, walletInfoProvider)
    }

    override val keyValueEntries: KeyValueEntriesRepository by lazy {
        KeyValueEntriesRepository(apiProvider, MemoryOnlyRepositoryCache())
    }

    override val blobs: BlobsRepository by lazy {
        BlobsRepository(apiProvider, walletInfoProvider)
    }

    override val localAccount: LocalAccountRepository by lazy {
        val persistence = localAccountPersistence
            ?: throw IllegalStateException("Local account persistence is required for this repo")
        LocalAccountRepository(persistence)
    }

    private val orderBookRepositories =
        LruCache<String, OrderBookRepository>(MAX_SAME_REPOSITORIES_COUNT)

    override fun orderBook(
        baseAsset: String,
        quoteAsset: String
    ): OrderBookRepository {
        val key = "$baseAsset.$quoteAsset"
        return orderBookRepositories.getOrPut(key) {
            OrderBookRepository(apiProvider, baseAsset, quoteAsset)
        }
    }

    private val offersRepositories =
        LruCache<String, OffersRepository>(MAX_SAME_REPOSITORIES_COUNT)

    override fun offers(onlyPrimaryMarket: Boolean): OffersRepository {
        val key = "$onlyPrimaryMarket"
        return offersRepositories.getOrPut(key) {
            OffersRepository(apiProvider, walletInfoProvider, onlyPrimaryMarket)
        }
    }

    override val kycRequestState: KycRequestStateRepository by lazy {
        KycRequestStateRepository(apiProvider, walletInfoProvider, blobs, keyValueEntries)
    }

    override val activeKyc: ActiveKycRepository by lazy {
        ActiveKycRepository(account, blobs, activeKycPersistence)
    }

    private val balanceChangesRepositoriesByBalanceId =
        LruCache<String, BalanceChangesRepository>(MAX_SAME_REPOSITORIES_COUNT)

    override fun balanceChanges(balanceId: String?): BalanceChangesRepository {
        return balanceChangesRepositoriesByBalanceId.getOrPut(balanceId.toString()) {
            val cache =
                // Cache only account-wide movements.
                if (database != null && balanceId == null)
                    BalanceChangesPagedDbCache(balanceId, database.balanceChanges)
                else
                    MemoryOnlyCursorCursorPagedDataCache()

            BalanceChangesRepository(
                balanceId,
                walletInfoProvider.getWalletInfo().accountId,
                apiProvider,
                DefaultParticipantEffectConverter(),
                accountDetails,
                cache
            )
        }
    }

    private val tradesRepositoriesByAssetPair =
        LruCache<String, TradeHistoryRepository>(MAX_SAME_REPOSITORIES_COUNT)

    override fun tradeHistory(base: String, quote: String): TradeHistoryRepository {
        return tradesRepositoriesByAssetPair.getOrPut("$base:$quote") {
            TradeHistoryRepository(
                base,
                quote,
                apiProvider
            )
        }
    }

    private val chartRepositoriesByCode =
        LruCache<String, AssetChartRepository>(MAX_SAME_REPOSITORIES_COUNT)

    override fun assetChart(asset: String): AssetChartRepository {
        return chartRepositoriesByCode.getOrPut(asset) {
            AssetChartRepository(
                asset,
                apiProvider
            )
        }
    }

    override fun assetChart(baseAsset: String, quoteAsset: String): AssetChartRepository {
        return chartRepositoriesByCode.getOrPut("$baseAsset-$quoteAsset") {
            AssetChartRepository(
                baseAsset,
                quoteAsset,
                apiProvider
            )
        }
    }

    private val investmentInfoRepositoriesBySaleId =
        LruCache<Long, InvestmentInfoRepository>(MAX_SAME_REPOSITORIES_COUNT)

    override fun investmentInfo(sale: SaleRecord): InvestmentInfoRepository {
        return investmentInfoRepositoriesBySaleId.getOrPut(sale.id) {
            InvestmentInfoRepository(sale, offers(), sales)
        }
    }

    private val pollsRepositoriesByOwnerAccountId =
        LruCache<String, PollsRepository>(MAX_SAME_REPOSITORIES_COUNT)

    override fun polls(ownerAccountId: String): PollsRepository {
        return pollsRepositoriesByOwnerAccountId.getOrPut(ownerAccountId) {
            PollsRepository(
                ownerAccountId, apiProvider, walletInfoProvider,
                keyValueEntries, PollsCache()
            )
        }
    }

    private val atomicSwapRepositoryByAsset =
        LruCache<String, AtomicSwapAsksRepository>(MAX_SAME_REPOSITORIES_COUNT)

    override fun atomicSwapAsks(asset: String): AtomicSwapAsksRepository {
        return atomicSwapRepositoryByAsset.getOrPut(asset) {
            AtomicSwapAsksRepository(apiProvider, asset, MemoryOnlyRepositoryCache())
        }
    }

    companion object {
        private const val MAX_SAME_REPOSITORIES_COUNT = 10
    }
}