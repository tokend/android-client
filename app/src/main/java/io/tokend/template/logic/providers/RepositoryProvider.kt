package io.tokend.template.logic.providers

import io.tokend.template.data.repository.AccountDetailsRepository
import io.tokend.template.data.repository.AtomicSwapAsksRepository
import io.tokend.template.data.repository.BlobsRepository
import io.tokend.template.features.account.data.storage.AccountRepository
import io.tokend.template.features.assets.storage.AssetChartRepository
import io.tokend.template.features.assets.storage.AssetsRepository
import io.tokend.template.features.balances.storage.BalancesRepository
import io.tokend.template.features.fees.repository.FeesRepository
import io.tokend.template.features.history.storage.BalanceChangesRepository
import io.tokend.template.features.invest.model.SaleRecord
import io.tokend.template.features.invest.repository.InvestmentInfoRepository
import io.tokend.template.features.invest.repository.SalesRepository
import io.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import io.tokend.template.features.kyc.storage.ActiveKycRepository
import io.tokend.template.features.kyc.storage.KycRequestStateRepository
import io.tokend.template.features.limits.repository.LimitsRepository
import io.tokend.template.features.localaccount.storage.LocalAccountRepository
import io.tokend.template.features.offers.repository.OffersRepository
import io.tokend.template.features.polls.repository.PollsRepository
import io.tokend.template.features.send.recipient.contacts.repository.ContactsRepository
import io.tokend.template.features.systeminfo.storage.SystemInfoRepository
import io.tokend.template.features.tfa.repository.TfaFactorsRepository
import io.tokend.template.features.trade.history.repository.TradeHistoryRepository
import io.tokend.template.features.trade.orderbook.repository.OrderBookRepository
import io.tokend.template.features.trade.pairs.repository.AssetPairsRepository

/**
 * Provides SINGLETON instances of repositories
 * If you need parametrized repo use fun ...() getter and LruCache
 */
interface RepositoryProvider {
    val balances: BalancesRepository
    val accountDetails: AccountDetailsRepository
    val systemInfo: SystemInfoRepository
    val tfaFactors: TfaFactorsRepository
    val assets: AssetsRepository
    val assetPairs: AssetPairsRepository
    fun orderBook(baseAsset: String, quoteAsset: String): OrderBookRepository
    fun offers(onlyPrimaryMarket: Boolean = false): OffersRepository
    val account: AccountRepository
    val sales: SalesRepository
    val filteredSales: SalesRepository
    val contacts: ContactsRepository
    val limits: LimitsRepository
    val fees: FeesRepository
    fun balanceChanges(balanceId: String?): BalanceChangesRepository
    fun tradeHistory(base: String, quote: String): TradeHistoryRepository
    fun assetChart(asset: String): AssetChartRepository
    fun assetChart(baseAsset: String, quoteAsset: String): AssetChartRepository
    val kycRequestState: KycRequestStateRepository
    val activeKyc: ActiveKycRepository
    fun investmentInfo(sale: SaleRecord): InvestmentInfoRepository
    fun polls(ownerAccountId: String): PollsRepository
    fun atomicSwapAsks(asset: String): AtomicSwapAsksRepository
    val keyValueEntries: KeyValueEntriesRepository
    val blobs: BlobsRepository
    val localAccount: LocalAccountRepository
}