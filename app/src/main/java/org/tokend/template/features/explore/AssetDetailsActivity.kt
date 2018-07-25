package org.tokend.template.features.explore

import android.app.Activity
import android.app.FragmentManager
import android.app.ProgressDialog
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.list_item_asset.*
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.extensions.Asset
import org.tokend.template.features.explore.adapter.AssetListItem
import org.tokend.template.features.explore.adapter.AssetListItemViewHolder
import org.tokend.template.util.FileDownloader
import org.tokend.template.util.FragmentFactory
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class AssetDetailsActivity : BaseActivity() {

    private lateinit var asset: Asset

    override fun onCreateAllowed(savedITxHnstanceState: Bundle?) {
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
