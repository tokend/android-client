package io.tokend.template.features.assets.details.view

import android.os.Bundle
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.features.assets.model.AssetRecord
import kotlinx.android.synthetic.main.activity_asset_details.*
import kotlinx.android.synthetic.main.appbar_with_tabs.*
import kotlinx.android.synthetic.main.toolbar.*

class AssetDetailsActivity : BaseActivity() {

    private lateinit var asset: AssetRecord

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_asset_details)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        asset = (intent.getSerializableExtra(ASSET_EXTRA) as? AssetRecord)
            ?: return

        title = asset.name ?: getString(R.string.template_asset_code_asset, asset.code)

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
        private const val ASSET_EXTRA = "asset"

        fun getBundle(asset: AssetRecord) = Bundle().apply {
            putSerializable(ASSET_EXTRA, asset)
        }
    }
}
