package org.tokend.template.features.assets

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_asset_details.*
import kotlinx.android.synthetic.main.appbar_with_tabs.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.AssetRecord

class AssetDetailsActivity : BaseActivity() {

    private lateinit var asset: AssetRecord

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_asset_details)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        asset = (intent.getSerializableExtra(ASSET_EXTRA) as? AssetRecord)
                ?: return

        title = getString(R.string.asset_details_title)

        supportPostponeEnterTransition()
        initViewPager()
    }

    private fun initViewPager() {
        val adapter = AssetDetailsPagerAdapter(asset, this, supportFragmentManager)
        pager.adapter = adapter
        appbar_tabs.setupWithViewPager(pager)
        pager.offscreenPageLimit = adapter.count
    }

    override fun onBackPressed() {
        if (pager.currentItem == AssetDetailsPagerAdapter.DETAILS_PAGE_POSITION) {
            super.onBackPressed()
        } else finish()
    }

    companion object {
        const val ASSET_EXTRA = "asset"
    }
}
