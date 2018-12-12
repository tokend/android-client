package org.tokend.template.features.assets

import android.os.Bundle
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.Asset
import org.tokend.template.fragments.FragmentFactory

class AssetDetailsActivity : BaseActivity() {

    private lateinit var asset: Asset

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_asset_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        asset = (intent.getSerializableExtra(ASSET_EXTRA) as? Asset)
                ?: return

        title = getString(R.string.template_asset_details, asset.code)

        supportPostponeEnterTransition()
        startFragment()
    }

    private fun startFragment() {
        val fragment = FragmentFactory().getAssetDetailsFragment(asset)
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
    }

    companion object {
        const val ASSET_EXTRA = "asset"
    }
}
