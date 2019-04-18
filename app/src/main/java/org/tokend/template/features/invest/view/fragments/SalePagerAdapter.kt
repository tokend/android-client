package org.tokend.template.features.invest.view.fragments

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import org.tokend.template.R
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.fragments.FragmentFactory

class SalePagerAdapter(sale: SaleRecord,
                       context: Context,
                       fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager) {
    private val fragmentFactory = FragmentFactory()

    private val pages = listOf(
            fragmentFactory.getSaleOverviewFragment() to
                    context.getString(R.string.sale_overview_title),
            fragmentFactory.getSaleInvestFragment() to
                    context.getString(R.string.sale_investing),
            fragmentFactory.getSaleDetailsFragment(sale.baseAssetCode) to
                    context.getString(R.string.sale_details_title),
            fragmentFactory.getSaleChartFragment() to
                    context.getString(R.string.sale_chart_title)
    )

    override fun getItem(position: Int): Fragment? {
        return pages.getOrNull(position)?.first
    }

    override fun getPageTitle(position: Int): CharSequence {
        return pages.getOrNull(position)?.second ?: ""
    }

    override fun getCount(): Int = pages.size
}