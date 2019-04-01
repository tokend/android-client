package org.tokend.template.features.trade

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_multiple_fragments.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.AssetPairRecord

class TradeActivity : BaseActivity() {

    private lateinit var assetPair: AssetPairRecord

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_trade)

        val assetPair = intent.getSerializableExtra(ASSET_PAIR_EXTRA)
                as? AssetPairRecord

        if (assetPair == null) {
            finish()
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
    }

    private fun initViewPager() {
        val adapter = TradePagerAdapter(assetPair, this, supportFragmentManager)
        pager.adapter = adapter
        toolbar_tabs.setupWithViewPager(pager)
    }

    companion object {
        const val ASSET_PAIR_EXTRA = "asset_pair"
    }
}
