package org.tokend.template.features.trade

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import org.tokend.template.R
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.fragments.FragmentFactory

class TradePagerAdapter(assetPair: AssetPairRecord,
                        context: Context,
                        fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager) {
    private val fragmentFactory = FragmentFactory()

    private val pages = listOf(
            Fragment() to
                    context.getString(R.string.trade_order_book_title),
            Fragment() to
                    context.getString(R.string.trade_chart_title),
            Fragment() to
                    context.getString(R.string.trade_trades_title)
    )

    override fun getItem(position: Int): Fragment? {
        return pages.getOrNull(position)?.first
    }

    override fun getPageTitle(position: Int): CharSequence {
        return pages.getOrNull(position)?.second ?: ""
    }

    override fun getCount(): Int = pages.size
}