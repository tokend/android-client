package org.tokend.template.features.assets

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import org.tokend.template.R
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.fragments.FragmentFactory

class AssetDetailsPagerAdapter(asset: AssetRecord,
                               context: Context,
                               fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager) {

    private val fragmentFactory = FragmentFactory()

    private val pages = listOf(
            fragmentFactory.getAssetDetailsFragment(asset) to
                    context.getString(R.string.asset_overview),
            fragmentFactory.getPollsFragment(asset.ownerAccountId) to
                    context.getString(R.string.polls_title)
    )

    override fun getItem(position: Int): Fragment? {
        return pages.getOrNull(position)?.first
    }

    override fun getPageTitle(position: Int): CharSequence {
        return pages.getOrNull(position)?.second ?: ""
    }

    override fun getCount(): Int = pages.size

    companion object {
        /**
         * Page that contains view for transition.
         */
        const val DETAILS_PAGE_POSITION = 0
    }
}