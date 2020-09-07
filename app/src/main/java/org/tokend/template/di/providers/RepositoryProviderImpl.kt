package org.tokend.template.di.providers

import android.content.Context
import android.content.SharedPreferences
import androidx.collection.LruCache
import com.fasterxml.jackson.databind.ObjectMapper
import org.tokend.template.BuildConfig
import org.tokend.template.data.model.AccountRecord
import org.tokend.template.data.repository.AccountDetailsRepository
import org.tokend.template.data.repository.AccountRepository
import org.tokend.template.data.repository.AtomicSwapAsksRepository
import org.tokend.template.data.repository.BlobsRepository
import org.tokend.template.data.repository.base.MemoryOnlyObjectPersistence
import org.tokend.template.data.repository.base.MemoryOnlyRepositoryCache
import org.tokend.template.data.repository.base.ObjectPersistence
import org.tokend.template.data.repository.base.ObjectPersistenceOnPrefs
import org.tokend.template.data.repository.base.pagination.MemoryOnlyPagedDataCache
import org.tokend.template.db.AppDatabase
import org.tokend.template.extensions.getOrPut
import org.tokend.template.features.assets.storage.AssetChartRepository
import org.tokend.template.features.assets.storage.AssetsDbCache
import org.tokend.template.features.assets.storage.AssetsRepository
import org.tokend.template.features.balances.storage.BalancesDbCache
import org.tokend.template.features.balances.storage.BalancesRepository
import org.tokend.template.features.fees.repository.FeesRepository
import org.tokend.template.features.history.logic.DefaultParticipantEffectConverter
import org.tokend.template.features.history.storage.BalanceChangesPagedDbCache
import org.tokend.template.features.history.storage.BalanceChangesRepository
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.repository.InvestmentInfoRepository
import org.tokend.template.features.invest.repository.SalesRepository
import org.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import org.tokend.template.features.kyc.storage.ActiveKycPersistence
import org.tokend.template.features.kyc.storage.ActiveKycRepository
import org.tokend.template.features.kyc.storage.KycRequestStateRepository
import org.tokend.template.features.limits.repository.LimitsRepository
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.storage.LocalAccountRepository
import org.tokend.template.features.offers.repository.OffersRepository
import org.tokend.template.features.polls.repository.PollsCache
import org.tokend.template.features.polls.repository.PollsRepository
import org.tokend.template.features.send.recipient.contacts.repository.ContactsRepository
import org.tokend.template.features.systeminfo.model.SystemInfoRecord
import org.tokend.template.features.systeminfo.storage.SystemInfoRepository
import org.tokend.template.features.tfa.repository.TfaFactorsRepository
import org.tokend.template.features.trade.history.repository.TradeHistoryRepository
import org.tokend.template.features.trade.orderbook.repository.OrderBookRepository
import org.tokend.template.features.trade.pairs.repository.AssetPairsRepository

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
    private val balancesRepository: BalancesRepository by lazy {
        BalancesRepository(
                apiProvider,
                walletInfoProvider,
                urlConfigProvider,
                mapper,
                conversionAssetCode,
                balancesCache
        )
    }
    private val accountDetails: AccountDetailsRepository by lazy {
        AccountDetailsRepository(apiProvider)
    }
    private val systemInfoRepository: SystemInfoRepository by lazy {
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
    private val tfaFactorsRepository: TfaFactorsRepository by lazy {
        TfaFactorsRepository(apiProvider, walletInfoProvider, MemoryOnlyRepositoryCache())
    }
    private val assetsRepository: AssetsRepository by lazy {
        AssetsRepository(apiProvider, urlConfigProvider, mapper, assetsCache)
    }
    private val orderBookRepositories =
            LruCache<String, OrderBookRepository>(MAX_SAME_REPOSITORIES_COUNT)
    private val assetPairsRepository: AssetPairsRepository by lazy {
        AssetPairsRepository(apiProvider, urlConfigProvider, mapper,
                conversionAssetCode, MemoryOnlyRepositoryCache())
    }
    private val offersRepositories =
            LruCache<String, OffersRepository>(MAX_SAME_REPOSITORIES_COUNT)
    private val accountRepository: AccountRepository by lazy {
        val persistence =
                if (persistencePreferences != null)
                    ObjectPersistenceOnPrefs.forType(
                            persistencePreferences,
                            "account_record"
                    )
                else
                    MemoryOnlyObjectPersistence<AccountRecord>()

        AccountRepository(apiProvider, walletInfoProvider, persistence)
    }
    private val salesRepository: SalesRepository by lazy {
        SalesRepository(
                walletInfoProvider,
                apiProvider,
                urlConfigProvider,
                mapper
        )
    }

    private val filteredSalesRepository: SalesRepository by lazy {
        SalesRepository(
                walletInfoProvider,
                apiProvider,
                urlConfigProvider,
                mapper
        )
    }

    private val contactsRepository: ContactsRepository by lazy {
        context ?: throw IllegalStateException("This provider has no context " +
                "required to provide contacts repository")
        ContactsRepository(context, MemoryOnlyRepositoryCache())
    }

    private val limitsRepository: LimitsRepository by lazy {
        LimitsRepository(apiProvider, walletInfoProvider)
    }

    private val feesRepository: FeesRepository by lazy {
        FeesRepository(apiProvider, walletInfoProvider)
    }

    private val balanceChangesRepositoriesByBalanceId =
            LruCache<String, BalanceChangesRepository>(MAX_SAME_REPOSITORIES_COUNT)

    private val tradesRepositoriesByAssetPair =
            LruCache<String, TradeHistoryRepository>(MAX_SAME_REPOSITORIES_COUNT)

    private val chartRepositoriesByCode =
            LruCache<String, AssetChartRepository>(MAX_SAME_REPOSITORIES_COUNT)

    private val investmentInfoRepositoriesBySaleId =
            LruCache<Long, InvestmentInfoRepository>(MAX_SAME_REPOSITORIES_COUNT)

    private val pollsRepositoriesByOwnerAccountId =
            LruCache<String, PollsRepository>(MAX_SAME_REPOSITORIES_COUNT)

    private val atomicSwapRepositoryByAsset =
            LruCache<String, AtomicSwapAsksRepository>(MAX_SAME_REPOSITORIES_COUNT)

    private val keyValueEntries: KeyValueEntriesRepository by lazy {
        KeyValueEntriesRepository(apiProvider, MemoryOnlyRepositoryCache())
    }

    private val blobs: BlobsRepository by lazy {
        BlobsRepository(apiProvider)
    }

    private val localAccount: LocalAccountRepository by lazy {
        val persistence = localAccountPersistence
                ?: throw IllegalStateException("Local account persistence is required for this repo")
        LocalAccountRepository(persistence)
    }

    override fun balances(): BalancesRepository {
        return balancesRepository
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
                           quoteAsset: String): OrderBookRepository {
        val key = "$baseAsset.$quoteAsset"
        return orderBookRepositories.getOrPut(key) {
            OrderBookRepository(apiProvider, baseAsset, quoteAsset)
        }
    }

    override fun offers(onlyPrimaryMarket: Boolean): OffersRepository {
        val key = "$onlyPrimaryMarket"
        return offersRepositories.getOrPut(key) {
            OffersRepository(apiProvider, walletInfoProvider, onlyPrimaryMarket)
        }
    }

    private val kycRequestStateRepository: KycRequestStateRepository by lazy {
        KycRequestStateRepository(apiProvider, walletInfoProvider, blobs())
    }

    private val activeKycRepository: ActiveKycRepository by lazy {
        ActiveKycRepository(account(), blobs(), activeKycPersistence)
    }

    override fun account(): AccountRepository {
        return accountRepository
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

    override fun balanceChanges(balanceId: String?): BalanceChangesRepository {
        return balanceChangesRepositoriesByBalanceId.getOrPut(balanceId.toString()) {
            val cache =
                    if (database != null)
                        BalanceChangesPagedDbCache(balanceId, database.balanceChanges)
                    else
                        MemoryOnlyPagedDataCache()

            BalanceChangesRepository(
                    balanceId,
                    walletInfoProvider.getWalletInfo()?.accountId,
                    apiProvider,
                    DefaultParticipantEffectConverter(),
                    accountDetails(),
                    cache
            )
        }
    }

    override fun tradeHistory(base: String, quote: String): TradeHistoryRepository {
        return tradesRepositoriesByAssetPair.getOrPut("$base:$quote") {
            TradeHistoryRepository(
                    base,
                    quote,
                    apiProvider
            )
        }
    }

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

    override fun kycRequestState(): KycRequestStateRepository {
        return kycRequestStateRepository
    }

    override fun activeKyc(): ActiveKycRepository {
        return activeKycRepository
    }

    override fun investmentInfo(sale: SaleRecord): InvestmentInfoRepository {
        return investmentInfoRepositoriesBySaleId.getOrPut(sale.id) {
            InvestmentInfoRepository(sale, offers(), sales())
        }
    }

    override fun polls(ownerAccountId: String): PollsRepository {
        return pollsRepositoriesByOwnerAccountId.getOrPut(ownerAccountId) {
            PollsRepository(ownerAccountId, apiProvider, walletInfoProvider,
                    keyValueEntries(), PollsCache())
        }
    }

    override fun atomicSwapAsks(asset: String): AtomicSwapAsksRepository {
        return atomicSwapRepositoryByAsset.getOrPut(asset) {
            AtomicSwapAsksRepository(apiProvider, asset,
                    MemoryOnlyRepositoryCache())
        }
    }

    override fun keyValueEntries(): KeyValueEntriesRepository {
        return keyValueEntries
    }

    override fun blobs(): BlobsRepository {
        return blobs
    }

    override fun localAccount(): LocalAccountRepository {
        return localAccount
    }

    companion object {
        private const val MAX_SAME_REPOSITORIES_COUNT = 10
    }
}