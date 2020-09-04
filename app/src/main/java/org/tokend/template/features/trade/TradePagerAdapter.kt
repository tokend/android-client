package org.tokend.template.features.trade

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.tokend.template.R
import org.tokend.template.features.trade.pairs.model.AssetPairRecord
import org.tokend.template.features.trade.chart.view.AssetPairChartFragment
import org.tokend.template.features.trade.history.view.TradeHistoryFragment
import org.tokend.template.features.trade.offers.view.OffersFragment
import org.tokend.template.features.trade.orderbook.view.OrderBookFragment

class TradePagerAdapter(assetPair: AssetPairRecord,
                        context: Context,
                        fragmentManager: androidx.fragment.app.FragmentManager
) : androidx.fragment.app.FragmentPagerAdapter(fragmentManager) {
    private val pages = listOf(
            OrderBookFragment.newInstance(OrderBookFragment.getBundle(assetPair)) to
                    context.getString(R.string.trade_order_book_title),
            AssetPairChartFragment.newInstance(AssetPairChartFragment.getBundle(assetPair)) to
                    context.getString(R.string.trade_chart_title),
            TradeHistoryFragment.newInstance(TradeHistoryFragment.getBundle(assetPair)) to
                    context.getString(R.string.trade_trades_title),
            OffersFragment.newInstance(OffersFragment.getBundle(assetPair)) to
                    context.getString(R.string.trade_offers_title)
    )

    override fun getItem(position: Int): androidx.fragment.app.Fragment? {
        return pages.getOrNull(position)?.first
    }

    override fun getPageTitle(position: Int): CharSequence {
        return pages.getOrNull(position)?.second ?: ""
    }

    override fun getCount(): Int = pages.size
}