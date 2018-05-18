package org.tokend.template.features.explore

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.list_item_asset.*
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.models.Asset
import org.tokend.template.R
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.features.explore.adapter.AssetListItem
import org.tokend.template.features.explore.adapter.AssetListItemViewHolder
import org.tokend.template.util.FileDownloader
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class AssetDetailsActivity : BaseActivity() {

    private lateinit var asset: Asset

    private val balanceExists: Boolean
        get() = repositoryProvider.balances().itemsSubject.value
                .find { it.asset == asset.code } != null

    private val fileDownloader = FileDownloader(this)

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_asset_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        asset = (intent.getSerializableExtra(ASSET_EXTRA) as? Asset)
                ?: return

        title = getString(R.string.template_asset_details, asset.code)

        supportPostponeEnterTransition()

        displayDetails()
        initButtons()

        supportStartPostponedEnterTransition()
    }

    // region Display
    private fun displayDetails() {
        displayLogoAndName()
        displaySummary()
        displayTermsIfNeeded()
    }

    private fun displayLogoAndName() {
        AssetListItemViewHolder(asset_card).bind(AssetListItem(asset, balanceExists))
    }

    private fun displaySummary() {
        InfoCard(cards_layout)
                .setHeading(R.string.asset_summary_title, null)
                .addRow(R.string.asset_available,
                        AmountFormatter.formatAssetAmount(asset.available))
                .addRow(R.string.asset_issued, AmountFormatter.formatAssetAmount(asset.issued))
                .addRow(R.string.asset_maximum, AmountFormatter.formatAssetAmount(asset.maximum))
    }

    private fun displayTermsIfNeeded() {
        val terms = asset.details.terms.takeIf { !it.name.isNullOrEmpty() }
                ?: return

        val fileCardView =
                layoutInflater.inflate(R.layout.list_item_remote_file,
                        null, false)

        val fileLayout = fileCardView?.find<View>(R.id.file_content_layout) ?: return
        (fileCardView as? CardView)?.removeView(fileLayout)

        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        fileLayout.layoutParams = layoutParams

        fileLayout.onClick {
            fileDownloader.download(this, terms)
        }

        val fileNameTextView = fileLayout.find<TextView>(R.id.file_name_text_view)
        fileNameTextView.text = terms.name

        val fileIconImageView = fileLayout.find<ImageView>(R.id.file_icon_image_view)
        fileIconImageView.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.ic_file)
        )

        InfoCard(cards_layout)
                .setHeading(R.string.asset_terms_of_use, null)
                .addView(fileLayout)
    }
    // endregion

    private fun initButtons() {
        asset_details_button.visibility = View.GONE

        if (!balanceExists) {
            asset_primary_action_button.visibility = View.VISIBLE
            asset_primary_action_button.text = getString(R.string.create_balance_action)
            asset_primary_action_button.onClick {
                createBalance()
            }
        } else {
            asset_primary_action_button.visibility = View.GONE
            asset_card_divider.visibility = View.GONE
        }
    }

    private fun createBalance() {
        val progress = ProgressDialog(this)
        progress.isIndeterminate = true
        progress.setMessage(getString(R.string.processing_progress))
        progress.setCancelable(false)

        repositoryProvider.balances()
                .create(asset.code, accountProvider, repositoryProvider.systemInfo(),
                        TxManager(apiProvider))
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                .doOnSubscribe {
                    progress.show()
                }
                .doOnTerminate {
                    progress.dismiss()
                }
                .subscribeBy(
                        onComplete = {
                            onBalanceCreated()
                        },
                        onError = { ErrorHandlerFactory.getDefault().handle(it) }
                )
    }

    private fun onBalanceCreated() {
        ToastManager.short(getString(R.string.template_asset_balance_created,
                asset.code))

        setResult(Activity.RESULT_OK)
        displayLogoAndName()
        initButtons()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        fileDownloader.handlePermissionResult(requestCode, permissions, grantResults)
    }

    companion object {
        const val ASSET_EXTRA = "asset"
    }
}
