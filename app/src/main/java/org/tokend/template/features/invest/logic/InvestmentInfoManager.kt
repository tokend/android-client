package org.tokend.template.features.invest.logic

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.toMaybe
import org.tokend.sdk.api.TokenDApi
import org.tokend.sdk.api.assets.model.AssetChartData
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.extensions.toSingle
import org.tokend.template.features.assets.model.AssetRecord
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.logic.FeeManager
import org.tokend.template.view.util.formatter.AmountFormatter
import java.math.BigDecimal

/**
 * Loads and manages investment-related information
 */
class InvestmentInfoManager(
        private val sale: SaleRecord,
        private val repositoryProvider: RepositoryProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val amountFormatter: AmountFormatter
) {
    /**
     * Contains data required for sale details display
     * and investment calculations
     */
    class InvestmentInfo(
            val assetDetails: AssetRecord,
            val financialInfo: InvestmentFinancialInfo
    )

    /**
     * Contains data required for investment calculations
     */
    class InvestmentFinancialInfo(
            /**
             * Pending offers by quote assets
             */
            val offersByAsset: Map<String, OfferRecord>,
            /**
             * Detailed sale info contains calculated caps for quote assets
             */
            val detailedSale: SaleRecord,
            /**
             * Max possible fee for each quote asset
             */
            val maxFeeByAsset: Map<String, BigDecimal>
    )

    private fun getAssetDetails(): Single<AssetRecord> {
        return repositoryProvider
                .assets()
                .getSingle(sale.baseAssetCode)
    }

    private fun getOffersByAsset(): Single<Map<String, OfferRecord>> {
        return repositoryProvider
                .offers()
                .getForSale(sale.id)
                .map {
                    it.associateBy { offer ->
                        offer.quoteAssetCode
                    }
                }
    }

    private fun getDetailedSale(): Single<SaleRecord> {
        return repositoryProvider
                .sales()
                .getSingle(sale.id)
    }

    private fun getDetailedSaleIfNeeded(): Single<SaleRecord> {
        return if (sale.isAvailable)
            getDetailedSale()
        else Single.just(sale)
    }

    private fun getMaxFeesMap(feeManager: FeeManager,
                              offersByAsset: Map<String, OfferRecord>): Single<Map<String, BigDecimal>> {
        val feeMap = mutableMapOf<String, BigDecimal>()

        return walletInfoProvider
                .getWalletInfo()
                ?.accountId
                .toMaybe()
                .switchIfEmpty(Single.error(IllegalStateException("Missing account ID")))
                .flatMap { accountId ->
                    Observable.merge(
                            sale.quoteAssets.mapNotNull { quoteAsset ->
                                val availableBalance = getAvailableBalance(
                                        quoteAsset.code,
                                        offersByAsset
                                )

                                return@mapNotNull if (availableBalance.signum() == 0)
                                    null
                                else
                                    feeManager.getOfferFee(
                                            accountId,
                                            quoteAsset.code,
                                            availableBalance
                                    )
                                            .map {
                                                val percent = it.percent
                                                feeMap[it.asset] = percent
                                                percent
                                            }
                                            .onErrorResumeNext(Single.just(BigDecimal.ZERO))
                                            .toObservable()
                            }
                    ).last(BigDecimal.ZERO)
                }
                .map {
                    feeMap
                }
    }

    private fun getMaxFeesMapIfNeeded(feeManager: FeeManager,
                                      offersByAsset: Map<String, OfferRecord>): Single<Map<String, BigDecimal>> {
        return if (sale.isAvailable)
            getMaxFeesMap(feeManager, offersByAsset)
        else
            Single.just(emptyMap())
    }

    /**
     * @return balance available for investment in specified [asset]
     * considering pending offers
     */
    fun getAvailableBalance(asset: String, offersByAsset: Map<String, OfferRecord>): BigDecimal {
        val offer = offersByAsset.get(asset)
        val locked = (offer?.quoteAmount ?: BigDecimal.ZERO).add(offer?.fee ?: BigDecimal.ZERO)

        val assetBalance = repositoryProvider
                .balances()
                .itemsList
                .find { it.assetCode == asset }
                ?.available ?: BigDecimal.ZERO

        return locked + assetBalance
    }

    /**
     * @return maximal investment amount in specified [asset]
     * considering pending offers, available balance, fees and sale limitations
     */
    fun getMaxInvestmentAmount(asset: String,
                               detailedSale: SaleRecord,
                               offersByAsset: Map<String, OfferRecord>,
                               maxFeeByAsset: Map<String, BigDecimal>): BigDecimal {
        val investAssetDetails = detailedSale.quoteAssets.find { it.code == asset }
        val maxByHardCap = (investAssetDetails?.hardCap ?: BigDecimal.ZERO)
                .subtract(investAssetDetails?.totalCurrentCap ?: BigDecimal.ZERO)
                .add(getExistingInvestmentAmount(asset, offersByAsset))

        val maxByBalance = getAvailableBalance(asset, offersByAsset)
                .minus(maxFeeByAsset[asset] ?: BigDecimal.ZERO)

        return BigDecimalUtil.scaleAmount(
                maxByBalance.min(maxByHardCap),
                amountFormatter.getDecimalDigitsCount(asset)
        )
    }

    /**
     * @return amount of investment in specified [asset]
     * i.e. amount locked by the pending offer
     */
    fun getExistingInvestmentAmount(asset: String,
                                    offersByAsset: Map<String, OfferRecord>): BigDecimal {
        return offersByAsset[asset]?.quoteAmount ?: BigDecimal.ZERO
    }

    /**
     * @return data required for sale details display
     * and investment calculations
     */
    fun getInvestmentInfo(feeManager: FeeManager): Single<InvestmentInfo> {
        return Single.zip(
                getAssetDetails(),
                getFinancialInfo(feeManager),
                BiFunction { assetDetails: AssetRecord, financialInfo: InvestmentFinancialInfo ->
                    InvestmentInfo(
                            assetDetails,
                            financialInfo
                    )
                }
        )
    }

    /**
     * @return data required for investment calculations
     */
    fun getFinancialInfo(feeManager: FeeManager): Single<InvestmentFinancialInfo> {
        return Single.zip(
                getDetailedSaleIfNeeded(),
                getOffersByAsset(),
                BiFunction { detailedSale: SaleRecord, offers: Map<String, OfferRecord> ->
                    detailedSale to offers
                }
        )
                .flatMap { (detailedSale, offers) ->
                    getMaxFeesMapIfNeeded(
                            feeManager,
                            offers
                    )
                            .map { feesMap ->
                                InvestmentFinancialInfo(
                                        offers,
                                        detailedSale,
                                        feesMap
                                )
                            }
                }
    }

    /**
     * @return sale chart (amount of investments by time)
     */
    fun getChart(api: TokenDApi): Single<AssetChartData> {
        return api
                .assets
                .getChart(sale.baseAssetCode)
                .toSingle()
    }
}