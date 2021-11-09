package io.tokend.template.features.invest.view.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.tokend.template.R
import io.tokend.template.features.invest.model.SaleRecord

class SalePagerAdapter(
    sale: SaleRecord,
    context: Context,
    fragmentManager: FragmentManager
) : androidx.fragment.app.FragmentPagerAdapter(
    fragmentManager,
    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {
    private val pages = listOf(
        SaleOverviewFragment.newInstance() to context.getString(R.string.sale_overview_title),

        SaleDetailsFragment.newInstance(
            SaleDetailsFragment.getBundle(
                saleAssetCode = sale.baseAsset.code
            )
        ) to context.getString(R.string.sale_details_title)
    )

    override fun getItem(position: Int): Fragment {
        return pages[position].first
    }

    override fun getPageTitle(position: Int): CharSequence {
        return pages.getOrNull(position)?.second ?: ""
    }

    override fun getCount(): Int = pages.size
}