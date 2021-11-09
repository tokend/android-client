package io.tokend.template.features.trade

import android.os.Bundle
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.extensions.dip
import io.tokend.template.features.trade.pairs.model.AssetPairRecord
import kotlinx.android.synthetic.main.activity_trade.*
import kotlinx.android.synthetic.main.appbar_with_tabs.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.toolbar.*

class TradeActivity : BaseActivity() {

    private lateinit var assetPair: AssetPairRecord

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_trade)

        val assetPair = intent.getSerializableExtra(ASSET_PAIR_EXTRA)
                as? AssetPairRecord

        if (assetPair == null) {
            finishWithMissingArgError(ASSET_PAIR_EXTRA)
            return
        }

        this.assetPair = assetPair

        initToolbar()
        initViewPager()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.template_asset_pair, assetPair.base, assetPair.quote)
        toolbar.setSubtitleTextAppearance(this, R.style.ToolbarSubtitleAppearance)
        toolbar.subtitle = getString(
            R.string.template_price_one_equals, assetPair.base,
            amountFormatter.formatAssetAmount(assetPair.price, assetPair.quote)
        )
        appbar_elevation_view.layoutParams = appbar_elevation_view.layoutParams.apply {
            height = dip(3)
        }
    }

    private fun initViewPager() {
        val adapter = TradePagerAdapter(assetPair, this, supportFragmentManager)
        pager.adapter = adapter
        appbar_tabs.setupWithViewPager(pager)
        pager.offscreenPageLimit = adapter.count
    }

    companion object {
        private const val ASSET_PAIR_EXTRA = "asset_pair"

        fun getBundle(assetPair: AssetPairRecord) = Bundle().apply {
            putSerializable(ASSET_PAIR_EXTRA, assetPair)
        }
    }
}
