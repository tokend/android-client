package org.tokend.template.fragments

import android.support.v4.app.Fragment
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.features.assets.AssetDetailsFragment
import org.tokend.template.features.assets.ExploreAssetsFragment
import org.tokend.template.features.assets.buy.view.AtomicSwapAsksFragment
import org.tokend.template.features.dashboard.balances.view.BalancesFragment
import org.tokend.template.features.dashboard.movements.view.AccountMovementsFragment
import org.tokend.template.features.dashboard.view.DashboardFragment
import org.tokend.template.features.deposit.DepositFragment
import org.tokend.template.features.invest.view.SalesFragment
import org.tokend.template.features.invest.view.fragments.SaleChartFragment
import org.tokend.template.features.invest.view.fragments.SaleDetailsFragment
import org.tokend.template.features.invest.view.fragments.SaleOverviewFragment
import org.tokend.template.features.polls.view.PollsFragment
import org.tokend.template.features.qr.ShareQrFragment
import org.tokend.template.features.send.SendFragment
import org.tokend.template.features.settings.GeneralSettingsFragment
import org.tokend.template.features.trade.chart.view.AssetPairChartFragment
import org.tokend.template.features.trade.history.view.TradeHistoryFragment
import org.tokend.template.features.trade.offers.view.OffersFragment
import org.tokend.template.features.trade.orderbook.view.OrderBookFragment
import org.tokend.template.features.trade.pairs.view.TradeAssetPairsFragment
import org.tokend.template.features.withdraw.WithdrawFragment

class FragmentFactory {

    fun getDashboardFragment(): Fragment {
        return DashboardFragment.newInstance()
    }

    fun getAssetDetailsFragment(asset: AssetRecord, balanceCreation: Boolean = true): Fragment {
        return AssetDetailsFragment.newInstance(asset, balanceCreation)
    }

    fun getAssetDetailsFragment(assetCode: String, balanceCreation: Boolean = true): Fragment {
        return AssetDetailsFragment.newInstance(assetCode, balanceCreation)
    }

    fun getSettingsFragment(): Fragment {
        return GeneralSettingsFragment()
    }

    fun getOrderBookFragment(assetPair: AssetPairRecord): Fragment {
        return OrderBookFragment.newInstance(assetPair)
    }

    fun getWithdrawFragment(asset: String? = null): Fragment {
        return WithdrawFragment.newInstance(asset)
    }

    fun getSendFragment(asset: String? = null, allowToolbar: Boolean = true): Fragment {
        return SendFragment.newInstance(asset, allowToolbar)
    }

    fun getExploreFragment(): Fragment {
        return ExploreAssetsFragment()
    }

    fun getDepositFragment(asset: String? = null): Fragment {
        return DepositFragment.newInstance(asset)
    }

    fun getSalesFragment(): Fragment {
        return SalesFragment()
    }

    fun getSaleOverviewFragment(): Fragment {
        return SaleOverviewFragment()
    }

    fun getSaleDetailsFragment(saleAssetCode: String): Fragment {
        return SaleDetailsFragment.newInstance(saleAssetCode)
    }

    fun getSaleChartFragment(): Fragment {
        return SaleChartFragment()
    }

    fun getTradeAssetPairsFragment(): Fragment {
        return TradeAssetPairsFragment()
    }

    fun getAssetPairChartFragment(assetPair: AssetPairRecord): Fragment {
        return AssetPairChartFragment.newInstance(assetPair)
    }

    fun getTradeHistoryFragment(assetPair: AssetPairRecord): Fragment {
        return TradeHistoryFragment.newInstance(assetPair)
    }

    fun getOffersFragment(assetPair: AssetPairRecord): Fragment {
        return OffersFragment.newInstance(assetPair)
    }

    fun getOffersFragment(onlyPrimary: Boolean): Fragment {
        return OffersFragment.newInstance(onlyPrimary)
    }

    fun getAccountMovementsFragment(): Fragment {
        return AccountMovementsFragment()
    }

    fun getBalancesFragment(): Fragment {
        return BalancesFragment()
    }

    fun getShareQrFragment(title: String? = null,
                           data: String? = null,
                           shareDialogText: String? = null,
                           shareText: String? = null,
                           topText: String? = null): Fragment {
        return ShareQrFragment.newInstance(title, data, shareDialogText, shareText, topText)
    }

    fun getPollsFragment(ownerAccountId: String? = null): Fragment {
        return PollsFragment.newInstance(
                allowToolbar = ownerAccountId == null,
                ownerAccountId = ownerAccountId
        )
    }

    fun getAtomicSwapAsksFragment(assetCode: String): Fragment {
        return AtomicSwapAsksFragment.newInstance(assetCode)
    }
}