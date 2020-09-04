package org.tokend.template.features.assets.details.view

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.tokend.template.R
import org.tokend.template.features.assets.model.AssetRecord
import org.tokend.template.features.polls.view.PollsFragment

class AssetDetailsPagerAdapter(asset: AssetRecord,
                               context: Context,
                               fragmentManager: androidx.fragment.app.FragmentManager
) : androidx.fragment.app.FragmentPagerAdapter(fragmentManager) {

    private val pages = mutableListOf(
            AssetDetailsFragment.newInstance(AssetDetailsFragment.getBundle(
                    asset = asset,
                    balanceCreation = true
            )) to context.getString(R.string.asset_overview),
            PollsFragment.newInstance(PollsFragment.getBundle(
                    allowToolbar = false,
                    ownerAccountId = asset.ownerAccountId
            )) to context.getString(R.string.polls_title)
    )

    override fun getItem(position: Int): androidx.fragment.app.Fragment? {
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