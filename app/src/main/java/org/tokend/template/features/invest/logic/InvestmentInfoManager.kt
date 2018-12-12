package org.tokend.template.features.invest.logic

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.toMaybe
import org.tokend.sdk.api.TokenDApi
import org.tokend.sdk.api.accounts.params.OffersParams
import org.tokend.sdk.api.assets.model.AssetChartData
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.extensions.Asset
import org.tokend.template.extensions.Sale
import org.tokend.template.extensions.toSingle
import org.tokend.template.logic.FeeManager
import org.tokend.template.view.util.formatter.AmountFormatter
import java.math.BigDecimal

class InvestmentInfoManager(
        private val sale: Sale,
        private val repositoryProvider: RepositoryProvider,
        private val walletInfoProvider: WalletInfoProvider
) {
    class InvestmentInfo(
            val assetDetails: Asset,
            val financialInfo: InvestmentFinancialInfo
    )

    class InvestmentFinancialInfo(
            val offersByAsset: Map<String, Offer>,
            val detailedSale: Sale,
            val maxFeeByAsset: Map<String, BigDecimal>
    )

    fun getAssetDetails(): Single<Asset> {
        return repositoryProvider
                .assets()
                .getSingle(sale.baseAsset)
    }

    fun getOffersByAsset(): Single<Map<String, Offer>> {
        return repositoryProvider
                .offers()
                .getPage(
                        OffersParams(
                                orderBookId = sale.id,
                                isBuy = true,
                                onlyPrimary = true,
                                baseAsset = null,
                                quoteAsset = null
                        )
                )
                .map {
                    it.items
                }
                .map {
                    it.associateBy { offer ->
                        offer.quoteAsset
                    }
                }
    }

    fun getDetailedSale(): Single<Sale> {
        return repositoryProvider
                .sales()
                .getSingle(sale.id)
    }

    fun getDetailedSaleIfNeeded(): Single<Sale> {
        return if (sale.isAvailable)
            getDetailedSale()
        else Single.just(sale)
    }

    fun getMaxFeesMap(feeManager: FeeManager,
                      offersByAsset: Map<String, Offer>): Single<Map<String, BigDecimal>> {
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

    fun getMaxFeesMapIfNeeded(feeManager: FeeManager,
                              offersByAsset: Map<String, Offer>): Single<Map<String, BigDecimal>> {
        return if (sale.isAvailable)
            getMaxFeesMap(feeManager, offersByAsset)
        else
            Single.just(emptyMap())
    }

    fun getAvailableBalance(asset: String, offersByAsset: Map<String, Offer>): BigDecimal {
        val offer = offersByAsset.get(asset)
        val locked = (offer?.quoteAmount ?: BigDecimal.ZERO).add(offer?.fee ?: BigDecimal.ZERO)

        val assetBalance = repositoryProvider
                .balances()
                .itemsList
                .find { it.asset == asset }
                ?.balance ?: BigDecimal.ZERO

        return locked + assetBalance
    }

    fun getMaxInvestmentAmount(asset: String,
                               detailedSale: Sale,
                               offersByAsset: Map<String, Offer>,
                               maxFeeByAsset: Map<String, BigDecimal>): BigDecimal {
        val investAssetDetails = detailedSale.quoteAssets.find { it.code == asset }
        val maxByHardCap = (investAssetDetails?.hardCap ?: BigDecimal.ZERO)
                .subtract(investAssetDetails?.totalCurrentCap ?: BigDecimal.ZERO)
                .add(getExistingInvestmentAmount(asset, offersByAsset))

        val maxByBalance = getAvailableBalance(asset, offersByAsset)
                .minus(maxFeeByAsset[asset] ?: BigDecimal.ZERO)

        return BigDecimalUtil.scaleAmount(
                maxByBalance.min(maxByHardCap),
                AmountFormatter.getDecimalDigitsCount(asset)
        )
    }

    fun getExistingInvestmentAmount(asset: String,
                                    offersByAsset: Map<String, Offer>): BigDecimal {
        return offersByAsset[asset]?.quoteAmount ?: BigDecimal.ZERO
    }

    fun getInvestmentInfo(feeManager: FeeManager): Single<InvestmentInfo> {
        return Single.zip(
                getAssetDetails(),
                getFinancialInfo(feeManager),
                BiFunction { assetDetails: Asset, financialInfo: InvestmentFinancialInfo ->
                    InvestmentInfo(
                            assetDetails,
                            financialInfo
                    )
                }
        )
    }

    fun getFinancialInfo(feeManager: FeeManager): Single<InvestmentFinancialInfo> {
        return Single.zip(
                getDetailedSaleIfNeeded(),
                getOffersByAsset(),
                BiFunction { detailedSale: Sale, offers: Map<String, Offer> ->
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

    fun getChart(api: TokenDApi): Single<AssetChartData> {
        return api
                .assets
                .getChart(sale.baseAsset)
                .toSingle()
    }
}